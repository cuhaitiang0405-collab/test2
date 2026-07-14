<script setup lang="ts">
import AppShell from '../components/AppShell.vue'
import { auth } from '../store/auth'
</script>

<template>
  <AppShell title="系统管理" crumb="数据源适配 · 租户配置 · 权限管理">
    <!-- 仅管理员可见 -->
    <div class="card" v-if="auth.state.role !== 'ADMIN'">
      <p class="deny">您没有系统管理权限，请联系管理员。</p>
    </div>

    <template v-else>
      <!-- 数据源管理 -->
      <div class="card">
        <h3>数据源适配</h3>
        <p class="desc">对接 HIS / EMR / PACS 异构系统，统一就诊唯一标识（PatientVisitUID）索引。</p>
        <div class="grid2">
          <div class="src-card">
            <strong>DICOM SCP</strong>
            <small>PACS 影像设备推送接入</small>
            <span class="chip ok">已实现（M2）</span>
          </div>
          <div class="src-card">
            <strong>DICOM SCU</strong>
            <small>主动查询拉取 PACS 检查</small>
            <span class="chip ok">已实现（M2）</span>
          </div>
          <div class="src-card">
            <strong>HIS 适配器</strong>
            <small>患者基本信息、挂号就诊</small>
            <span class="chip plan">规划中</span>
          </div>
          <div class="src-card">
            <strong>EMR 适配器</strong>
            <small>电子病历、检验报告</small>
            <span class="chip plan">规划中</span>
          </div>
        </div>
        <div class="mt">
          <RouterLink to="/patients" class="btn-ghost btn sm">前往患者管理验证数据接入 →</RouterLink>
        </div>
      </div>

      <!-- 租户与权限 -->
      <div class="card">
        <h3>租户与角色权限</h3>
        <p class="desc">多机构数据隔离 · 字段级 RBAC · 审计日志。当前示例：</p>
        <table class="info-table">
          <tr><th>当前租户</th><td>{{ auth.state.tenantId }}</td></tr>
          <tr><th>当前用户</th><td>{{ auth.state.username }}</td></tr>
          <tr><th>当前角色</th><td><span class="chip ok">{{ auth.state.role }}</span></td></tr>
          <tr><th>全链路 TraceId</th><td class="mono">{{ auth.state.traceId || '—' }}</td></tr>
        </table>
      </div>

      <!-- 技术底座信息（开发者可见，生产环境可隐藏） -->
      <details class="card tech">
        <summary>技术底座详情（开发期可见）</summary>
        <div class="tech-grid">
          <div><strong>六域微服务</strong><small>网关 + Auth/Image/Integration/Collab/Workflow/Ext 六模块，Spring Boot 3.2</small></div>
          <div><strong>内部通信</strong><small>gRPC（AuthService、VolumeService）</small></div>
          <div><strong>外部接口</strong><small>RESTful 经 API 网关（8080）统一暴露，TraceId 全链路透传</small></div>
          <div><strong>影像引擎</strong><small>WebGL2 体积渲染，GPU 侧窗宽窗位，NEAREST 采样（含 float_linear 回退）</small></div>
          <div><strong>协同通讯</strong><small>WebRTC SFU（M5 规划）</small></div>
          <div><strong>扩展点</strong><small>SPI 模式云影像 / 区域专家标注（M6 规划）</small></div>
        </div>
      </details>
    </template>
  </AppShell>
</template>

<style scoped>
.desc { color: var(--md-muted); font-size: .85rem; margin: .3rem 0 var(--sp-3); }
.deny { color: #c0392b; font-size: .9rem; }

.grid2 { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: var(--sp-3); }
.src-card {
  padding: var(--sp-3); border: 1px solid var(--md-line); border-radius: var(--r-md);
  display: flex; flex-direction: column; gap: .25rem;
}
.src-card strong { font-size: .92rem; }
.src-card small { font-size: .78rem; color: var(--md-muted); }
.src-card .chip { align-self: flex-start; font-size: .68rem; margin-top: .3rem; }

.info-table { width: 100%; border-collapse: collapse; font-size: .85rem; margin-top: var(--sp-3); }
.info-table th, .info-table td { text-align: left; padding: .5rem .8rem; border-bottom: 1px solid var(--md-line); }
.info-table th { width: 140px; color: var(--md-muted); font-weight: 500; }
.mono { font-family: ui-monospace, monospace; font-size: .78rem; }
.mt { margin-top: var(--sp-3); }

.tech { margin-top: var(--sp-4); }
.tech summary { cursor: pointer; font-weight: 600; color: var(--md-blue-700); font-size: .9rem; padding: .3rem 0; }
.tech-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: var(--sp-3);
  margin-top: var(--sp-3); }
.tech-grid > div { display: flex; flex-direction: column; gap: .2rem;
  padding: var(--sp-2) var(--sp-3); border-left: 2px solid var(--md-line); font-size: .82rem; }
.tech-grid strong { color: var(--md-ink); font-size: .84rem; }
.tech-grid small { color: var(--md-muted); font-size: .76rem; }
</style>
