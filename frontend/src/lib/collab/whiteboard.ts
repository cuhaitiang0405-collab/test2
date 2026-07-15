/**
 * M5 协同白板：归一化坐标([0,1])绘制，与后端 AnnotationSerializer 对齐。
 * 支持 pen(自由笔)/rect(矩形)/arrow(箭头)/text(文字)；可整页回放（迟到加入/刷新恢复）。
 */
import type { AnnotationOp } from '../../api'

function uid(): string {
  return (crypto.randomUUID?.() || 'op-' + Math.random().toString(36).slice(2))
}

export class Whiteboard {
  private canvas: HTMLCanvasElement
  private ctx: CanvasRenderingContext2D
  ops: AnnotationOp[] = []
  current: AnnotationOp | null = null
  color = '#ff3b30'
  tool: AnnotationOp['type'] = 'pen'
  private dpr = Math.min(window.devicePixelRatio || 1, 2)

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas
    this.ctx = canvas.getContext('2d')!
    this.resize()
  }

  /** 适配高 DPI 并随容器尺寸变化重绘 */
  resize() {
    const r = this.canvas.getBoundingClientRect()
    this.canvas.width = Math.max(1, Math.round(r.width * this.dpr))
    this.canvas.height = Math.max(1, Math.round(r.height * this.dpr))
    this.redraw()
  }

  /** 屏幕坐标 → 归一化 [0,1] */
  norm(clientX: number, clientY: number): [number, number] {
    const r = this.canvas.getBoundingClientRect()
    return [
      Math.min(1, Math.max(0, (clientX - r.left) / r.width)),
      Math.min(1, Math.max(0, (clientY - r.top) / r.height))
    ]
  }

  /** 本地起一笔 */
  start(clientX: number, clientY: number, text?: string): AnnotationOp {
    const [x, y] = this.norm(clientX, clientY)
    this.current = {
      id: uid(), type: this.tool,
      points: text ? [[x, y]] : [[x, y]],
      color: this.color, author: 'me', t: Date.now()
    }
    if (this.tool === 'text' && text) (this.current as any).text = text
    return this.current
  }

  /** 本地追加点（自由笔用） */
  addPoint(clientX: number, clientY: number) {
    if (!this.current) return
    this.current.points.push(this.norm(clientX, clientY))
    this.redraw()
  }

  /** 本地结束一笔，返回完成的 op（供发送） */
  end(): AnnotationOp | null {
    const op = this.current
    if (!op) return null
    if (op.points.length === 0) { this.current = null; return null }
    this.ops.push(op)
    this.current = null
    this.redraw()
    return op
  }

  /** 远端一笔：直接追加并重绘 */
  addRemote(op: AnnotationOp) {
    this.ops.push(op)
    this.redraw()
  }

  /** 回放：用服务端标注列表重建整页 */
  replay(ops: AnnotationOp[]) {
    this.ops = ops
    this.redraw()
  }

  private redraw() {
    const { ctx, canvas } = this
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    for (const op of this.ops) this.drawOp(op)
    if (this.current) this.drawOp(this.current)
  }

  private drawOp(op: AnnotationOp) {
    const ctx = this.ctx
    const W = this.canvas.width, H = this.canvas.height
    const P = op.points.map(([x, y]) => [x * W, y * H] as [number, number])
    ctx.strokeStyle = op.color
    ctx.fillStyle = op.color
    ctx.lineWidth = 2 * this.dpr
    ctx.lineJoin = 'round'
    ctx.lineCap = 'round'

    if (op.type === 'pen' && P.length) {
      ctx.beginPath()
      P.forEach(([x, y], i) => i ? ctx.lineTo(x, y) : ctx.moveTo(x, y))
      ctx.stroke()
    } else if (op.type === 'rect' && P.length >= 2) {
      const [a, b] = [P[0], P[P.length - 1]]
      ctx.strokeRect(a[0], a[1], b[0] - a[0], b[1] - a[1])
    } else if (op.type === 'arrow' && P.length >= 2) {
      const [a, b] = [P[0], P[P.length - 1]]
      ctx.beginPath(); ctx.moveTo(a[0], a[1]); ctx.lineTo(b[0], b[1]); ctx.stroke()
      const ang = Math.atan2(b[1] - a[1], b[0] - a[0])
      const h = 10 * this.dpr
      ctx.beginPath()
      ctx.moveTo(b[0], b[1])
      ctx.lineTo(b[0] - h * Math.cos(ang - Math.PI / 6), b[1] - h * Math.sin(ang - Math.PI / 6))
      ctx.lineTo(b[0] - h * Math.cos(ang + Math.PI / 6), b[1] - h * Math.sin(ang + Math.PI / 6))
      ctx.closePath(); ctx.fill()
    } else if (op.type === 'text' && P.length) {
      const [x, y] = P[0]
      ctx.font = `${14 * this.dpr}px sans-serif`
      ctx.fillText((op as any).text || '', x, y + 12 * this.dpr)
    }
  }
}
