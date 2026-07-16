<script setup lang="ts">
/**
 * ② 影像核心引擎 — WebGL2 MPR 阅片视图（M3 主界面 + M3 增强 Backlog GAP-6/7/8）。
 *
 * 三正交平面（轴位/矢状/冠状）由 VolumeRenderer 在 GPU 渲染；
 * 本版新增：反相(invert)、电影播放(cine)、悬挂布局(hanging)、
 * 文字注释(text annotation)、相关影像关联(related images)、跨机构发布(publish)。
 */
import { ref, reactive, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, type StudyRecord, type ClinicalCategories } from '../api'
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
const patientVisitUid = ref('')
const accessionNumber = ref('')
const loading = ref(true)
const error = ref('')
const traceId = ref('')

// ── 渲染器实例 ──────────────────────────────────────────
const axisLabels = ['轴位（Axial XY）', '矢状（Sagittal YZ）', '冠状（Coronal XZ）']
let renderers: VolumeRenderer[] = []

// ── 交互状态 ────────────────────────────────────────────
const slicePositions = reactive([0.5, 0.5, 0.5]) // axial, sagittal, coronal
const ww = ref(400)
const wl = ref(40)
const zoom = ref(1)
const invert = ref(false)

// GAP-6 电影播放
const cine = reactive({ playing: false, fps: 12, axis: 0, dir: 1 })
let cineTimer: number | null = null

// GAP-6 悬挂布局：1×1 / 1×2 / 2×2
// GAP-6 悬挂布局：1×1 / 1×2 / 2×2（show 为可见平面的 MprAxis 列表）
const layouts: { key: string; label: string; show: number[] }[] = [
  { key: '1x1', label: '单视图', show: [0] },
  { key: '1x2', label: '双联',   show: [0, 1] },
  { key: '2x2', label: '四联',   show: [0, 1, 2] },
]
const layout = ref<string>('2x2')
const visibleAxes = computed<number[]>(() => layouts.find(l => l.key === layout.value)!.show)

// GAP-7 文字注释（按平面归属）
interface Annotation { id: number; axis: number; x: number; y: number; text: string }
const annotations = reactive<Annotation[]>([])
const annoMode = ref(false)
const annoDraft = reactive({ axis: 0, x: 0, y: 0 })
let annoSeq = 1

// GAP-8 相关影像
const related = ref<StudyRecord[]>([])
const linked = reactive<Record<string, boolean>>({})

// GAP-7 跨机构发布
const publishState = ref<'idle' | 'publishing' | 'done'>('idle')
const publishId = ref('')

// GAP-9 诊疗数据细类（入院病历 / 长期医嘱 / 临时医嘱）
const clinical = ref<ClinicalCategories | null>(null)

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
  patientVisitUid.value = (route.query.patientVisitUid as string) || ''
  accessionNumber.value = (route.query.accessionNumber as string) || ''
  const modality = (route.query.modality as string) || 'CT'

  if (!studyUid.value) { router.push('/patients'); return }

  loading.value = true
  error.value = ''
  publishState.value = 'idle'
  try {
    const data = await api.fetchVolume(studyUid.value, modality)
    traceId.value = data.traceId || ''
    modalityLabel.value = data.modality
    activePresets.value = presets[data.modality] || presets.CT
    ww.value = data.ww
    wl.value = data.wl

    loading.value = false
    await nextTick()
    await mountRenderers()
    await loadRelated()
    await loadClinical()
  } catch (e: any) {
    error.value = e.message || '加载体数据失败'
    loading.value = false
  }
}

