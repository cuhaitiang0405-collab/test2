/**
 * M5 协同信令 WebSocket 客户端。
 * 同源连接 ws(s)://location.host/ws/collab（dev 期经 vite 代理升级到网关 8080 → collab 8085）。
 * 信令仅转发 SDP/ICE/标注增量；媒体走浏览器间 Mesh。
 */
export type SignalHandler = (msg: any) => void

export interface SignalOpts {
  token: string
  user: string
  room: string
  tenant?: string
  patientVisitUid?: string
}

export class CollabSignal {
  private ws: WebSocket | null = null
  private url: string
  private handlers: Record<string, SignalHandler[]> = {}

  constructor(opts: SignalOpts) {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    const q = new URLSearchParams({
      token: opts.token,
      user: opts.user,
      room: opts.room,
      tenant: opts.tenant || 'T001',
      patientVisitUid: opts.patientVisitUid || ''
    })
    // 同源：dev 走 vite 代理(/ws)，生产走网关(/ws)
    this.url = `${proto}://${location.host}/ws/collab?${q.toString()}`
  }

  connect() {
    this.ws = new WebSocket(this.url)
    this.ws.onopen = () => this.emit('__open', {})
    this.ws.onclose = () => this.emit('__close', {})
    this.ws.onmessage = (e) => {
      try {
        const m = JSON.parse(e.data)
        this.emit(m.type, m)
      } catch { /* ignore */ }
    }
  }

  on(type: string, cb: SignalHandler) {
    ;(this.handlers[type] ||= []).push(cb)
    return this
  }

  private emit(type: string, m: any) {
    ;(this.handlers[type] || []).forEach(cb => cb(m))
  }

  send(m: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) this.ws.send(JSON.stringify(m))
  }

  close() {
    this.ws?.close()
    this.ws = null
  }
}
