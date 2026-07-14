/**
 * ② 影像核心引擎 — WebGL2 体渲染器（自研，不依赖 Cornerstone3D）。
 * <p>
 * 功能：
 * - 将 Int16 LE 体素上传为 R32F 3D 纹理
 * - MPR 三正交平面（轴位/矢状/冠状）
 * - GPU 端窗宽窗位（WL 映射），不阻塞主线程
 * - 缩放 / 平移
 * <p>
 * 每个 Canvas 实例一张纹理 + 一组 uniform，三正交平面共享 VolumeData
 * 但各实例独立 GL Context（~18MB 显存，128³ 级别带宽可控）。
 *
 * @module VolumeRenderer
 */

export type MprAxis = 0 | 1 | 2 // 0=轴位(Axial XY), 1=矢状(Sagittal YZ), 2=冠状(Coronal XZ)

/** 前端从响应头拿到的体数据元数据 */
export interface VolumeMeta {
  dims: [number, number, number]
  spacing: [number, number, number]
  modality: string
  ww: number
  wl: number
}

/** 一次完整加载 = Int16 原始字节 + 元数据 */
export interface VolumeData extends VolumeMeta {
  voxels: ArrayBuffer
}

// ====== GLSL Shaders ======

/** 全屏四边形 + UV 变换（缩放/平移作用于纹理空间） */
const VERTEX_SRC = `#version 300 es
in vec2 a_position;
out vec2 v_texCoord;
uniform vec3 u_zoomPan;  // x=zoom, y=panX, z=panY

void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    vec2 uv = a_position * 0.5 + 0.5;          // [-1,1] → [0,1]
    v_texCoord = (uv - 0.5) / u_zoomPan.x + 0.5 + u_zoomPan.yz;
}`

/** 3 纹理采样 + MPR 三轴分支 + 窗宽窗位 */
const FRAGMENT_SRC = `#version 300 es
precision highp float;
precision highp sampler3D;

uniform sampler3D u_volume;
uniform float u_slicePos;   // 0..1 归一化切片位置
uniform vec2  u_wwl;        // x=窗宽, y=窗位
uniform int   u_axis;       // 0=轴位, 1=矢状, 2=冠状

in vec2 v_texCoord;
out vec4 fragColor;

void main() {
    vec2 tc = clamp(v_texCoord, 0.0, 1.0);

    // 三轴采样坐标
    vec3 coord;
    if (u_axis == 0)       // 轴位 XY
        coord = vec3( tc.x,      tc.y,      u_slicePos );
    else if (u_axis == 1)  // 矢状 YZ
        coord = vec3( u_slicePos, tc.x,      tc.y       );
    else                   // 冠状 XZ
        coord = vec3( tc.x,      u_slicePos, tc.y       );

    float val = texture(u_volume, coord).r;

    // 窗宽窗位映射
    float lo = u_wwl.y - u_wwl.x * 0.5;
    float hi = u_wwl.y + u_wwl.x * 0.5;
    float intensity = clamp((val - lo) / (hi - lo), 0.0, 1.0);

    fragColor = vec4(vec3(intensity), 1.0);
}`

// ====== 渲染器核心 ======

export class VolumeRenderer {
  private gl: WebGL2RenderingContext | null = null
  private program: WebGLProgram | null = null
  private vao: WebGLVertexArrayObject | null = null
  private tex: WebGLTexture | null = null

  // 当前状态
  private slicePos = 0.5
  private ww = 400
  private wl = 40
  private zoom = 1.0
  private panX = 0
  private panY = 0

  // uniform 位置缓存
  private loc!: {
    u_volume: WebGLUniformLocation | null
    u_slicePos: WebGLUniformLocation | null
    u_wwl: WebGLUniformLocation | null
    u_zoomPan: WebGLUniformLocation | null
    u_axis: WebGLUniformLocation | null
  }

  private initialized = false

  /**
   * @param canvas 目标 Canvas 元素
   * @param axis   正交平面轴
   */
  constructor(
    private readonly canvas: HTMLCanvasElement,
    private readonly axis: MprAxis,
  ) {}

  // ---- 初始化 ----

  /** 初始化 WebGL2 上下文、着色器、几何体 */
  init(): void {
    const gl = this.canvas.getContext('webgl2', {
      alpha: false, antialias: false, premultipliedAlpha: false,
      // 保留绘制缓冲，便于截图 / 导出快照 / 像素回读诊断
      preserveDrawingBuffer: true,
    })
    if (!gl) throw new Error('WebGL2 不可用，请使用 Chrome 90+ / Firefox / Safari')
    this.gl = gl

    const vs = this.compile(gl, gl.VERTEX_SHADER, VERTEX_SRC)
    const fs = this.compile(gl, gl.FRAGMENT_SHADER, FRAGMENT_SRC)
    this.program = gl.createProgram()!
    gl.attachShader(this.program, vs)
    gl.attachShader(this.program, fs)
    gl.linkProgram(this.program)
    if (!gl.getProgramParameter(this.program, gl.LINK_STATUS)) {
      throw new Error('着色器链接失败: ' + gl.getProgramInfoLog(this.program))
    }

    this.loc = {
      u_volume:   gl.getUniformLocation(this.program, 'u_volume'),
      u_slicePos: gl.getUniformLocation(this.program, 'u_slicePos'),
      u_wwl:      gl.getUniformLocation(this.program, 'u_wwl'),
      u_zoomPan:  gl.getUniformLocation(this.program, 'u_zoomPan'),
      u_axis:     gl.getUniformLocation(this.program, 'u_axis'),
    }

    // 全屏四边形（两个三角形，6 顶点）
    const verts = new Float32Array([-1, -1,  1, -1,  -1, 1,  -1, 1,  1, -1,  1, 1])
    this.vao = gl.createVertexArray()!
    gl.bindVertexArray(this.vao)
    const buf = gl.createBuffer()!
    gl.bindBuffer(gl.ARRAY_BUFFER, buf)
    gl.bufferData(gl.ARRAY_BUFFER, verts, gl.STATIC_DRAW)
    gl.enableVertexAttribArray(0)
    gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0)
    gl.bindVertexArray(null)

