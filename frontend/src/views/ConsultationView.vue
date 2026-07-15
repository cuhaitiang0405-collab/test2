<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { auth } from '../store/auth'
import {
  consultationApi,
  type Consultation,
} from '../api'
import AppShell from '../components/AppShell.vue'

// M4 无独立专家目录服务，内置模拟专家名录用于演示「全员确认门控」
const MOCK_EXPERTS = [
  { id: 'EXP-ONCO', name: '张明 · 肿瘤科主任医师' },
  { id: 'EXP-RAD',  name: '李华 · 放射科副主任医师' },
  { id: 'EXP-SURG', name: '王强 · 外科主任医师' },
  { id: 'EXP-PATH', name: '赵敏 · 病理科主治医师' },
]

// 状态元信息（标签 + 配色）
const STATUS_META: Record<string, { label: string; cls: string }> = {
  APPLIED:     { label: '已申请', cls: 'st-applied' },
  NOTIFIED:    { label: '已通知', cls: 'st-notified' },
  CONFIRMED:   { label: '已确认', cls: 'st-confirmed' },
  IN_PROGRESS: { label: '进行中', cls: 'st-progress' },
  COMPLETED:   { label: '已完成', cls: 'st-completed' },
  CANCELLED:   { label: '已取消', cls: 'st-cancelled' },
}

