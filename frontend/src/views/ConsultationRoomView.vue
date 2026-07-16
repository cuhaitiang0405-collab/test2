<script setup lang="ts">
/**
 * ③ 协同通讯层 — 会诊协同房间（M5 主界面）。
 * Mesh WebRTC 音视频 + 电子白板标注同步 + 桌面共享。
 * 信令/标注经 CollabSignal(WS) 中继；媒体浏览器间直连（生产可换 SFU，前端零改）。
 */
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth } from '../store/auth'
import { collabApi, type AnnotationOp } from '../api'
import { CollabSignal } from '../lib/collab/signaling'
import { MeshPeerManager } from '../lib/collab/webrtc'
import { Whiteboard } from '../lib/collab/whiteboard'

const route = useRoute()
const router = useRouter()
const consultationId = (route.params.consultationId as string) || 'C0001'

const loading = ref(true)
const error = ref('')
const traceId = ref('')
const patientVisitUid = ref('')
const title = ref(consultationId)
const myId = ref(auth.state.username || 'me')

// 媒体
const myStream = ref<MediaStream | null>(null)
const localVideo = ref<HTMLVideoElement | null>(null)
const peers = reactive<{ id: string }[]>([])
const streams = new Map<string, MediaStream>() // peerId -> 远端流（非响应式，直接绑 DOM）

// 白板
const boardCanvas = ref<HTMLCanvasElement | null>(null)
let board: Whiteboard | null = null
const tool = ref<AnnotationOp['type']>('pen')
const color = ref('#ff3b30')
const drawing = ref(false)
const shareState = ref<'off' | 'on' | 'unsupported'>('off')
let screenTrack: MediaStreamTrack | null = null

// 信令 + Mesh
let signal: CollabSignal | null = null
const peerMgr = new MeshPeerManager()

async function loadConsultationMeta() {
  // 拉会诊元信息（patientVisitUid 用于审计脱敏）；失败用空，不影响入房
  try {
    const r = await fetch(`/api/workflow/consultations/${consultationId}`, { headers: { 'Content-Type': 'application/json' } })
    if (r.ok) {
      const c = await r.json()
      patientVisitUid.value = c.patientVisitUid || ''
      title.value = c.title || consultationId
    }
  } catch { /* 忽略 */ }
}

async function getLocalStream(): Promise<MediaStream> {
  // 优先真实摄像头/麦克风；无设备（headless）回退合成画布流，保证媒体链路可测
  try {
    const s = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    if (s.getVideoTracks().length) return s
    s.getTracks().forEach(t => t.stop())
  } catch { /* 回退 */ }
  return makeSyntheticStream()
}

function makeSyntheticStream(): MediaStream {
  const c = document.createElement('canvas')
  c.width = 320; c.height = 240
  const ctx = c.getContext('2d')!
  let n = 0
  const tick = () => {
    n++
    ctx.fillStyle = `hsl(${n % 360} 70% 55%)`
    ctx.fillRect(0, 0, c.width, c.height)
    ctx.fillStyle = '#fff'
    ctx.font = '20px sans-serif'
    ctx.fillText(`MDT ${myId.value} #${n}`, 20, 40)
    requestAnimationFrame(tick)
  }
  tick()
  // @ts-ignore captureStream 在较新浏览器可用
  return (c.captureStream?.(15) || new MediaStream()) as MediaStream
}

async function setupMedia() {
  const s = await getLocalStream()
  myStream.value = s
  peerMgr.setLocalStream(s)
  await nextTick()
  if (localVideo.value) localVideo.value.srcObject = s
}

function bindRemote(peerId: string, stream: MediaStream) {
  streams.set(peerId, stream)
  if (!peers.find(p => p.id === peerId)) peers.push({ id: peerId })
  nextTick(() => {
    const el = document.querySelector<HTMLVideoElement>(`video[data-peer="${peerId}"]`)
    if (el) el.srcObject = stream
  })
}