// 依据当前悬挂布局挂载可见渲染器
async function mountRenderers() {
  const domCanvases = Array.from(document.querySelectorAll('.mpr-canvas')) as HTMLCanvasElement[]
  if (domCanvases.length < visibleAxes.value.length) throw new Error('MPR 画布未就绪')
  renderers.forEach(r => r.destroy())
  renderers = visibleAxes.value.map((axis, i) => {
    const r = new VolumeRenderer(domCanvases[i], axis as MprAxis)
    r.init()
    return r
  })
  // 重新拉取体数据并应用当前窗宽窗位/反相/缩放
  const modality = (route.query.modality as string) || 'CT'
  const data = await api.fetchVolume(studyUid.value, modality)
  renderers.forEach(r => {
    r.loadVolume(data.voxels, data.dims, ww.value, wl.value)
    r.setInvert(invert.value)
    r.setZoom(zoom.value)
  })
  applySliceAll()
  // 注释按可见平面恢复显示
  renderAnnotations()
}

// ── 控件回调 ────────────────────────────────────────────
function onSliceChange(axis: number, pos: number) {
  slicePositions[axis] = pos
  renderers[visibleAxes.value.indexOf(axis)]?.setSlice(pos)
}

function applyWl(wwv: number, wlv: number) {
  ww.value = wwv; wl.value = wlv
  renderers.forEach(r => r.setWindowLevel(wwv, wlv))
}
function applyPreset(p: WlPreset) { applyWl(p.ww, p.wl) }
function resetZoom() { zoom.value = 1; renderers.forEach(r => r.setZoom(1)) }
function toggleInvert() {
  invert.value = !invert.value
  renderers.forEach(r => r.setInvert(invert.value))
}
function applySliceAll() {
  visibleAxes.value.forEach((axis, i) => renderers[i]?.setSlice(slicePositions[axis]))
}

// ── GAP-6 电影播放 ─────────────────────────────────────
function toggleCine() {
  cine.playing = !cine.playing
  if (cine.playing) startCine()
  else stopCine()
}
function startCine() {
  stopCine()
  const stepMs = 1000 / cine.fps
  cineTimer = window.setInterval(() => {
    let p = slicePositions[cine.axis] + cine.dir * 0.01
    if (p >= 1) { p = 1; cine.dir = -1 }
    if (p <= 0) { p = 0; cine.dir = 1 }
    slicePositions[cine.axis] = p
    renderers[visibleAxes.value.indexOf(cine.axis)]?.setSlice(p)
  }, stepMs)
}
function stopCine() { if (cineTimer !== null) { clearInterval(cineTimer); cineTimer = null } }

// ── GAP-6 悬挂布局切换 ─────────────────────────────────
async function setLayout(key: typeof layouts[number]['key']) {
  layout.value = key
  stopCine()
  await nextTick()
  await mountRenderers()
}

// ── GAP-7 文字注释 ─────────────────────────────────────
function onCanvasClick(e: MouseEvent, axis: number) {
  if (!annoMode.value) return
  const rect = (e.currentTarget as HTMLCanvasElement).getBoundingClientRect()
  annoDraft.axis = axis
  annoDraft.x = (e.clientX - rect.left) / rect.width
  annoDraft.y = (e.clientY - rect.top) / rect.height
  const text = window.prompt('输入标注文字：')
  if (text && text.trim()) {
    annotations.push({ id: annoSeq++, axis, x: annoDraft.x, y: annoDraft.y, text: text.trim() })
  }
  annoMode.value = false
}
function removeAnno(id: number) {
  const i = annotations.findIndex(a => a.id === id)
  if (i >= 0) annotations.splice(i, 1)
}
// 注释以 HTML 叠加层定位在对应画布上（仅渲染可见平面）
function renderAnnotations() { /* 叠加层由模板 v-for 驱动，无需手动绘制 */ }

// ── GAP-8 相关影像 ─────────────────────────────────────
async function loadRelated() {
  try {
    const r = await api.listStudies()
    related.value = r.data.filter(s =>
      s.patientId === patientId.value && s.studyInstanceUid !== studyUid.value)
    related.value.forEach(s => { if (!(s.studyInstanceUid in linked)) linked[s.studyInstanceUid] = false })
  } catch { /* 静默降级 */ }
}
function openRelated(s: StudyRecord) {
  router.push({
    path: '/imaging',
    query: {
      studyUid: s.studyInstanceUid, patientId: s.patientId,
      accessionNumber: s.accessionNumber, patientVisitUid: s.patientVisitUid || '',
      modality: s.modality,
    },
  })
}
function toggleLink(uid: string) { linked[uid] = !linked[uid] }

