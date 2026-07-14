<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { api, type StudyRecord, type ClinicalSummary } from '../api'
import AppShell from '../components/AppShell.vue'

const studies = ref<StudyRecord[]>([])
const loading = ref(false)
const ingest = ref<{ status: 'idle' | 'run' | 'ok' | 'fail'; mode?: string; msg: string; traceId?: string }>(
  { status: 'idle', msg: '' }
)

// 临床数据面板（可选）
const clinicalUid = ref('')
const clinical = ref<{ status: 'idle' | 'run' | 'ok' | 'fail'; traceId?: string; data?: ClinicalSummary; error?: string }>(
  { status: 'idle' }
)
const clinicalPretty = computed(() => {
  if (!clinical.value.data) return ''
  try { return JSON.stringify(JSON.parse(clinical.value.data.emrJson), null, 2) }
  catch { return clinical.value.data.emrJson }
})

async function loadStudies() {
  loading.value = true
  try {
    const r = await api.listStudies()
    studies.value = r.data
    clinicalUid.value = r.data[0]?.patientVisitUid || ''
  } catch (e: any) {
    studies.value = []
  } finally {
    loading.value = false
  }
}

async function runSimulate(mode: 'scu' | 'scp') {
  ingest.value = { status: 'run', mode, msg: '接入中…' }
  try {
    const r = await api.simulateIngestion(mode)
    const d = r.data
    ingest.value = {
      status: 'ok', mode, traceId: r.traceId,
      msg: `接入成功 · ${d.patientVisitUid || ''}${d.modality ? ' · 模态 ' + d.modality : ''}` +
           `${d.instanceCount != null ? ' · ' + d.instanceCount + ' 序列' : ''}`
    }
    await loadStudies()
  } catch (e: any) {
    ingest.value = { status: 'fail', mode, msg: e.message }
  }
}

async function runClinical() {
  if (!clinicalUid.value) return
  clinical.value = { status: 'run' }
  try {
    const r = await api.fetchClinical(clinicalUid.value)
    clinical.value = { status: 'ok', traceId: r.traceId, data: r.data }
  } catch (e: any) {
    clinical.value = { status: 'fail', error: e.message }
  }
}

const modalityClass: Record<string, string> = {
  CT: 'ct', MRI: 'mri', US: 'us', PATH: 'path', ECG: 'ecg', ENDOSCOPY: 'endo'
}

onMounted(loadStudies)
</script>

<template>
  <AppShell title="患者管理" crumb="患者检索 · 检查记录 · 临床数据">

    <!-- 操作栏：SCU / SCP 双模拟 -->
    <div class="card ops">
      <div class="ops-head">
        <div>
          <h3>模拟接入</h3>
          <p class="hint">研发期以内存态 Mock PACS + DicomSimulator 触发，落 STUDY_INDEX；生产期替换为真实适配器即可。</p>
        </div>
        <div class="btns">
          <button class="btn scu" :disabled="ingest.status === 'run'" @click="runSimulate('scu')">
            {{ ingest.status === 'run' && ingest.mode === 'scu' ? '拉取中…' : 'SCU 拉取（P1001）' }}
          </button>
          <button class="btn scp" :disabled="ingest.status === 'run'" @click="runSimulate('scp')">
            {{ ingest.status === 'run' && ingest.mode === 'scp' ? '推送中…' : 'SCP 推送（P1002）' }}
          </button>
        </div>
      </div>
      <p v-if="ingest.status !== 'idle'" class="result" :class="ingest.status">
        {{ ingest.msg }}
        <span v-if="ingest.traceId" class="tid">TraceId {{ ingest.traceId.slice(0, 12) }}…</span>
      </p>
    </div>

    <!-- 接入记录列表（统一索引，脱敏） -->
    <div class="card">
      <div class="ops-head">
        <h3>接入记录（统一索引 · 脱敏）</h3>
        <button class="btn-ghost btn sm" :disabled="loading" @click="loadStudies">
          {{ loading ? '刷新中…' : '刷新' }}
        </button>
      </div>
      <div class="table-wrap">
        <table v-if="studies.length">
          <thead>
            <tr>
              <th>就诊 UID</th><th>患者</th><th>检查号</th><th>模态</th><th>检查日期</th><th>报告</th><th>病理</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in studies" :key="s.patientVisitUid + s.modality">
              <td class="mono">{{ s.patientVisitUid }}</td>
              <td class="mono">{{ s.patientId }}</td>
              <td class="mono">{{ s.accessionNumber }}</td>
              <td><span class="mod" :class="modalityClass[s.modality]">{{ s.modality }}</span></td>
              <td>{{ s.studyDate || '—' }}</td>
              <td>{{ s.reportId ? '有' : '—' }}</td>
              <td>{{ s.hasPathology ? '有' : '—' }}</td>
              <td>
                <router-link
                  :to="`/imaging?studyUid=${s.studyInstanceUid}&patientId=${s.patientId}&accessionNumber=${s.accessionNumber}&patientVisitUid=${s.patientVisitUid}&modality=${s.modality}`"
                  class="btn-ghost btn xs">阅片</router-link>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty">暂无接入记录，点击上方按钮模拟一次接入。</p>
      </div>
    </div>

    <!-- 可选临床数据面板 -->
    <div class="card">
      <div class="ops-head">
        <h3>临床数据（脱敏 HIS / EMR / LIS）</h3>
        <div class="btns">
          <select v-model="clinicalUid" class="input sel">
            <option v-for="s in studies" :key="s.patientVisitUid" :value="s.patientVisitUid">
              {{ s.patientVisitUid }}
            </option>
          </select>
          <button class="btn" :disabled="!clinicalUid || clinical.status === 'run'" @click="runClinical">
            {{ clinical.status === 'run' ? '拉取中…' : '获取' }}
          </button>
        </div>
      </div>
      <p v-if="clinical.status === 'fail'" class="result fail">{{ clinical.error }}</p>
      <pre v-if="clinical.status === 'ok' && clinical.data" class="json">{{ clinicalPretty }}</pre>
      <p v-else-if="clinical.status === 'idle'" class="empty">选择一个就诊 UID，拉取聚合后的脱敏临床数据（不含姓名 / 身份证）。</p>
      <p v-if="clinical.status === 'ok' && clinical.traceId" class="tid">TraceId {{ clinical.traceId.slice(0, 12) }}…</p>
    </div>
  </AppShell>
