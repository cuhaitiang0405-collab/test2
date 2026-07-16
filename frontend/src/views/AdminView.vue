<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '../components/AppShell.vue'
import { auth } from '../store/auth'
import { api } from '../api'

// GAP-10 机构管理
const tenants = ref<any[]>([])
const tenantForm = ref({ tenantId: '', tenantName: '', region: '', type: '综合' })
async function loadTenants() { try { tenants.value = await api.listTenants() } catch(e) { /* 忽略 */ } }
onMounted(loadTenants)
async function createTenant() {
  if (!tenantForm.value.tenantId || !tenantForm.value.tenantName) return
  await api.createTenant(tenantForm.value)
  tenantForm.value = { tenantId: '', tenantName: '', region: '', type: '综合' }
  await loadTenants()
}
async function toggleTenantStatus(t: any) {
  await api.updateTenant(t.tenantId, { status: t.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' })
  await loadTenants()
}
</script>

<template>
  <AppShell title="系统管理" crumb="数据源适配 · 租户配置 · 权限管理">
    <div class="card" v-if="auth.state.role !== 'ADMIN'">
      <p class="deny">您没有系统管理权限，请联系管理员。</p>
    </div>

    <template v-else>
      <div class="card">
        <h3>数据源适配</h3>
        <p class="desc">对接 HIS / EMR / PACS 异构系统，统一就诊唯一标识（PatientVisitUID）索引。</p>
        <div class="grid2">
          <div class="src-card"><strong>DICOM SCP</strong><small>PACS 影像设备推送接入</small><span class="chip ok">已实现（M2）</span></div>
          <div class="src-card"><strong>DICOM SCU</strong><small>主动查询拉取 PACS 检查</small><span class="chip ok">已实现（M2）</span></div>
          <div class="src-card"><strong>HIS 适配器</strong><small>Mock 中间表 / 视图聚合临床数据</small><span class="chip ok">已实现（M2）</span></div>
          <div class="src-card"><strong>LIS 检验</strong><small>检验结果 Mock（血常规/生化/肿瘤标志物）</small><span class="chip ok">已实现（M2）</span></div>
        </div>
      </div>

      <!-- GAP-10 医疗单位管理 -->
      <div class="card">
        <h3>医疗单位（租户）管理</h3>
        <p class="desc">管理接入系统的医疗机构：新建、启用/停用。当前仅 ADMIN 可操作；机构按 tenantId 行级隔离。</p>
        <div style="display:flex;gap:.5rem;margin-bottom:.6rem;flex-wrap:wrap">
          <input v-model="tenantForm.tenantId" placeholder="机构ID" style="width:100px" />
          <input v-model="tenantForm.tenantName" placeholder="机构名称" style="width:140px" />
          <input v-model="tenantForm.region" placeholder="区域" style="width:80px" />
          <button class="btn primary xs" @click="createTenant" :disabled="!tenantForm.tenantId || !tenantForm.tenantName">新建</button>
        </div>
        <p v-if="!tenants.length" class="hint">暂无机构记录。</p>
        <table v-else><thead><tr><th>ID</th><th>名称</th><th>区域</th><th>类型</th><th>状态</th><th></th></tr></thead>
        <tbody>
          <tr v-for="t in tenants" :key="t.tenantId">
            <td class="mono">{{ t.tenantId }}</td><td>{{ t.tenantName }}</td><td>{{ t.region }}</td><td>{{ t.type }}</td>
            <td><span :class="t.status==='ACTIVE'?'chip ok':'chip warn'">{{ t.status }}</span></td>
            <td><button class="btn-ghost btn xs" @click="toggleTenantStatus(t)">{{ t.status==='ACTIVE' ? '停用' : '启用' }}</button></td>
          </tr>
        </tbody></table>
      </div>

      <details class="card">
        <summary><h3>技术架构速览</h3></summary>
        <div class="grid2" style="margin-top:.6rem">
          <div><strong>API 网关</strong><small>Spring Cloud Gateway，统一路由 + TraceId 透传</small></div>
          <div><strong>gRPC 后端</strong><small>六域微服务，域内 gRPC，对外 REST</small></div>
          <div><strong>影像引擎</strong><small>WebGL2 体积渲染，GPU 侧窗宽窗位</small></div>
          <div><strong>协同通讯</strong><small>WebRTC Mesh + 白板标注同步</small></div>
          <div><strong>扩展点</strong><small>SPI 模式云影像 / 5 种负载均衡策略</small></div>
        </div>
      </details>
    </template>
  </AppShell>
</template>

<style scoped>
.card { background: var(--md-surface); border: 1px solid var(--md-line); border-radius: var(--r-md); padding: var(--sp-4); margin-bottom: var(--sp-4); }
.card h3 { margin: 0 0 .4rem; font-size: .95rem; }
.desc { color: var(--md-muted); font-size: .82rem; margin-bottom: var(--sp-3); }
.deny { color: var(--md-muted); text-align: center; padding: 2rem 0; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: var(--sp-3); }
.src-card { border: 1px solid var(--md-line); border-radius: var(--r-sm); padding: var(--sp-3); }
.src-card strong { display: block; font-size: .88rem; }
.src-card small { display: block; color: var(--md-muted); font-size: .75rem; margin: .2rem 0 .4rem; }
.chip { font-size: .68rem; padding: .15rem .5rem; border-radius: 999px; white-space: nowrap; }
.chip.ok { background: #e6fcf5; color: #2f9e44; }
.chip.warn { background: #f1f3f5; color: #868e96; }
.hint { font-size: .8rem; color: var(--md-muted); }
table { width: 100%; border-collapse: collapse; font-size: .82rem; }
th, td { text-align: left; padding: .4rem .5rem; border-bottom: 1px solid var(--md-line); }
th { color: var(--md-muted); font-weight: 600; }
.mono { font-family: ui-monospace, monospace; font-size: .78rem; }
.btn { border: none; border-radius: var(--r-sm); padding: .35rem .7rem; font-size: .8rem; cursor: pointer; }
.btn.primary { background: var(--md-blue-700); color: #fff; }
.btn.primary:disabled { opacity: .4; cursor: not-allowed; }
.btn-ghost { background: transparent; border: 1px solid var(--md-line); }
.btn.xs { padding: .2rem .5rem; font-size: .72rem; }
details summary { cursor: pointer; }
</style>
