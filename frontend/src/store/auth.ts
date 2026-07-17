import { reactive, computed } from 'vue'

interface Account { username: string; token: string; role: string; tenantId: string }

const state = reactive({
  token: localStorage.getItem('mdt_token') || '',
  username: localStorage.getItem('mdt_user') || '',
  tenantId: localStorage.getItem('mdt_tenant') || '',
  role: localStorage.getItem('mdt_role') || '',
  traceId: localStorage.getItem('mdt_trace') || '',
  accounts: JSON.parse(localStorage.getItem('mdt_accounts') || '[]') as Account[]
})

function persist() {
  localStorage.setItem('mdt_token', state.token)
  localStorage.setItem('mdt_user', state.username)
  localStorage.setItem('mdt_tenant', state.tenantId)
  localStorage.setItem('mdt_role', state.role)
  localStorage.setItem('mdt_trace', state.traceId)
  localStorage.setItem('mdt_accounts', JSON.stringify(state.accounts))
}

export const auth = {
  state,
  isAuthed: computed(() => !!state.token),
  set(d: { token: string; username: string; tenantId: string; role: string; traceId: string }) {
    Object.assign(state, d)
    // 去重保存到账号列表
    const exists = state.accounts.find(a => a.username === d.username)
    if (!exists) state.accounts.push({ username: d.username, token: d.token, role: d.role, tenantId: d.tenantId })
    else { exists.token = d.token; exists.role = d.role }
    persist()
  },
  switchTo(index: number) {
    const a = state.accounts[index]
    if (!a) return
    Object.assign(state, { token: a.token, username: a.username, tenantId: a.tenantId, role: a.role, traceId: '' })
    persist()
  },
  removeAccount(index: number) {
    state.accounts.splice(index, 1)
    persist()
  },
  clear() {
    state.token = state.username = state.tenantId = state.role = state.traceId = ''
    localStorage.clear()
    state.accounts.length = 0
  }
}
