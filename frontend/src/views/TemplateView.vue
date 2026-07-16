<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '../components/AppShell.vue'

const templates = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const showForm = ref(false)
const editing = ref<any>(null)
const form = ref({ name: '', type: 'DISCUSSION' as string })
const types = [
  { key: 'IMAGING', label: '检查模板' },
  { key: 'DISCUSSION', label: '讨论模板' },
  { key: 'EMR', label: '病历模板' },
]

async function load() {
  loading.value = true; error.value = ''
  try { templates.value = await (await fetch('/api/workflow/templates')).json() }
  catch (e: any) { error.value = e.message || '加载失败' }
  finally { loading.value = false }
}
onMounted(load)

function openNew() {
  editing.value = null; form.value = { name: '', type: 'DISCUSSION' }; showForm.value = true
}
function openEdit(t: any) {
  editing.value = t; form.value = { name: t.name, type: t.type }; showForm.value = true
}
async function save() {
  const body: any = { name: form.value.name, type: form.value.type, sections: editing.value?.sections || '[]' }
  if (editing.value) {
    await fetch(`/api/workflow/templates/${editing.value.templateId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  } else {
    await fetch('/api/workflow/templates', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  }
  showForm.value = false; await load()
}
async function remove(id: string) {
  if (!confirm('确认删除此模板？')) return
  await fetch(`/api/workflow/templates/${id}`, { method: 'DELETE' })
  await load()
}
function cancel() { showForm.value = false }
</script>

<template>
  <AppShell title="模板管理" crumb="检查 / 讨论 / 病历模板 · GAP-1">
    <div class="toolbar">
      <h3>模板列表</h3>
      <button class="btn primary" @click="openNew">＋ 新建模板</button>
    </div>

    <div v-if="error" class="alert">{{ error }}</div>
    <p v-if="loading" class="hint">加载中…</p>
    <p v-else-if="!templates.length" class="hint">暂无模板，点击「新建模板」创建第一套。</p>

    <div v-else class="cards">
      <div v-for="t in templates" :key="t.templateId" class="card">
        <div class="card-head">
          <span class="chip" :class="t.type">🞑 {{ types.find(x=>x.key===t.type)?.label || t.type }}</span>
          <span class="mono small">{{ t.templateId }}</span>
        </div>
        <strong>{{ t.name }}</strong>
        <div class="acts">
          <button class="btn-ghost btn xs" @click="openEdit(t)">编辑</button>
          <button class="btn-ghost btn xs danger" @click="remove(t.templateId)">删除</button>
        </div>
      </div>
    </div>

    <!-- 编辑/新建弹窗 -->
    <div class="overlay" v-if="showForm" @click.self="cancel">
      <div class="modal">
        <h4>{{ editing ? '编辑模板' : '新建模板' }}</h4>
        <label class="field"><span>模板名称</span><input v-model="form.name" placeholder="如：CT 腹部检查模板" /></label>
        <label class="field"><span>模板类型</span>
          <select v-model="form.type">
            <option v-for="ty in types" :key="ty.key" :value="ty.key">{{ ty.label }}</option>
          </select>
        </label>
        <p class="hint">模板字段可在编辑器中拖拽定制（本版为结构化 JSON，生产接拖拽编辑器）。</p>
        <div class="acts"><button class="btn-ghost btn" @click="cancel">取消</button><button class="btn primary" @click="save" :disabled="!form.name">保存</button></div>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3); }
.cards { display: grid; gap: var(--sp-3); }
.card { background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md); padding: var(--sp-3); display: flex; align-items: center; gap: var(--sp-3); }
.card-head { display: flex; align-items: center; gap: .4rem; }
.card strong { flex: 1; font-size: .92rem; }
.acts { display: flex; gap: .3rem; }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: grid; place-items: center; z-index: 100; }
.modal { background: var(--md-surface); border-radius: var(--r-lg); padding: var(--sp-5); width: min(460px, 90vw); display: grid; gap: var(--sp-3); }
.field { display: flex; flex-direction: column; gap: .3rem; font-size:.82rem; color:var(--md-muted) }
.field input, .field select { border:1px solid var(--md-line); border-radius:var(--r-sm); padding:.5rem .6rem; font-size:.9rem }
.chip { font-size:.68rem; padding:.15rem .5rem; border-radius:999px; white-space:nowrap; }
.chip.IMAGING { background:#eef2ff; color:#3b5bdb } .chip.DISCUSSION { background:#fff4e6; color:#d9480f } .chip.EMR { background:#e6fcf5; color:#0c8599 }
.mono { font-family:ui-monospace,monospace } .small { font-size:.72rem; color:var(--md-muted) }
.hint { color: var(--md-muted); font-size: .82rem; }
.alert { background:#fff2f0; border:1px solid #ffccc7; color:#cf1322; padding:.5rem .7rem; border-radius:var(--r-md); font-size:.82rem }
.btn { border:none; border-radius:var(--r-sm); padding:.55rem 1.1rem; font-size:.88rem; cursor:pointer }
.btn.primary { background:var(--md-blue-700); color:#fff } .btn.primary:disabled { opacity:.4; cursor:not-allowed }
.btn-ghost { background:transparent; border:1px solid var(--md-line) } .btn.xs { padding:.25rem .6rem; font-size:.75rem }
.btn.danger { color:#c0392b; border-color:#ffccc7 } .btn.danger:hover { background:#fff2f0 }
</style>
