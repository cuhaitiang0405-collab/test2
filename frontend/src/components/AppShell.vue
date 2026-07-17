<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { auth } from '../store/auth'

defineProps<{ title: string; crumb: string }>()

const router = useRouter()

// —— 医生视角导航（按临床工作流分级，隐藏技术实现细节） ——
interface NavItem {
  label: string; desc: string; to?: string; badge?: string
}
interface NavGroup { group: string; role?: string; items: NavItem[] }

const isAdmin = computed(() => auth.state.role === 'ADMIN')

const navGroups: NavGroup[] = [
  {
    group: '临床工作',
    items: [
      { label: '工作台',     desc: '待办事项与今日概览',           to: '/workbench' },
      { label: '患者管理',   desc: '患者检索、检查记录与临床数据', to: '/patients' },
      { label: '影像中心',   desc: 'MPR 多平面重建与序列对比',     to: '/imaging' },
      { label: '会诊中心',   desc: '多学科协同会诊',               to: '/consultations' },
    ]
  },
  {
    group: '系统管理',
    role: 'ADMIN',
    items: [
      { label: '数据与权限', desc: '数据源适配、租户与角色管理',   to: '/admin' },
      { label: '会诊录制',   desc: '会诊录制管理与回放',           to: '/recordings' },
      { label: '模板管理',   desc: '检查/讨论/病历模板定制',       to: '/templates' },
    ]
  }
]

// 根据角色过滤可见的导航组
const visibleGroups = computed(() =>
  navGroups.filter(g => !g.role || isAdmin.value)
)

function logout() { auth.clear(); router.push('/login') }
function switchAccount(e: Event) { const idx = parseInt((e.target as HTMLSelectElement).value); if (idx >= 0) { auth.switchTo(idx); window.location.reload() } }
</script>

<template>
  <div class="shell">
    <!-- 侧边导航（深医疗蓝，按临床工作流分级） -->
    <aside class="side">
      <div class="brand">
        <span class="mk">＋</span>
        <div class="brand-txt">
          <strong>多学科会诊</strong>
          <span class="brand-sub">MDT Center</span>
        </div>
      </div>

      <nav>
        <template v-for="grp in visibleGroups" :key="grp.group">
          <div class="nav-sep">{{ grp.group }}</div>
          <template v-for="item in grp.items" :key="item.label">
            <RouterLink v-if="item.to && !item.badge" :to="item.to"
              class="navitem" active-class="active">
              <span class="nw">
                <b>{{ item.label }}</b>
                <small>{{ item.desc }}</small>
              </span>
            </RouterLink>
            <span v-else class="navitem upcoming">
              <span class="nw">
                <b>{{ item.label }}</b>
                <small>{{ item.desc }}</small>
              </span>
              <span v-if="item.badge" class="chip plan">{{ item.badge }}</span>
            </span>
          </template>
        </template>
      </nav>

      <div class="sidefoot">多学科会诊中心系统 · v1.0</div>
    </aside>

    <!-- 主区 -->
    <section class="main">
      <header class="top">
        <div>
          <h2>{{ title }}</h2>
          <p class="crumb">{{ crumb }}</p>
        </div>
        <div class="me">
          <span class="chip ok">● {{ auth.state.role }}</span>
          <span class="who">{{ auth.state.username }} · {{ auth.state.tenantId }}</span>
          <select v-if="auth.state.accounts.length > 1" class="sel xs" @change="switchAccount" title="切换账号" style="max-width:100px;margin-right:4px">
            <option value="-1">切换账号</option>
            <option v-for="(a,i) in auth.state.accounts" :key="i" :value="i" :selected="a.username === auth.state.username">{{ a.username }}</option>
          </select>
          <button class="btn-ghost btn" @click="logout">退出</button>
        </div>
      </header>

      <div class="content">
        <slot />
      </div>
    </section>
  </div>
</template>

<style scoped>
.shell { min-height: 100%; display: grid; grid-template-columns: 260px 1fr; }
@media (max-width: 760px) { .shell { grid-template-columns: 1fr; } .side { display: none; } }

.side {
  background: linear-gradient(180deg, var(--md-blue-900), var(--md-blue-700));
  color: #dce8ff; padding: var(--sp-4) var(--sp-3); display: flex; flex-direction: column; gap: var(--sp-4);
}
.brand { display: flex; align-items: center; gap: .6rem; color: #fff; }
.brand .mk {
  width: 32px; height: 32px; border-radius: 8px; background: var(--md-blue-400);
  display: grid; place-items: center; font-weight: 800; color: #fff; flex: none;
}
.brand-txt { display: flex; flex-direction: column; line-height: 1.25; }
.brand-txt strong { font-size: 1.05rem; letter-spacing: .04em; }
.brand-sub { font-size: .66rem; color: #8fb0e6; letter-spacing: .08em; text-transform: uppercase; }

nav { display: grid; gap: .15rem; }
.nav-sep {
  font-size: .66rem; font-weight: 600; color: #7fa8e6; letter-spacing: .1em;
  padding: .8rem .7rem .35rem; text-transform: uppercase;
}
.nav-sep:first-child { padding-top: .2rem; }
.navitem {
  display: flex; align-items: center; gap: .65rem; padding: .65rem .7rem; border-radius: var(--r-md);
  color: #c5d8ff; text-decoration: none; transition: background .18s var(--ease), color .18s var(--ease);
}
.navitem:hover { background: rgba(255,255,255,.06); color: #fff; }
.navitem.active { background: rgba(255,255,255,.12); color: #fff; }
.navitem.upcoming { opacity: .8; cursor: default; }
.nw { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.nw b { font-size: .9rem; }
.nw small { color: #9bb8e8; font-size: .72rem; }
.navitem .chip { font-size: .64rem; }
.sidefoot { margin-top: auto; font-size: .72rem; color: #8fb0e6; }

.top {
  display: flex; align-items: center; justify-content: space-between; gap: var(--sp-3);
  padding: var(--sp-4) var(--sp-5); border-bottom: 1px solid var(--md-line); background: var(--md-surface);
}
.top h2 { font-size: var(--fs-h2); }
.crumb { color: var(--md-muted); font-size: .82rem; margin: .2rem 0 0; }
.me { display: flex; align-items: center; gap: .6rem; }
.who { font-size: .85rem; color: var(--md-ink); }

.content { padding: var(--sp-5); display: grid; gap: var(--sp-4); max-width: 1100px; }
</style>