const STATUS_FILTERS = [
  { key: '', label: '全部' },
  { key: 'APPLIED', label: '已申请' },
  { key: 'NOTIFIED', label: '已通知' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'IN_PROGRESS', label: '进行中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

type View = 'list' | 'detail' | 'form'
const view = ref<View>('list')
const statusFilter = ref('')
const loading = ref(false)
const error = ref('')
const list = ref<Consultation[]>([])
const selected = ref<Consultation | null>(null)
const summaryText = ref('')

// —— 申请表单 ——
const form = ref({
  patientVisitUid: '',
  patientId: '',
  accessionNumber: '',
  title: '',
  reason: '',
  applicant: auth.state.username || '',
  expertIds: [] as string[],
})

const canApply = computed(() =>
  form.value.patientVisitUid.trim() &&
  form.value.patientId.trim() &&
  form.value.title.trim() &&
  form.value.expertIds.length > 0
)

async function loadList() {
  loading.value = true; error.value = ''
  try {
    const r = await consultationApi.list(statusFilter.value || undefined)
    list.value = r.data
  } catch (e: any) { error.value = e?.message || '加载会诊列表失败' }
  finally { loading.value = false }
}

function openForm() { view.value = 'form'; error.value = '' }

function backToList() {
  view.value = 'list'; selected.value = null; summaryText.value = ''
  loadList()
}

async function openDetail(id: string) {
  loading.value = true; error.value = ''
  try {
    const r = await consultationApi.get(id)
    selected.value = r.data
    summaryText.value = r.data.summaryText || ''
    view.value = 'detail'
  } catch (e: any) { error.value = e?.message || '加载会诊详情失败' }
  finally { loading.value = false }
}

function toggleExpert(id: string) {
  const i = form.value.expertIds.indexOf(id)
  if (i >= 0) form.value.expertIds.splice(i, 1)
  else form.value.expertIds.push(id)
}

async function submitApply() {
  if (!canApply.value) return
  loading.value = true; error.value = ''
  const ids = form.value.expertIds
  const names = MOCK_EXPERTS.filter(e => ids.includes(e.id)).map(e => e.name)
  try {
    await consultationApi.apply({
      patientVisitUid: form.value.patientVisitUid.trim(),
      patientId: form.value.patientId.trim(),
      accessionNumber: form.value.accessionNumber.trim() || undefined,
      title: form.value.title.trim(),
      reason: form.value.reason.trim(),
      applicant: form.value.applicant.trim() || auth.state.username || 'WEB',
      expertIds: ids,
      expertNames: names,
    })
    form.value = {
      patientVisitUid: '', patientId: '', accessionNumber: '', title: '',
      reason: '', applicant: auth.state.username || '', expertIds: []
    }
    backToList()
  } catch (e: any) { error.value = e?.message || '申请失败' }
  finally { loading.value = false }
}

// 变更类操作：后端返回更新后的完整视图，直接刷新 selected
async function act(fn: () => Promise<Consultation>) {
  if (!selected.value) return
  loading.value = true; error.value = ''
  try {
    selected.value = await fn()
    await loadList()
  } catch (e: any) { error.value = e?.message || '操作失败' }
  finally { loading.value = false }
}

function doNotify()  { if (selected.value) act(() => consultationApi.notify(selected.value!.consultationId)) }
function doConfirm(expId: string) { if (selected.value) act(() => consultationApi.confirm(selected.value!.consultationId, expId)) }
function doStart()   { if (selected.value) act(() => consultationApi.start(selected.value!.consultationId)) }
function doCancel()  { if (selected.value) act(() => consultationApi.cancel(selected.value!.consultationId)) }
async function doComplete() {
  if (!selected.value) return
  loading.value = true; error.value = ''
  try {
    selected.value = await consultationApi.complete(selected.value.consultationId, summaryText.value.trim())
    await loadList()
  } catch (e: any) { error.value = e?.message || '完成失败' }
  finally { loading.value = false }
}

const confirmedCount = computed(() =>
  selected.value ? selected.value.experts.filter(e => e.confirmed).length : 0
)
const statusLabel = (s: string) => STATUS_META[s]?.label ?? s

onMounted(loadList)
</script>

<template>
  <AppShell title="会诊中心" crumb="多学科协同会诊 · 业务流程层（M4）">
    <div v-if="error" class="alert">{{ error }}</div>

    <!-- ============ 列表视图 ============ -->
    <template v-if="view === 'list'">
      <div class="toolbar">
        <div class="filters">
          <button v-for="f in STATUS_FILTERS" :key="f.key"
            class="chip filt" :class="{ on: statusFilter === f.key }"
            @click="statusFilter = f.key; loadList()">{{ f.label }}</button>
        </div>
        <button class="btn primary" @click="openForm">＋ 新建会诊</button>
      </div>

      <p v-if="loading" class="empty">加载中…</p>
      <p v-else-if="!list.length" class="empty">暂无会诊记录，点击「新建会诊」发起一次多学科会诊。</p>

      <div v-else class="cards">
        <button v-for="c in list" :key="c.consultationId" class="row-card"
          @click="openDetail(c.consultationId)">
          <div class="row-main">
            <div class="row-top">
              <strong class="row-title">{{ c.title }}</strong>
              <span class="badge" :class="STATUS_META[c.status]?.cls">{{ statusLabel(c.status) }}</span>
            </div>
            <div class="row-meta">
              <span class="mono">患者 {{ c.patientId }}</span>
              <span class="dot">·</span>
              <span>申请人 {{ c.applicant }}</span>
              <span class="dot">·</span>
              <span>{{ c.createdAt ? c.createdAt.replace('T', ' ').slice(0, 16) : '—' }}</span>
            </div>
          </div>
          <span class="row-arrow">›</span>
        </button>
      </div>
    </template>

    <!-- ============ 申请表单视图 ============ -->
    <template v-else-if="view === 'form'">
      <div class="form-head">
        <button class="btn-ghost btn sm" @click="backToList">← 返回列表</button>
        <h3>新建多学科会诊</h3>
      </div>

      <div class="card form">
        <label class="field">
          <span>患者就诊 UID <i>*</i></span>
          <input v-model="form.patientVisitUid" placeholder="如 PV-2025-000123" />
        </label>
        <label class="field">
          <span>患者 ID <i>*</i></span>
          <input v-model="form.patientId" placeholder="如 P-10086" />
        </label>
        <label class="field">
          <span>检查号（Acc. No.）</span>
          <input v-model="form.accessionNumber" placeholder="选填，关联影像序列" />
        </label>
        <label class="field">
          <span>会诊标题 <i>*</i></span>
          <input v-model="form.title" placeholder="如 肺部占位 MDT 会诊" />
        </label>
        <label class="field">
          <span>申请人</span>
          <input v-model="form.applicant" placeholder="默认当前登录用户" />
        </label>
        <label class="field span2">
          <span>会诊事由 / 临床摘要</span>
          <textarea v-model="form.reason" rows="3" placeholder="描述病情、既往史与待决策问题"></textarea>
        </label>
        <div class="field span2">
          <span>邀请专家 <i>*</i>（全员确认后方可开始）</span>
          <div class="exp-pick">
            <label v-for="e in MOCK_EXPERTS" :key="e.id" class="exp-opt" :class="{ on: form.expertIds.includes(e.id) }">
              <input type="checkbox" :checked="form.expertIds.includes(e.id)" @change="toggleExpert(e.id)" />
              <span>{{ e.name }}</span>
            </label>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button class="btn-ghost btn" @click="backToList">取消</button>
        <button class="btn primary" :disabled="!canApply || loading" @click="submitApply">
          {{ loading ? '提交中…' : '提交申请' }}
        </button>
      </div>
    </template>

    <!-- ============ 详情 / 操作视图 ============ -->
    <template v-else-if="selected">
      <div class="form-head">
        <button class="btn-ghost btn sm" @click="backToList">← 返回列表</button>
        <h3>{{ selected.title }}</h3>
        <span class="badge" :class="STATUS_META[selected.status]?.cls">{{ statusLabel(selected.status) }}</span>
      </div>

      <div class="card meta-grid">
        <div><span class="k">患者就诊 UID</span><b class="mono">{{ selected.patientVisitUid }}</b></div>
        <div><span class="k">患者 ID</span><b class="mono">{{ selected.patientId }}</b></div>
        <div><span class="k">检查号</span><b class="mono">{{ selected.accessionNumber || '—' }}</b></div>
        <div><span class="k">申请人</span><b>{{ selected.applicant }}</b></div>
        <div><span class="k">创建时间</span><b>{{ selected.createdAt ? selected.createdAt.replace('T', ' ').slice(0, 16) : '—' }}</b></div>
      </div>

      <div class="card">
        <h4>会诊事由</h4>
        <p class="reason">{{ selected.reason || '—' }}</p>
      </div>

      <!-- 专家确认门控 -->
      <div class="card">
        <div class="card-head">
          <h4>专家确认</h4>
          <span class="count">{{ confirmedCount }} / {{ selected.experts.length }} 已确认</span>
        </div>
        <ul class="exp-list">
          <li v-for="e in selected.experts" :key="e.expertId" class="exp-item">
            <span class="exp-name">{{ e.expertName }}</span>
            <span v-if="e.confirmed" class="ok-tag">✓ 已确认</span>
            <button v-else-if="selected.status === 'NOTIFIED'" class="btn-ghost btn xs"
              :disabled="loading" @click="doConfirm(e.expertId)">确认出席</button>
            <span v-else class="pending-tag">待确认</span>
          </li>
        </ul>
        <p v-if="selected.status === 'NOTIFIED'" class="hint">
          需全体受邀专家确认后，会诊自动进入「已确认」状态（全员确认门控）。
        </p>
      </div>

      <!-- 总结（进行中可编辑；已完成只读） -->
      <div v-if="selected.status === 'IN_PROGRESS' || selected.status === 'COMPLETED'" class="card">
        <h4>{{ selected.status === 'COMPLETED' ? '会诊结论' : '会诊总结' }}</h4>
        <textarea v-if="selected.status === 'IN_PROGRESS'" v-model="summaryText" rows="4"
          placeholder="记录多方讨论结论、诊疗决策与下一步计划"></textarea>
        <p v-else class="reason">{{ selected.summaryText || '（无结论记录）' }}</p>
      </div>

      <!-- 操作栏：按状态显示可用动作 -->
      <div class="actions">
        <RouterLink v-if="selected.status !== 'CANCELLED'" :to="`/room/${selected.consultationId}`"
          class="btn primary">进入协同房间 →</RouterLink>
        <template v-if="selected.status === 'APPLIED'">
          <button class="btn primary" :disabled="loading" @click="doNotify">通知专家</button>
          <button class="btn-ghost btn" :disabled="loading" @click="doCancel">取消会诊</button>
        </template>
        <template v-else-if="selected.status === 'NOTIFIED'">
          <button class="btn-ghost btn" :disabled="loading" @click="doCancel">取消会诊</button>
        </template>
        <template v-else-if="selected.status === 'CONFIRMED'">
          <button class="btn primary" :disabled="loading" @click="doStart">开始会诊</button>
          <button class="btn-ghost btn" :disabled="loading" @click="doCancel">取消会诊</button>
        </template>
        <template v-else-if="selected.status === 'IN_PROGRESS'">
          <button class="btn primary" :disabled="loading || !summaryText.trim()" @click="doComplete">完成会诊</button>
        </template>
        <p v-else-if="selected.status === 'CANCELLED'" class="hint muted">该会诊已取消。</p>
        <p v-else class="hint muted">该会诊已完成。</p>
      </div>
    </template>
  </AppShell>
</template>

<style scoped>
.alert { background: #fff2f0; border: 1px solid #ffccc7; color: #cf1322;
  padding: .6rem .8rem; border-radius: var(--r-md); font-size: .85rem; }
.empty { color: var(--md-muted); font-size: .9rem; padding: var(--sp-4) 0; }

.toolbar { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3); flex-wrap: wrap; }
.filters { display: flex; gap: .4rem; flex-wrap: wrap; }
.chip.filt { border: 1px solid var(--md-line); background: var(--md-surface); color: var(--md-muted);
  font-size: .76rem; padding: .3rem .7rem; border-radius: 999px; cursor: pointer; transition: all .15s var(--ease); }
.chip.filt:hover { border-color: var(--md-blue-400); color: var(--md-blue-700); }
.chip.filt.on { background: var(--md-blue-700); border-color: var(--md-blue-700); color: #fff; }

.cards { display: grid; gap: var(--sp-3); }
.row-card { display: flex; align-items: center; gap: var(--sp-3); width: 100%; text-align: left;
  background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md);
  padding: var(--sp-3) var(--sp-4); cursor: pointer; transition: border-color .18s var(--ease), box-shadow .18s var(--ease); }
.row-card:hover { border-color: var(--md-blue-400); box-shadow: var(--shadow-1); }
.row-main { flex: 1; min-width: 0; }
.row-top { display: flex; align-items: center; gap: .6rem; }
.row-title { font-size: .98rem; color: var(--md-ink); }
.row-meta { display: flex; gap: .4rem; align-items: center; color: var(--md-muted); font-size: .8rem; margin-top: .25rem; flex-wrap: wrap; }
.row-meta .dot { opacity: .5; }
.row-arrow { color: var(--md-line); font-size: 1.4rem; }

.badge { font-size: .72rem; font-weight: 700; padding: .2rem .6rem; border-radius: 999px; white-space: nowrap; }
.st-applied    { background: #eef2ff; color: #3b5bdb; }
.st-notified   { background: #fff4e6; color: #d9480f; }
.st-confirmed  { background: #e6fcf5; color: #0c8599; }
.st-progress   { background: #edf2ff; color: #1c7ed6; }
.st-completed  { background: #ebfbee; color: #2f9e44; }
.st-cancelled  { background: #f1f3f5; color: #868e96; }

.form-head { display: flex; align-items: center; gap: .8rem; }
.form-head h3 { margin: 0; font-size: 1.15rem; }

.card { background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md); padding: var(--sp-4); }
.card h4 { margin: 0 0 .6rem; font-size: .92rem; color: var(--md-ink); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: .6rem; }
.card-head h4 { margin: 0; }
.count { font-size: .8rem; color: var(--md-muted); }

.form { display: grid; grid-template-columns: 1fr 1fr; gap: var(--sp-3) var(--sp-4); }
.field { display: flex; flex-direction: column; gap: .3rem; font-size: .82rem; color: var(--md-muted); }
.field.span2 { grid-column: 1 / -1; }
.field i { color: #e03131; font-style: normal; }
.field input, .field textarea, textarea {
  border: 1px solid var(--md-line); border-radius: var(--r-sm); padding: .5rem .6rem;
  font-size: .9rem; color: var(--md-ink); background: var(--md-bg); font-family: inherit;
}
.field input:focus, textarea:focus { outline: none; border-color: var(--md-blue-400); }

.exp-pick { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: .5rem; margin-top: .1rem; }
.exp-opt { display: flex; align-items: center; gap: .5rem; border: 1px solid var(--md-line);
  border-radius: var(--r-sm); padding: .45rem .6rem; font-size: .85rem; cursor: pointer; color: var(--md-ink); }
.exp-opt.on { border-color: var(--md-blue-400); background: #f1f6ff; }
.exp-opt input { accent-color: var(--md-blue-600); }

.form-actions { display: flex; justify-content: flex-end; gap: .6rem; }

.meta-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--sp-3); }
.meta-grid .k { display: block; font-size: .74rem; color: var(--md-muted); margin-bottom: .2rem; }
.meta-grid b { font-size: .9rem; color: var(--md-ink); font-weight: 600; }

.reason { color: var(--md-ink); font-size: .88rem; line-height: 1.6; white-space: pre-wrap; }

.exp-list { list-style: none; margin: 0; padding: 0; display: grid; gap: .5rem; }
.exp-item { display: flex; align-items: center; justify-content: space-between; gap: .6rem;
  border: 1px solid var(--md-line); border-radius: var(--r-sm); padding: .5rem .7rem; }
.exp-name { font-size: .88rem; color: var(--md-ink); }
.ok-tag { font-size: .76rem; color: #2f9e44; font-weight: 600; }
.pending-tag { font-size: .76rem; color: var(--md-muted); }
.hint { font-size: .8rem; color: var(--md-muted); margin-top: .6rem; }
.hint.muted { color: var(--md-muted); }

.actions { display: flex; gap: .6rem; flex-wrap: wrap; align-items: center; }
.actions .hint { margin: 0; }

.btn { border: none; border-radius: var(--r-sm); padding: .55rem 1.1rem; font-size: .88rem; cursor: pointer;
  font-family: inherit; transition: filter .15s var(--ease), opacity .15s var(--ease); }
.btn.primary { background: var(--md-blue-700); color: #fff; }
.btn.primary:hover { filter: brightness(1.08); }
.btn.primary:disabled { opacity: .5; cursor: not-allowed; }
.btn-ghost { background: transparent; border: 1px solid var(--md-line); color: var(--md-ink); }
.btn-ghost:hover { border-color: var(--md-blue-400); color: var(--md-blue-700); }
.btn-ghost:disabled { opacity: .5; cursor: not-allowed; }
.btn.sm { padding: .35rem .7rem; font-size: .8rem; }
.btn.xs { padding: .25rem .6rem; font-size: .75rem; }
.mono { font-family: ui-monospace, monospace; }
</style>