// ── GAP-9 诊疗数据细类 ─────────────────────────────────
async function loadClinical() {
  if (!patientVisitUid.value) return
  try {
    const r = await api.fetchClinical(patientVisitUid.value)
    const parsed = JSON.parse(r.data.emrJson || '{}') as any
    clinical.value = (parsed.categories as ClinicalCategories) || null
  } catch { clinical.value = null }
}

// ── GAP-7 跨机构发布 ───────────────────────────────────
async function publish() {
  if (publishState.value === 'publishing') return
  publishState.value = 'publishing'
  try {
    const r = await api.publishStudy({
      studyInstanceUid: studyUid.value, patientId: patientId.value,
      accessionNumber: accessionNumber.value, tenantId: 'T001',
    })
    publishId.value = r.publishId || ''
    publishState.value = 'done'
  } catch { publishState.value = 'idle' }
}

// ── Canvas 鼠标事件 ──────────────────────────────────────
const mouse = { down: false, mode: 'slice' as 'slice' | 'wl', lastX: 0, lastY: 0, activeCanvas: -1 }
function onMousedown(e: MouseEvent, axis: number) {
  if (annoMode.value) return
  mouse.down = true; mouse.lastX = e.clientX; mouse.lastY = e.clientY
  mouse.activeCanvas = visibleAxes.value.indexOf(axis)
  mouse.mode = e.button === 2 ? 'wl' : 'slice'
}
function onMousemove(e: MouseEvent, _axis: number) {
  if (!mouse.down || mouse.activeCanvas < 0) return
  const dx = e.clientX - mouse.lastX, dy = e.clientY - mouse.lastY
  mouse.lastX = e.clientX; mouse.lastY = e.clientY
  const axis = visibleAxes.value[mouse.activeCanvas]
  if (mouse.mode === 'slice') {
    const pos = Math.max(0, Math.min(1, slicePositions[axis] + dy * 0.004))
    onSliceChange(axis, pos)
  } else {
    applyWl(Math.max(1, ww.value + dx * 2), Math.max(-1024, Math.min(3071, wl.value + dy * 2)))
  }
}
function onMouseup() { mouse.down = false; mouse.activeCanvas = -1 }
function onWheel(e: WheelEvent, axis: number) {
  if (annoMode.value) return
  if (e.ctrlKey || e.metaKey) {
    zoom.value = Math.max(0.1, Math.min(20, zoom.value * (e.deltaY > 0 ? 0.9 : 1.1)))
    renderers.forEach(r => r.setZoom(zoom.value))
  } else {
    const pos = Math.max(0, Math.min(1, slicePositions[axis] + e.deltaY * 0.002))
    onSliceChange(axis, pos)
  }
  e.preventDefault()
}
function onContextMenu(e: Event) { e.preventDefault() }
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'r' || e.key === 'R') resetZoom()
  if (e.key === 'i' || e.key === 'I') toggleInvert()
}

