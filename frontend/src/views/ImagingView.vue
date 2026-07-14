<script setup lang="ts">
/**
 * ② 影像核心引擎 — WebGL2 MPR 阅片视图（M3 主界面）。
 *
 * 三正交平面（轴位/矢状/冠状）由 VolumeRenderer 在 GPU 渲染，
 * 窗宽窗位 / 缩放 / 切片由鼠标事件或控件驱动。
 *
 * 从路由 query 读取 studyUid，若缺省则回退到数据接入页。
 */
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import {
  VolumeRenderer,
  type MprAxis,
} from '../lib/webgl/VolumeRenderer'
import AppShell from '../components/AppShell.vue'

const route = useRoute()
const router = useRouter()

// ── 就诊信息（路由 query） ──────────────────────────────
const studyUid = ref('')
const patientId = ref('')
const accessionNumber = ref('')
const loading = ref(true)
const error = ref('')
const traceId = ref('')

// ── 渲染器实例 ──────────────────────────────────────────
const axisLabels = ['轴位（Axial XY）', '矢状（Sagittal YZ）', '冠状（Coronal XZ）']
let renderers: VolumeRenderer[] = []

// ── 交互状态 ────────────────────────────────────────────
const slicePositions = ref([0.5, 0.5, 0.5]) // axial, sagittal, coronal
const ww = ref(400)
const wl = ref(40)
const zoom = ref(1)

const mouse = { down: false, mode: 'slice' as 'slice' | 'wl', lastX: 0, lastY: 0, activeCanvas: -1 }

// ── 窗宽窗位预设 ──────────────────────────────────────
interface WlPreset { label: string; ww: number; wl: number }
const presets: Record<string, WlPreset[]> = {
  CT: [
    { label: '腹部', ww: 400, wl: 40 },
    { label: '肺部', ww: 1500, wl: -500 },
    { label: '骨窗', ww: 2000, wl: 500 },
    { label: '脑部', ww: 80, wl: 40 },
  ],
  MRI: [
    { label: 'T1', ww: 800, wl: 400 },
    { label: 'T2', ww: 1500, wl: 700 },
    { label: 'FLAIR', ww: 2000, wl: 1000 },
  ],
}
const activePresets = ref<WlPreset[]>(presets.CT)
const modalityLabel = ref('CT')

// ── 加载 ──────────────────────────────────────────────
async function loadStudy() {
  studyUid.value = (route.query.studyUid as string) || ''
  patientId.value = (route.query.patientId as string) || ''
  accessionNumber.value = (route.query.accessionNumber as string) || ''
  const modality = (route.query.modality as string) || 'CT'

  if (!studyUid.value) {
    router.push('/patients')
    return
  }

  loading.value = true
  error.value = ''
  try {
    const data = await api.fetchVolume(studyUid.value, modality)
    traceId.value = data.traceId || ''
    modalityLabel.value = data.modality
    activePresets.value = presets[data.modality] || presets.CT
    ww.value = data.ww
    wl.value = data.wl

    // 先把 loading=false，触发重渲染出 v-else 的 3 个 canvas
    loading.value = false
    // 等 DOM 渲染出 canvas（直接 querySelectorAll 拿 DOM，比 ref 函数更稳）
    await nextTick()
    const domCanvases = Array.from(document.querySelectorAll('.mpr-canvas')) as HTMLCanvasElement[]
    if (domCanvases.length < 3) throw new Error('MPR 画布未就绪')

    // 初始化 3 个渲染器（同一体数据各传一份，显存约 18MB）
    renderers.forEach(r => r.destroy())
    renderers = [0, 1, 2].map((axis, i) => {
      const r = new VolumeRenderer(domCanvases[i], axis as MprAxis)
      r.init()
      r.loadVolume(data.voxels, data.dims, data.ww, data.wl)
      return r
    })
  } catch (e: any) {
    error.value = e.message || '加载体数据失败'
    loading.value = false
  }
}

// ── 控件回调 ────────────────────────────────────────────
function onSliceChange(axis: number, pos: number) {
  slicePositions.value[axis] = pos
  renderers[axis]?.setSlice(pos)
}

