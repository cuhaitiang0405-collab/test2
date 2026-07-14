import { reactive, computed } from 'vue'

// 简易认证状态（M1：JWT 存内存 + localStorage；生产接医院统一认证）
const state = reactive({
  token: localStorage.getItem('mdt_token') || '',
  username: localStorage.getItem('mdt_user') || '',
  tenantId: localStorage.getItem('mdt_tenant') || '',
  role: localStorage.getItem('mdt_role') || '',
  traceId: localStorage.getItem('mdt_trace') || ''
})

function persist() {
  localStorage.setItem('mdt_token', state.token)
  localStorage.setItem('mdt_user', state.username)
  localStorage.setItem('mdt_tenant', state.tenantId)
  localStorage.setItem('mdt_role', state.role)
  localStorage.setItem('mdt_trace', state.traceId)
}

export const auth = {
  state,
  isAuthed: computed(() => !!state.token),
  set(d: { token: string; username: string; tenantId: string; role: string; traceId: string }) {
    Object.assign(state, d)
    persist()
  },
  clear() {
    state.token = state.username = state.tenantId = state.role = state.traceId = ''
    localStorage.clear()
  }
}
