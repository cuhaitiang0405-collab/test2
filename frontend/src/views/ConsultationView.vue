<script setup lang="ts">
import AppShell from '../components/AppShell.vue'
</script>

<template>
  <AppShell title="会诊中心" crumb="多学科协同会诊 · M4 里程碑规划中">
    <div class="card placeholder">
      <div class="ph-icon">📋</div>
      <h3>会诊中心</h3>
      <p>多学科会诊（MDT）全流程管理：申请 → 通知 → 确认 → 进行中 → 总结</p>
      <div class="flow">
        <span class="step done">申请</span>
        <span class="arrow">→</span>
        <span class="step">通知</span>
        <span class="arrow">→</span>
        <span class="step">确认</span>
        <span class="arrow">→</span>
        <span class="step">进行中</span>
        <span class="arrow">→</span>
        <span class="step">总结</span>
      </div>
      <p class="hint">本模块将在 <strong>M4 里程碑（业务流程层）</strong> 中实现，包含状态机引擎、短信异步通知队列与会诊报告生成。</p>
      <p class="hint">协同通讯（WebRTC 电子白板）将在 M5 里程碑中接入。</p>
    </div>
  </AppShell>
</template>

<style scoped>
.placeholder { text-align: center; padding: var(--sp-6) var(--sp-4); max-width: 560px; margin: 0 auto; }
.ph-icon { font-size: 3rem; margin-bottom: var(--sp-3); }
.placeholder h3 { font-size: 1.3rem; margin: .5rem 0; }
.placeholder p { color: var(--md-muted); font-size: .9rem; line-height: 1.6; max-width: 48ch; margin: .4rem auto; }
.flow { display: flex; align-items: center; justify-content: center; gap: .4rem; margin: var(--sp-4) 0; flex-wrap: wrap; }
.step {
  display: inline-block; padding: .35rem .8rem; border-radius: 999px;
  font-size: .8rem; font-weight: 600; background: var(--md-bg); color: var(--md-muted);
}
.step.done { background: oklch(92% 0.06 165); color: var(--md-ok); }
.arrow { color: var(--md-line); font-size: .85rem; }
.hint { font-size: .8rem !important; margin-top: var(--sp-4) !important; }
.hint strong { color: var(--md-blue-700); }
</style>