function applyWl(wwv: number, wlv: number) {
  ww.value = wwv
  wl.value = wlv
  renderers.forEach(r => r.setWindowLevel(wwv, wlv))
}

function applyPreset(p: WlPreset) { applyWl(p.ww, p.wl) }

function resetZoom() {
  zoom.value = 1
  renderers.forEach(r => r.setZoom(1))
}

// ── Canvas 鼠标事件 ──────────────────────────────────────
function onMousedown(e: MouseEvent, axis: number) {
  mouse.down = true
  mouse.lastX = e.clientX
  mouse.lastY = e.clientY
  mouse.activeCanvas = axis
  mouse.mode = e.button === 2 ? 'wl' : 'slice'
}

function onMousemove(e: MouseEvent, _axis: number) {
  if (!mouse.down || mouse.activeCanvas < 0) return
  const dx = e.clientX - mouse.lastX
  const dy = e.clientY - mouse.lastY
  mouse.lastX = e.clientX
  mouse.lastY = e.clientY

  if (mouse.mode === 'slice') {
    const pos = Math.max(0, Math.min(1,
      slicePositions.value[mouse.activeCanvas] + dy * 0.004))
    onSliceChange(mouse.activeCanvas, pos)
  } else {
    // window/level drag
    applyWl(Math.max(1, ww.value + dx * 2),
            Math.max(-1024, Math.min(3071, wl.value + dy * 2)))
  }
}

function onMouseup() { mouse.down = false; mouse.activeCanvas = -1 }

function onWheel(e: WheelEvent, axis: number) {
  if (e.ctrlKey || e.metaKey) {
    const f = e.deltaY > 0 ? 0.9 : 1.1
    zoom.value = Math.max(0.1, Math.min(20, zoom.value * f))
    renderers.forEach(r => r.setZoom(zoom.value))
  } else {
    const pos = Math.max(0, Math.min(1,
      slicePositions.value[axis] + e.deltaY * 0.002))
    onSliceChange(axis, pos)
  }
  e.preventDefault()
}

function onContextMenu(e: Event) { e.preventDefault() }

// ── 键盘快捷键 ──────────────────────────────────────────
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'r' || e.key === 'R') resetZoom()
}

onMounted(() => {
  loadStudy()
  window.addEventListener('mouseup', onMouseup)
  window.addEventListener('keydown', onKeydown)
})

// 同路径不同 query 切换时，Vue Router 复用组件，onMounted 不触发 → 需 watch
watch(() => route.query.studyUid, () => {
  // 清理旧渲染器后再加载新数据
  renderers.forEach(r => r.destroy())
  renderers.length = 0
  loadStudy()
})

