<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { auth } from '../store/auth'
import { api, type StudyRecord } from '../api'
import AppShell from '../components/AppShell.vue'

// —— 医生工作台：今日概览 + 待办 + 快捷入口 ——
const recentPatients = ref<StudyRecord[]>([])
const stats = ref({ total: 0, today: 0, ct: 0, mri: 0 })

onMounted(async () => {
  try {
    const r = await api.listStudies()
    recentPatients.value = r.data.slice(0, 6)
    const all = r.data
    stats.value.total = all.length
    stats.value.ct = all.filter(s => s.modality === 'CT').length
    stats.value.mri = all.filter(s => s.modality === 'MRI').length
  } catch { /* 静默降级 */ }
})

// 快捷操作
const quickActions = [
  { label: '查看患者', to: '/patients', desc: '检索患者、浏览检查记录' },
  { label: '新建会诊', to: '/consultations', desc: '发起多学科会诊申请' },
  { label: '影像阅片', to: '/patients', desc: '从患者记录进入影像中心' },
]
</script>

<template>
  <AppShell title="工作台" crumb="今日概览 · 待办事项 · 快捷入口">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">检查总数</div>
      </div>
      <div class="stat-card accent">
        <div class="stat-num">{{ stats.ct }}</div>
        <div class="stat-label">CT 检查</div>
      </div>
      <div class="stat-card accent2">
        <div class="stat-num">{{ stats.mri }}</div>
        <div class="stat-label">MRI 检查</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">—</div>
        <div class="stat-label">待办会诊</div>
        <small class="hint">M4 上线后启用</small>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="card">
      <h3>快捷操作</h3>
      <div class="quick-row">
        <RouterLink v-for="act in quickActions" :key="act.label"
          :to="act.to" class="quick-item">
          <strong>{{ act.label }}</strong>
          <small>{{ act.desc }}</small>
        </RouterLink>
      </div>
    </div>

    <!-- 最近患者 -->
    <div class="card">
      <div class="card-head">
        <h3>最近检查记录</h3>
        <RouterLink to="/patients" class="btn-ghost btn sm">查看全部</RouterLink>
      </div>
      <div class="table-wrap" v-if="recentPatients.length">
        <table>
          <thead>
            <tr><th>患者 ID</th><th>检查号</th><th>模态</th><th>检查日期</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="s in recentPatients" :key="s.patientVisitUid + s.modality">
              <td class="mono">{{ s.patientId }}</td>
              <td class="mono">{{ s.accessionNumber }}</td>
              <td><span class="mod-tag">{{ s.modality }}</span></td>
              <td>{{ s.studyDate || '—' }}</td>
              <td>
                <RouterLink
                  :to="`/imaging?studyUid=${s.studyInstanceUid}&patientId=${s.patientId}&accessionNumber=${s.accessionNumber}&patientVisitUid=${s.patientVisitUid}&modality=${s.modality}`"
                  class="btn-ghost btn xs">阅片</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="empty">暂无检查记录，请先在「患者管理」中导入数据。</p>
    </div>

    <!-- 当前用户信息 -->
    <div class="card muted">
      <p>登录身份：<strong>{{ auth.state.username }}</strong> · 机构 {{ auth.state.tenantId }} · 角色 {{ auth.state.role }}</p>
    </div>
  </AppShell>
</template>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: var(--sp-3); }
.stat-card {
  background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md);
  padding: var(--sp-4); text-align: center;
}
.stat-card.accent { border-left: 3px solid var(--md-blue-500); }
.stat-card.accent2 { border-left: 3px solid oklch(60% 0.15 270); }
.stat-num { font-size: 2rem; font-weight: 700; color: var(--md-ink); }
.stat-label { font-size: .82rem; color: var(--md-muted); margin-top: .2rem; }
.hint { display: block; color: var(--md-muted); font-size: .72rem; margin-top: .25rem; }

.quick-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--sp-3);
  margin-top: var(--sp-3); }
.quick-item {
  display: flex; flex-direction: column; gap: .2rem; padding: var(--sp-3); border-radius: var(--r-md);
  border: 1px solid var(--md-line); color: inherit; text-decoration: none;
  transition: border-color .18s var(--ease), box-shadow .18s var(--ease);
}
.quick-item:hover { border-color: var(--md-blue-400); box-shadow: var(--shadow-1); }
.quick-item strong { font-size: .95rem; color: var(--md-blue-700); }
.quick-item small { font-size: .78rem; color: var(--md-muted); }

.card-head { display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3); }
.card-head h3 { margin: 0; }

.table-wrap { margin-top: var(--sp-3); overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: .85rem; }
th, td { text-align: left; padding: .6rem .7rem; border-bottom: 1px solid var(--md-line); white-space: nowrap; }
th { color: var(--md-muted); font-weight: 600; font-size: .76rem; }
td.mono { font-family: ui-monospace, monospace; font-size: .78rem; }
.mod-tag { font-size: .72rem; font-weight: 700; padding: .15rem .55rem; border-radius: 999px;
  background: var(--md-bg); color: var(--md-blue-700); }

.empty { color: var(--md-muted); font-size: .85rem; padding: var(--sp-3) 0 0; }
.card.muted { background: var(--md-bg); }
.card.muted p { color: var(--md-muted); font-size: .82rem; margin: 0; }
.card.muted strong { color: var(--md-ink); }
</style>