    this.initialized = true
    this.resize()
  }

  // ---- 数据加载 ----

  /**
   * 加载体数据（Int16 LE 原始字节 → R32F 3D 纹理）。
   * 同一 ImageView 的三个渲染器实例调用相同数据各自上传（显存成本 ~18MB）。
   */
  loadVolume(voxels: ArrayBuffer, dims: [number, number, number], ww: number, wl: number): void {
    const gl = this.gl
    if (!gl) this.init()
    const g = this.gl!
    this.ww = ww
    this.wl = wl

    // Int16 → Float32（WebGL2 不支持原生 16 位有符号纹理线性过滤）
    const src = new Int16Array(voxels)
    const floats = new Float32Array(src.length)
    for (let i = 0; i < src.length; i++) floats[i] = src[i]

    // 3D 纹理
    if (this.tex) g.deleteTexture(this.tex)
    this.tex = g.createTexture()!
    g.bindTexture(g.TEXTURE_3D, this.tex)
    g.texStorage3D(g.TEXTURE_3D, 1, g.R32F, dims[0], dims[1], dims[2])
    g.texSubImage3D(g.TEXTURE_3D, 0, 0, 0, 0, dims[0], dims[1], dims[2],
      g.RED, g.FLOAT, floats)

    // ⚠️ 关键：R32F 在 WebGL2 核心规范中【不可线性过滤】。
    // 若直接设置 LINEAR 而缺少 OES_texture_float_linear 扩展，纹理会被判为“不完整”，
    // 采样恒返回 0（整片灰）。故优先启用扩展，缺失则回退 NEAREST（值正确、无插值）。
    const floatLinear = g.getExtension('OES_texture_float_linear')
    const filter = floatLinear ? g.LINEAR : g.NEAREST
    g.texParameteri(g.TEXTURE_3D, g.TEXTURE_MIN_FILTER, filter)
    g.texParameteri(g.TEXTURE_3D, g.TEXTURE_MAG_FILTER, filter)
    g.texParameteri(g.TEXTURE_3D, g.TEXTURE_WRAP_S, g.CLAMP_TO_EDGE)
    g.texParameteri(g.TEXTURE_3D, g.TEXTURE_WRAP_T, g.CLAMP_TO_EDGE)
    g.texParameteri(g.TEXTURE_3D, g.TEXTURE_WRAP_R, g.CLAMP_TO_EDGE)

    this.render()
  }

  // ---- 渲染 ----

  /** 有变化时调用（setter 均自动触发 render） */
  /** 渲染：全屏四边形 + 3D 纹理采样 + 三轴分支 + 窗宽窗位 */
  render(): void {
    const gl = this.gl
    if (!gl || !this.initialized) return

    // 高 DPI 适配
    const dpr = window.devicePixelRatio || 1
    const cw = this.canvas.clientWidth * dpr
    const ch = this.canvas.clientHeight * dpr
    if (this.canvas.width !== cw || this.canvas.height !== ch) {
      this.canvas.width = cw
      this.canvas.height = ch
    }

    gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight)
    gl.clearColor(0, 0, 0, 1)
    gl.clear(gl.COLOR_BUFFER_BIT)

    gl.useProgram(this.program)
    gl.bindVertexArray(this.vao)

    gl.activeTexture(gl.TEXTURE0)
    gl.bindTexture(gl.TEXTURE_3D, this.tex)
    gl.uniform1i(this.loc.u_volume, 0)

    gl.uniform1f(this.loc.u_slicePos, this.slicePos)
    gl.uniform2f(this.loc.u_wwl, this.ww, this.wl)
    gl.uniform3f(this.loc.u_zoomPan, this.zoom, this.panX, this.panY)
    gl.uniform1i(this.loc.u_axis, this.axis)

    gl.drawArrays(gl.TRIANGLES, 0, 6)
  }

  // ---- 交互控制 ----

  setSlice(pos: number)        { this.slicePos = Math.max(0, Math.min(1, pos)); this.render() }
  setWindowLevel(ww: number, wl: number) { this.ww = ww; this.wl = wl; this.render() }
  setZoom(z: number)           { this.zoom = Math.max(0.1, Math.min(20, z)); this.render() }
  setPan(x: number, y: number) { this.panX = x; this.panY = y; this.render() }

  /** 窗口大小变化时调（CSS 可 resize 的容器需要） */
  resize(): void { this.render() }

  /** 释放 GPU 资源 */
  destroy(): void {
    const gl = this.gl
    if (!gl) return
    if (this.tex) gl.deleteTexture(this.tex)
    if (this.program) gl.deleteProgram(this.program)
    if (this.vao) gl.deleteVertexArray(this.vao)
    this.gl = null
  }

  // ---- 内部工具 ----

  private compile(gl: WebGL2RenderingContext, type: number, source: string): WebGLShader {
    const shader = gl.createShader(type)!
    gl.shaderSource(shader, source)
    gl.compileShader(shader)
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      const tag = type === gl.VERTEX_SHADER ? 'VERTEX' : 'FRAGMENT'
      throw new Error(`${tag} 编译失败: ${gl.getShaderInfoLog(shader)}`)
    }
    return shader
  }
}