async function join() {
  loading.value = true
  try {
    await loadConsultationMeta()
    // 建房（审计 + 取 wsPath）
    await collabApi.joinRoom({
      consultationId, user: myId.value, tenant: auth.state.tenantId || 'T001',
      patientVisitUid: patientVisitUid.value
    })
    // 连接信令
    signal = new CollabSignal({
      token: auth.state.token, user: myId.value, room: consultationId,
      tenant: auth.state.tenantId || 'T001', patientVisitUid: patientVisitUid.value
    })
    signal.on('__open', () => { traceId.value = '' })
    signal.on('welcome', (m: any) => {
      myId.value = m.you
      // 我是新加入者：主动向已有对端发起 offer（避免双方同时 offer 的 glare）
      ;(m.peers as string[]).filter((p: string) => p !== m.you).forEach((p: string) => peerMgr.addPeer(p))
      syncPeers(m.peers)
    })
    signal.on('peer-join', (m: any) => syncPeers(m.peers)) // UI 仅更新列表
    signal.on('peer-leave', (m: any) => {
      syncPeers(m.peers)
      streams.delete(m.user)
    })
    signal.on('offer', (m: any) => peerMgr.handleSignal(m))
    signal.on('answer', (m: any) => peerMgr.handleSignal(m))
    signal.on('ice', (m: any) => peerMgr.handleSignal(m))
    signal.on('draw', (m: any) => board?.addRemote(m.op as AnnotationOp))
    signal.on('share', () => { /* 远端共享状态：可叠加提示，此处略 */ })

    peerMgr.onRemote = (peerId, stream) => bindRemote(peerId, stream)
    peerMgr.setSend((m: any) => signal?.send(m))

    signal.connect()
    await setupMedia()

    loading.value = false
    await nextTick() // canvas ref 就绪

    // 初始化白板（canvas 此时才在 DOM 中）
    if (boardCanvas.value) board = new Whiteboard(boardCanvas.value)
    ;(window as any).__m5_board = board
    ;(window as any).__m5_signal = signal // 调试钩子：走查用（Playwright evaluate 直接驱动白板/信令）

    // 迟到加入/刷新：拉取已有标注回放
    try {
      const list = await collabApi.getAnnotations(consultationId)
      const ops = list.map(a => JSON.parse(a.payload) as AnnotationOp)
      board?.replay(ops)
    } catch { /* 忽略 */ }
  } catch (e: any) {
    error.value = e.message || '入房失败'
    loading.value = false
  }
}

function syncPeers(list: string[]) {
  peers.length = 0
  list.forEach(id => peers.push({ id }))
  // 重新绑定已存在的远端流
  nextTick(() => peers.forEach(p => {
    const s = streams.get(p.id)
    if (s) {
      const el = document.querySelector<HTMLVideoElement>(`video[data-peer="${p.id}"]`)
      if (el) el.srcObject = s
    }
  }))
}

// —— 白板绘制 ——
function onPointerDown(e: PointerEvent) {
  if (!board) return
  board.tool = tool.value
  board.color = color.value
  if (tool.value === 'text') {
    const t = window.prompt('输入文字标注：')
    if (t) { const op = board.start(e.clientX, e.clientY, t); sendDraw(op) }
    return
  }
  drawing.value = true
  board.start(e.clientX, e.clientY)
}
function onPointerMove(e: PointerEvent) {
  if (drawing.value && board) board.addPoint(e.clientX, e.clientY)
}
function onPointerUp() {
  if (!drawing.value || !board) return
  drawing.value = false
  const op = board.end()
  if (op) sendDraw(op)
}
function sendDraw(op: AnnotationOp | null) {
  if (op) signal?.send({ type: 'draw', op })
}

// —— 桌面共享 ——
async function toggleShare() {
  if (shareState.value === 'unsupported') return
  if (shareState.value === 'off') {
    try {
      const ds = await (navigator.mediaDevices as any).getDisplayMedia({ video: true })
      const track = ds.getVideoTracks()[0]
      screenTrack = track
      await peerMgr.replaceVideoTrack(track)
      shareState.value = 'on'
      signal?.send({ type: 'share', on: true })
      if (track) track.onended = () => stopShare()
    } catch { /* 用户取消或不支持 */ }
  } else stopShare()
}
function stopShare() {
  if (screenTrack) { screenTrack.stop(); screenTrack = null }
  // 恢复摄像头轨（若有）
  const cam = myStream.value?.getVideoTracks()[0]
  if (cam) peerMgr.replaceVideoTrack(cam)
  shareState.value = 'off'
  signal?.send({ type: 'share', on: false })
}

function leave() {
  signal?.close()
  peerMgr.close()
  router.push('/consultations')
}

onMounted(() => {
  // 白板初始化与房间入会在 join() 内完成（待 canvas 渲染、WS 就绪）
  // 检测桌面共享能力
  if (!(navigator.mediaDevices as any)?.getDisplayMedia) shareState.value = 'unsupported'
  join()
  window.addEventListener('resize', () => board?.resize())
})
onUnmounted(() => { signal?.close(); peerMgr.close() })
</script>