</template>

<style scoped>
.hint { color: var(--md-muted); font-size: .8rem; margin: .25rem 0 0; max-width: 60ch; }
.ops-head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--sp-3); }
.btns { display: flex; align-items: center; gap: .6rem; flex: none; }
.btn.sm { padding: .5rem .9rem; }
.scu { background: var(--md-blue-600); }
.scp { background: var(--md-blue-700); }
.btn:disabled { opacity: .6; cursor: not-allowed; }

.result { margin: var(--sp-3) 0 0; font-size: .82rem; padding: .7rem .9rem; border-radius: var(--r-md);
  background: var(--md-bg); word-break: break-all; }
.result.ok { color: var(--md-ok); }
.result.fail { color: #c0392b; }
.tid { display: inline-block; margin-left: .6rem; font-family: ui-monospace, monospace; font-size: .72rem;
  color: var(--md-muted); background: var(--md-blue-50); padding: .1rem .5rem; border-radius: 999px; }

.table-wrap { margin-top: var(--sp-3); overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: .85rem; }
th, td { text-align: left; padding: .6rem .7rem; border-bottom: 1px solid var(--md-line); white-space: nowrap; }
th { color: var(--md-muted); font-weight: 600; font-size: .76rem; }
td.mono { font-family: ui-monospace, monospace; font-size: .78rem; color: var(--md-ink); }
.mod { display: inline-block; font-size: .72rem; font-weight: 700; padding: .15rem .55rem; border-radius: 999px; }
.mod.ct { background: oklch(94% 0.06 250); color: var(--md-blue-700); }
.mod.mri { background: oklch(94% 0.06 270); color: oklch(46% 0.13 270); }
.mod.us { background: oklch(94% 0.07 150); color: var(--md-ok); }
.mod.path { background: oklch(95% 0.06 30); color: oklch(55% 0.16 30); }
.mod.ecg { background: oklch(95% 0.08 75); color: oklch(55% 0.14 75); }
.mod.endo { background: oklch(94% 0.05 200); color: oklch(48% 0.1 200); }

.empty { color: var(--md-muted); font-size: .85rem; margin: var(--sp-3) 0 0; }
.sel { width: auto; min-width: 220px; padding: .55rem .7rem; }
.json { margin: var(--sp-3) 0 0; background: var(--md-bg); border: 1px solid var(--md-line); border-radius: var(--r-md);
  padding: var(--sp-3); font-size: .78rem; line-height: 1.5; max-height: 360px; overflow: auto; color: var(--md-ink); }
</style>
