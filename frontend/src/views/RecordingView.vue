<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '../components/AppShell.vue'

const recordings = ref<any[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true; error.value = ''
  try { recordings.value = await (await fetch('/api/collab/recordings')).json() }
  catch (e: any) { error.value = e.message || '加载失败' }
  finally { loading.value = false }
}
onMounted(load)

function formatDuration(s: number) {
  const m = Math.floor(s / 60), sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}
function formatTs(ts: number) { return new Date(ts).toLocaleString() }
const statusCls = (s: string) => s === 'COMPLETED' ? 'chip ok' : 'chip warn'
</script>

<template>
  <AppShell title="会诊录制" crumb="录制管理与回放 · GAP-2">
    <h3>录制列表</h3>
    <div v-if="error" class="alert">{{ error }}</div>
    <p v-if="loading" class="hint">加载中…</p>
    <p v-else-if="!recordings.length" class="hint">暂无录制记录。进入协同房间后点击「开始录制」即可保存。</p>

    <div v-else class="cards">
      <div v-for="r in recordings" :key="r.recordingId" class="card">
        <div class="card-head">
          <span :class="statusCls(r.status)">{{ r.status === 'COMPLETED' ? '已完成' : '录制中' }}</span>
          <span class="mono small">{{ r.recordingId }}</span>
        </div>
        <div class="meta">
          <span>会诊 {{ r.consultationId?.slice(0,12) }}…</span>
          <span>时长 {{ formatDuration(r.duration) }}</span>
          <span>{{ r.startedAt ? formatTs(r.startedAt) : '—' }}</span>
        </div>
        <div class="acts">
          <button v-if="r.status === 'COMPLETED' && r.objectKey" class="btn primary xs">▶ 回放</button>
          <span v-else class="hint">{{ r.status === 'RECORDING' ? '录制中…' : '待上传' }}</span>
        </div>
      </div>
    </div>
    <p class="hint">提示：录制由前端 MediaRecorder 采集后上传，本页仅展示录制元数据。当前为 Mock 环境。</p>
  </AppShell>
</template>

<style scoped>
.cards { display: grid; gap: var(--sp-3); }
.card { background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md); padding: var(--sp-3); display: flex; align-items: center; gap: var(--sp-3); }
.card-head { display: flex; align-items: center; gap: .4rem; }
.meta { flex: 1; display: flex; gap: var(--sp-2); font-size: .8rem; color: var(--md-muted); }
.acts { display: flex; gap: .3rem; }
.chip { font-size: .68rem; padding: .15rem .5rem; border-radius: 999px; white-space: nowrap; }
.chip.ok { background: #e6fcf5; color: #2f9e44; }
.chip.warn { background: #fff4e6; color: #d9480f; }
.mono { font-family: ui-monospace, monospace; } .small { font-size: .72rem; }
.hint { color: var(--md-muted); font-size: .82rem; margin-top: .5rem; }
.alert { background: #fff2f0; border: 1px solid #ffccc7; color: #cf1322; padding: .5rem .7rem; border-radius: var(--r-md); font-size: .82rem; }
.btn { border: none; border-radius: var(--r-sm); padding: .55rem 1.1rem; font-size: .88rem; cursor: pointer; }
.btn.primary { background: var(--md-blue-700); color: #fff; } .btn.xs { padding: .25rem .6rem; font-size: .75rem; }
</style>