onMounted(() => {
  loadStudy()
  window.addEventListener('mouseup', onMouseup)
  window.addEventListener('keydown', onKeydown)
})
watch(() => route.query.studyUid, () => {
  renderers.forEach(r => r.destroy()); renderers.length = 0
  annotations.length = 0
  loadStudy()
})
onUnmounted(() => {
  stopCine()
  renderers.forEach(r => r.destroy())
  window.removeEventListener('mouseup', onMouseup)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <AppShell title="影像中心 · MPR 阅片" :crumb="`${studyUid} | ${patientId} / ${accessionNumber}`">
    <div v-if="loading" class="status">加载体数据中…</div>
    <div v-else-if="error" class="status err">{{ error }}</div>

    <template v-else>
      <div class="work">
        <!-- 主区：三正交平面（按悬挂布局显隐） -->
        <div class="mpr-grid" :class="'lay-' + layout">
          <div v-for="axis in visibleAxes" :key="axis" class="view">
            <div class="view-label">
              {{ axisLabels[axis] }}
              <span class="slice-idx">{{ Math.round(slicePositions[axis] * 100) }}%</span>
            </div>
            <div class="canvas-wrap">
              <canvas
                class="mpr-canvas"
                @mousedown="onMousedown($event, axis)"
                @mousemove="onMousemove($event, axis)"
                @wheel="onWheel($event, axis)"
                @contextmenu="onContextMenu"
                @click="onCanvasClick($event, axis)"
              />
              <!-- GAP-7 文字注释叠加层 -->
              <div
                v-for="a in annotations.filter(x => x.axis === axis)" :key="a.id"
                class="anno" :style="{ left: (a.x * 100) + '%', top: (a.y * 100) + '%' }">
                <span class="anno-text">{{ a.text }}</span>
                <button class="anno-del" @click.stop="removeAnno(a.id)" title="删除标注">×</button>
              </div>
            </div>
            <input type="range" min="0" max="1" step="0.002" :value="slicePositions[axis]"
              class="slider" @input="onSliceChange(axis, Number(($event.target as HTMLInputElement).value))" />
          </div>
        </div>

        <!-- GAP-8 相关影像侧栏 -->
        <aside class="side">
          <h4>相关影像</h4>
          <p class="side-hint" v-if="!related.length">该患者暂无其它检查记录。</p>
          <ul class="rel-list">
            <li v-for="s in related" :key="s.studyInstanceUid" class="rel-item">
              <div class="rel-meta">
                <b>{{ s.modality }}</b>
                <small>{{ s.accessionNumber }}</small>
              </div>
              <div class="rel-acts">
                <button class="btn-ghost btn xs" @click="openRelated(s)">打开</button>
                <button class="btn-ghost btn xs" :class="{ on: linked[s.studyInstanceUid] }"
                  @click="toggleLink(s.studyInstanceUid)">
                  {{ linked[s.studyInstanceUid] ? '已关联' : '关联' }}
                </button>
              </div>
            </li>
          </ul>

          <h4 class="mt">文字注释</h4>
          <button class="btn-ghost btn sm" :class="{ on: annoMode }" @click="annoMode = !annoMode">
            {{ annoMode ? '点击画布放置中…' : '＋ 添加标注' }}
          </button>
          <p class="side-hint">{{ annotations.length }} 条标注</p>

          <h4 class="mt">跨机构发布</h4>
          <button class="btn primary sm" :disabled="publishState !== 'idle'" @click="publish">
            {{ publishState === 'publishing' ? '发布中…' : publishState === 'done' ? '已发布' : '发布到区域影像平台' }}
          </button>
          <p class="side-hint" v-if="publishState === 'done'">发布号：{{ publishId }}</p>

          <template v-if="clinical">
            <h4 class="mt">诊疗细类（GAP-9）</h4>
            <div class="cat" v-if="clinical.admissionRecord">
              <span class="cat-t">入院病历</span>
              <p class="cat-line">科室 {{ clinical.admissionRecord.dept }} · 主治医师 {{ clinical.admissionRecord.attending }}</p>
              <p class="cat-line">主诉：{{ clinical.admissionRecord.chiefComplaint }}</p>
              <p class="cat-line">诊断：{{ clinical.admissionRecord.diagnosis }}</p>
            </div>
            <div class="cat" v-if="clinical.longTermOrders">
              <span class="cat-t">{{ clinical.longTermOrders.type }}</span>
              <p class="cat-line" v-for="(o, i) in clinical.longTermOrders.items" :key="'l'+i">{{ o.name }}（{{ o.freq }}）</p>
            </div>
            <div class="cat" v-if="clinical.tempOrders">
              <span class="cat-t">{{ clinical.tempOrders.type }}</span>
              <p class="cat-line" v-for="(o, i) in clinical.tempOrders.items" :key="'t'+i">{{ o.name }}（{{ o.freq }}）</p>
            </div>
          </template>
        </aside>
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
          <button v-for="p in activePresets" :key="p.label" class="btn-ghost btn xs"
            :class="{ on: ww === p.ww && wl === p.wl }" @click="applyPreset(p)">{{ p.label }}</button>
        </div>

        <!-- GAP-6 反相 / 电影 / 悬挂 -->
        <div class="ctrl-group">
          <button class="btn-ghost btn xs" :class="{ on: invert }" @click="toggleInvert">反相 (I)</button>
        </div>
        <div class="ctrl-group">
          <button class="btn-ghost btn xs" :class="{ on: cine.playing }" @click="toggleCine">
            {{ cine.playing ? '⏸ 暂停' : '▶ 电影' }}
          </button>
          <select v-model.number="cine.fps" class="sel xs" title="帧率">
            <option :value="6">6fps</option><option :value="12">12fps</option><option :value="24">24fps</option>
          </select>
          <select v-model.number="cine.axis" class="sel xs" title="播放平面">
            <option v-for="(l, i) in axisLabels" :key="i" :value="i">{{ l.slice(0, 2) }}</option>
          </select>
        </div>
        <div class="ctrl-group">
          <span class="ctrl-label">布局</span>
          <button v-for="l in layouts" :key="l.key" class="btn-ghost btn xs"
            :class="{ on: layout === l.key }" @click="setLayout(l.key)">{{ l.label }}</button>
        </div>
      </div>

      <div class="hints">
        <span>🖱 左键拖动 → 切片 · 右键拖动 → 窗宽窗位</span>
        <span>🖱 <kbd>Ctrl</kbd>+滚轮 → 缩放 · <kbd>I</kbd> 反相 · <kbd>R</kbd> 重置</span>
        <span v-if="traceId" class="tid">TraceId {{ traceId.slice(0, 12) }}…</span>
      </div>
    </template>
  </AppShell>
</template>

<style scoped>
.status { padding: 3rem 0; text-align: center; color: var(--md-muted); font-size: .95rem; }
.err { color: #c0392b; }
.work { display: grid; grid-template-columns: 1fr 320px; gap: var(--sp-3); align-items: start; }
@media (max-width: 1100px) { .work { grid-template-columns: 1fr 280px; } }
@media (max-width: 860px) { .work { grid-template-columns: 1fr; } }

.mpr-grid { display: grid; gap: var(--sp-3); }
.mpr-grid.lay-1x1 { grid-template-columns: 1fr; }
.mpr-grid.lay-1x2 { grid-template-columns: 1fr 1fr; }
.mpr-grid.lay-2x2 { grid-template-columns: 1fr 1fr; }
@media (max-width: 560px) { .mpr-grid.lay-1x2, .mpr-grid.lay-2x2 { grid-template-columns: 1fr; } }

.view { background: #000; border-radius: var(--r-md); overflow: hidden; display: flex; flex-direction: column; }
.view-label {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0.4rem 0.65rem; background: oklch(28% 0.03 250 / 0.85);
  color: #b8ceff; font-size: .75rem; font-weight: 600; letter-spacing: .3px;
}
.slice-idx { font-size: .68rem; color: #8aaeef; font-family: ui-monospace, monospace; }
.canvas-wrap { position: relative; }
.mpr-canvas { width: 100%; aspect-ratio: 1; display: block; cursor: grab; touch-action: none; }
.mpr-canvas:active { cursor: grabbing; }
.mpr-canvas.anno-on { cursor: crosshair; }

.anno {
  position: absolute; transform: translate(-2px, -2px); pointer-events: auto;
  display: flex; align-items: center; gap: 2px;
}
.anno-text {
  background: rgba(255, 196, 0, .92); color: #1a1a1a; font-size: .72rem; font-weight: 600;
  padding: .1rem .4rem; border-radius: 4px; white-space: nowrap; box-shadow: 0 1px 3px rgba(0,0,0,.4);
}
.anno-del {
  border: none; background: #c0392b; color: #fff; width: 16px; height: 16px; border-radius: 50%;
  font-size: .7rem; line-height: 1; cursor: pointer; padding: 0;
}

/* GAP-8 侧栏 */
.side {
  background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md);
  padding: var(--sp-3); display: flex; flex-direction: column; gap: .35rem;
}
.side h4 { margin: .2rem 0 .1rem; font-size: .82rem; color: var(--md-ink); }
.side h4.mt { margin-top: var(--sp-3); }
.side-hint { font-size: .72rem; color: var(--md-muted); margin: 0; }
.rel-list { list-style: none; margin: 0; padding: 0; display: grid; gap: .4rem; }
.rel-item {
  border: 1px solid var(--md-line); border-radius: var(--r-sm); padding: .4rem .5rem;
  display: flex; align-items: center; justify-content: space-between; gap: .4rem; flex-wrap: wrap;
}
.rel-meta b { font-size: .8rem; color: var(--md-blue-700); margin-right: .4rem; }
.rel-meta small { font-size: .72rem; color: var(--md-muted); }
.rel-acts { display: flex; gap: .3rem; }

.slider {
  -webkit-appearance: none; appearance: none; width: 100%; height: 4px;
  background: oklch(60% 0.03 250 / 0.3); border-radius: 2px; outline: none; margin: 0; flex: none;
}
.slider::-webkit-slider-thumb {
  -webkit-appearance: none; width: 12px; height: 12px; border-radius: 50%;
  background: var(--md-blue-400); cursor: pointer;
}
.controls {
  display: flex; flex-wrap: wrap; align-items: center; gap: var(--sp-3) var(--sp-4);
  padding: var(--sp-3) var(--sp-4); background: var(--md-surface);
  border: 1px solid var(--md-line); border-radius: var(--r-lg); margin-top: var(--sp-3);
}
.ctrl-group { display: flex; align-items: center; gap: .5rem; flex: none; }
.ctrl-label { font-size: .78rem; color: var(--md-muted); font-weight: 600; white-space: nowrap; }
.val { font-family: ui-monospace, monospace; font-size: .8rem; min-width: 3rem; color: var(--md-ink); }
.slider.narrow { width: 100px; }
.presets { gap: .35rem; flex-wrap: wrap; }
.sel { border: 1px solid var(--md-line); border-radius: var(--r-sm); padding: .2rem .3rem; font-size: .72rem; background: var(--md-bg); }
.btn.xs { padding: .3rem .6rem; font-size: .74rem; border-radius: var(--r-sm); }
.btn.xs.on { background: var(--md-blue-600); color: #fff; box-shadow: none; }
.btn.sm { padding: .35rem .7rem; font-size: .8rem; }

.cat { border: 1px solid var(--md-line); border-radius: var(--r-sm); padding: .4rem .5rem; margin-bottom: .4rem; }
.cat-t { font-size: .74rem; font-weight: 700; color: var(--md-blue-700); display: block; margin-bottom: .2rem; }
.cat-line { font-size: .74rem; color: var(--md-ink); margin: .1rem 0; line-height: 1.45; }

.hints { display: flex; flex-wrap: wrap; align-items: center; gap: .6rem var(--sp-3);
  font-size: .76rem; color: var(--md-muted); margin-top: .5rem; }
.hints kbd { display: inline-block; font-family: inherit; padding: .05rem .4rem;
  background: var(--md-blue-50); border-radius: 4px; font-size: .72rem; border: 1px solid var(--md-line); }
.tid { font-family: ui-monospace, monospace; font-size: .7rem; color: var(--md-blue-600); margin-left: auto; }
</style>
