<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { auth } from '../store/auth'

const router = useRouter()
const username = ref('doctor')
const password = ref('doctor123')
const error = ref('')
const loading = ref(false)
const lastTrace = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const r: any = await api.login(username.value, password.value)
    auth.set({
      token: r.token, username: r.username, tenantId: r.tenantId, role: r.role,
      traceId: r.traceId || ''
    })
    lastTrace.value = r.traceId || ''
    router.push('/workbench')
  } catch (e: any) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <!-- 品牌侧：深医疗蓝，无霓虹/发光 -->
    <aside class="brand">
      <div class="logo">
        <svg viewBox="0 0 48 48" width="40" height="40" aria-hidden="true">
          <rect x="2" y="2" width="44" height="44" rx="12" fill="var(--md-blue-400)"/>
          <path d="M24 13v22M13 24h22" stroke="#fff" stroke-width="4" stroke-linecap="round"/>
          <path d="M6 30h7l3-6 4 10 3-14 3 10h9" fill="none" stroke="#fff" stroke-width="2.4"
                stroke-linecap="round" stroke-linejoin="round" opacity=".85"/>
        </svg>
        <div>
          <strong>MDT</strong>
          <small>多学科会诊中心</small>
        </div>
      </div>
      <h1>融合 PACS 与临床数据的<br/>多学科会诊平台</h1>
      <p>零下载 Web 阅片 · 多方协同 · 全链路合规审计</p>
      <ul class="notes">
        <li>统一就诊标识 PatientVisitUID</li>
        <li>字段级 RBAC · 多租户隔离</li>
        <li>全链路 TraceId 追踪</li>
      </ul>
    </aside>

    <!-- 表单侧 -->
    <main class="formwrap">
      <form class="card form" @submit.prevent="submit">
        <h2>登录</h2>
        <p class="sub">使用机构账号进入工作台</p>

        <label class="field">
          <span>用户名</span>
          <input class="input" v-model="username" autocomplete="username" placeholder="doctor"/>
        </label>
        <label class="field">
          <span>密码</span>
          <input class="input" type="password" v-model="password" autocomplete="current-password" placeholder="••••••••"/>
        </label>

        <p v-if="error" class="err">{{ error }}</p>
        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '进入工作台' }}
        </button>

        <p v-if="lastTrace" class="trace">本次会话 TraceId：<code>{{ lastTrace }}</code></p>
        <p class="hint">演示账户：doctor / doctor123</p>
      </form>
    </main>
  </div>
</template>

<style scoped>
.login { min-height: 100%; display: grid; grid-template-columns: 1.05fr 1fr; }
@media (max-width: 860px) { .login { grid-template-columns: 1fr; } .brand { display: none; } }

.brand {
  background: linear-gradient(160deg, var(--md-blue-900), var(--md-blue-700));
  color: #eaf2ff; padding: var(--sp-7) var(--sp-6);
  display: flex; flex-direction: column; justify-content: center; gap: var(--sp-4);
}
.logo { display: flex; align-items: center; gap: .7rem; }
.logo strong { font-size: 1.3rem; letter-spacing: .04em; display: block; }
.logo small { color: #b9d2ff; font-size: .78rem; }
.brand h1 { font-size: var(--fs-display); font-weight: 700; }
.brand p { color: #c5d8ff; margin: 0; }
.notes { list-style: none; padding: 0; margin: var(--sp-3) 0 0; display: grid; gap: .6rem; }
.notes li { position: relative; padding-left: 1.3rem; color: #dce8ff; font-size: .92rem; }
.notes li::before {
  content: ""; position: absolute; left: 0; top: .55em; width: .5rem; height: .5rem;
  border-radius: 2px; background: var(--md-blue-400); transform: rotate(45deg);
}

.formwrap { display: flex; align-items: center; justify-content: center; padding: var(--sp-5); }
.form { width: min(380px, 100%); }
.form h2 { font-size: var(--fs-h2); }
.sub { color: var(--md-muted); margin: .3rem 0 var(--sp-4); }
.err { color: #c0392b; font-size: .88rem; margin: 0 0 var(--sp-2); }
.trace { font-size: .76rem; color: var(--md-muted); margin-top: var(--sp-3); word-break: break-all; }
.trace code { color: var(--md-blue-700); }
.hint { font-size: .8rem; color: var(--md-muted); margin-top: var(--sp-3); }
</style>