<template>
  <div class="room">
    <header class="top">
      <div>
        <h2>协同房间 · {{ title }}</h2>
        <p class="crumb">房间 {{ consultationId }} · 我方 {{ myId }} · {{ shareState === 'on' ? '共享中' : '本地' }}</p>
      </div>
      <div class="me">
        <button class="btn-ghost btn" @click="leave">离开房间</button>
      </div>
    </header>

    <div v-if="loading" class="status">正在进入协同房间…</div>
    <div v-else-if="error" class="status err">{{ error }}</div>

    <template v-else>
      <div class="grid">
        <!-- 本地 -->
        <div class="tile local">
          <span class="tag">本地 · {{ myId }}</span>
          <video ref="localVideo" autoplay muted playsinline></video>
        </div>
        <!-- 远端 -->
        <div class="tile" v-for="p in peers" :key="p.id" v-show="p.id !== myId">
          <span class="tag">{{ p.id }}</span>
          <video :data-peer="p.id" autoplay playsinline></video>
        </div>
      </div>

      <!-- 白板叠加层 -->
      <div class="board-wrap">
        <canvas ref="boardCanvas" class="board"
          @pointerdown="onPointerDown" @pointermove="onPointerMove" @pointerup="onPointerUp" @pointerleave="onPointerUp"></canvas>
        <div class="tools">
          <button class="btn-ghost btn xs" :class="{ on: tool === 'pen' }" @click="tool = 'pen'">✏ 笔</button>
          <button class="btn-ghost btn xs" :class="{ on: tool === 'rect' }" @click="tool = 'rect'">▭ 矩形</button>
          <button class="btn-ghost btn xs" :class="{ on: tool === 'arrow' }" @click="tool = 'arrow'">↗ 箭头</button>
          <button class="btn-ghost btn xs" :class="{ on: tool === 'text' }" @click="tool = 'text'">T 文字</button>
          <input type="color" v-model="color" class="color" title="颜色" />
          <button class="btn primary xs" :disabled="shareState === 'unsupported'"
            @click="toggleShare">
            {{ shareState === 'on' ? '停止共享' : shareState === 'unsupported' ? '不支持共享' : '桌面共享' }}
          </button>
        </div>
        <p class="hint">白板标注实时同步给同房专家；迟到加入会自动回放已有标注。</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.room { display: grid; gap: var(--sp-4); }
.top { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3);
  padding: var(--sp-4) var(--sp-5); border-bottom: 1px solid var(--md-line); background: var(--md-surface); border-radius: var(--r-lg); }
.top h2 { font-size: var(--fs-h2); }
.crumb { color: var(--md-muted); font-size: .82rem; margin: .2rem 0 0; }
.me { display: flex; align-items: center; gap: .6rem; }
.status { padding: 3rem 0; text-align: center; color: var(--md-muted); }
.err { color: #c0392b; }

.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: var(--sp-3); }
.tile { position: relative; background: #000; border-radius: var(--r-md); overflow: hidden; aspect-ratio: 4/3; }
.tile video { width: 100%; height: 100%; object-fit: cover; display: block; }
.tag { position: absolute; top: 6px; left: 6px; z-index: 2; background: rgba(0,0,0,.55); color: #fff;
  font-size: .72rem; padding: .15rem .45rem; border-radius: 4px; }
.tile.local .tag { background: var(--md-blue-600); }

.board-wrap { position: relative; background: var(--md-surface); border: 1px solid var(--md-line);
  border-radius: var(--r-lg); padding: var(--sp-3); }
.board { width: 100%; height: 360px; background:
  linear-gradient(rgba(255,255,255,.04) 1px, transparent 1px) 0 0 / 100% 24px,
  linear-gradient(90deg, rgba(255,255,255,.04) 1px, transparent 1px) 0 0 / 24px 100%, #0c1322;
  border-radius: var(--r-sm); cursor: crosshair; touch-action: none; }
.tools { display: flex; flex-wrap: wrap; gap: .4rem; align-items: center; margin-top: .6rem; }
.color { width: 30px; height: 28px; border: 1px solid var(--md-line); border-radius: var(--r-sm); background: var(--md-bg); padding: 0; }
.btn.xs.on { background: var(--md-blue-600); color: #fff; box-shadow: none; }
.hint { font-size: .76rem; color: var(--md-muted); margin: .5rem 0 0; }
</style>