onUnmounted(() => {
  renderers.forEach(r => r.destroy())
  window.removeEventListener('mouseup', onMouseup)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <AppShell title="影像中心 · MPR 阅片" :crumb="`${studyUid} | ${patientId} / ${accessionNumber}`">
    <!-- 加载态 -->
    <div v-if="loading" class="status">加载体数据中…</div>
    <div v-else-if="error" class="status err">{{ error }}</div>

    <template v-else>
      <!-- 三正交平面 -->
      <div class="mpr-grid">
        <div v-for="(label, i) in axisLabels" :key="label" class="view">
          <div class="view-label">
            {{ label }}
            <span class="slice-idx">{{ Math.round(slicePositions[i] * 100) }}%</span>
          </div>
          <canvas
            class="mpr-canvas"
            @mousedown="onMousedown($event, i)"
            @mousemove="onMousemove($event, i)"
            @wheel="onWheel($event, i)"
            @contextmenu="onContextMenu"
          />
          <input
            type="range" min="0" max="1" step="0.002"
            :value="slicePositions[i]"
            class="slider"
            @input="onSliceChange(i, Number(($event.target as HTMLInputElement).value))"
          />
        </div>
      </div>

      <!-- 控制栏 -->
      <div class="controls">
        <div class="ctrl-group">
          <span class="ctrl-label">窗宽</span>
          <input type="range" min="1" max="4000" :value="ww" class="slider narrow"
            @input="applyWl(Number(($event.target as HTMLInputElement).value), wl)" />
          <span class="val">{{ ww }}</span>
        </div>
        <div class="ctrl-group">
          <span class="ctrl-label">窗位</span>
          <input type="range" min="-1024" max="3071" :value="wl" class="slider narrow"
            @input="applyWl(ww, Number(($event.target as HTMLInputElement).value))" />
          <span class="val">{{ wl }}</span>
        </div>
        <div class="ctrl-group">
          <span class="ctrl-label">缩放</span>
          <span class="val">{{ zoom.toFixed(1) }}×</span>
          <button class="btn-ghost btn xs" @click="resetZoom">重置</button>
        </div>

        <div class="ctrl-group presets">
          <span class="ctrl-label">{{ modalityLabel }} 预设</span>
          <button
            v-for="p in activePresets" :key="p.label"
            class="btn-ghost btn xs"
            :class="{ on: ww === p.ww && wl === p.wl }"
            @click="applyPreset(p)"
          >{{ p.label }}</button>
        </div>
      </div>

      <!-- 操作提示 -->
      <div class="hints">
        <span>🖱 左键拖动 → 调整切片 · 右键拖动 → 窗宽窗位</span>
        <span>🖱 <kbd>Ctrl</kbd> + 滚轮 → 缩放 · 滚轮 → 切片</span>
        <span v-if="traceId" class="tid">TraceId {{ traceId.slice(0, 12) }}…</span>
      </div>
    </template>
  </AppShell>
</template>

<style scoped>
.status { padding: 3rem 0; text-align: center; color: var(--md-muted); font-size: .95rem; }
.err { color: #c0392b; }

.mpr-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--sp-3);
}
@media (max-width: 860px) { .mpr-grid { grid-template-columns: 1fr 1fr; } }
@media (max-width: 560px) { .mpr-grid { grid-template-columns: 1fr; } }

.view {
  background: #000;
  border-radius: var(--r-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.view-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem 0.65rem;
  background: oklch(28% 0.03 250 / 0.85);
  color: #b8ceff;
  font-size: .75rem;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.slice-idx { font-size: .68rem; color: #8aaeef; font-family: ui-monospace, monospace; }
.mpr-canvas {
  width: 100%;
  aspect-ratio: 1;
  display: block;
  cursor: grab;
  touch-action: none;
}
.mpr-canvas:active { cursor: grabbing; }

.slider {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 4px;
  background: oklch(60% 0.03 250 / 0.3);
  border-radius: 2px;
  outline: none;
  margin: 0;
  flex: none;
}
.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px; height: 12px;
  border-radius: 50%;
  background: var(--md-blue-400);
  cursor: pointer;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sp-3) var(--sp-4);
  padding: var(--sp-3) var(--sp-4);
  background: var(--md-surface);
  border: 1px solid var(--md-line);
  border-radius: var(--r-lg);
  margin-top: var(--sp-3);
}
.ctrl-group {
  display: flex;
  align-items: center;
  gap: .5rem;
  flex: none;
}
.ctrl-label { font-size: .78rem; color: var(--md-muted); font-weight: 600; white-space: nowrap; }
.val { font-family: ui-monospace, monospace; font-size: .8rem; min-width: 3rem; color: var(--md-ink); }
.slider.narrow { width: 100px; }

.presets { gap: .35rem; flex-wrap: wrap; }
.btn.xs { padding: .3rem .6rem; font-size: .74rem; border-radius: var(--r-sm); }
.btn.xs.on { background: var(--md-blue-600); color: #fff; box-shadow: none; }

.hints {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: .6rem var(--sp-3);
  font-size: .76rem;
  color: var(--md-muted);
  margin-top: .5rem;
}
.hints kbd {
  display: inline-block;
  font-family: inherit;
  padding: .05rem .4rem;
  background: var(--md-blue-50);
  border-radius: 4px;
  font-size: .72rem;
  border: 1px solid var(--md-line);
}
.tid {
  font-family: ui-monospace, monospace;
  font-size: .7rem;
  color: var(--md-blue-600);
  margin-left: auto;
}
</style>
