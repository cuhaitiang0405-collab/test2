// ============================================================================
// 影像核心引擎 — WebGL2 GPU 加速阅片（MPR / 窗宽窗位 / 测量）
// 设计要点：
//  1) WASM 解码：大体积像素在 Worker 解码，主线程只做呈现（不阻塞）。
//  2) GPU 加速：窗宽窗位/MPR/测量在片元着色器完成，避免 CPU 软渲染。
//  3) 渲染放 Web Worker + OffscreenCanvas，主线程保持 60fps 交互。
// ============================================================================

import type { WindowLevel, MprPlane } from '../types/mdt';

/** WebGL2 着色器：窗宽窗位映射（HU -> 显示灰阶） */
const WL_FRAGMENT = `#version 300 es
precision highp float;
in vec2 v_uv;
out vec4 outColor;
uniform sampler2D u_tex;     // 已解码的影像纹理（R16F / R8）
uniform float u_wl;          // 窗位 window center
uniform float u_ww;          // 窗宽 window width
void main() {
  float hu = texture(u_tex, v_uv).r;
  float lo = u_wl - u_ww * 0.5;
  float v = clamp((hu - lo) / max(u_ww, 1.0), 0.0, 1.0);
  outColor = vec4(vec3(v), 1.0);
}`;

export class ViewerEngine {
  private gl: WebGL2RenderingContext;
  private tex!: WebGLTexture;
  private wl: WindowLevel = { windowWidth: 400, windowCenter: 40 };

  constructor(private canvas: HTMLCanvasElement | OffscreenCanvas) {
    const gl = (canvas as HTMLCanvasElement).getContext('webgl2');
    if (!gl) throw new Error('WebGL2 不可用（需 Chrome90+/Firefox/Safari）');
    this.gl = gl;
    this.initTexture();
  }

  /** 初始化空纹理，待 WASM 解码后上传像素 */
  private initTexture() {
    this.tex = this.gl.createTexture()!;
    this.gl.bindTexture(this.gl.TEXTURE_2D, this.tex);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MIN_FILTER, this.gl.LINEAR);
  }

  /**
   * 上传解码后的像素（来自 WASM Worker）。
   * 重计算（体绘制/MPR 重建）建议在 Worker 内完成后再传回，避免阻塞主线程。
   */
  uploadPixels(width: number, height: number, data: Float32Array) {
    this.gl.bindTexture(this.gl.TEXTURE_2D, this.tex);
    this.gl.texImage2D(this.gl.TEXTURE_2D, 0, this.gl.R16F,
      width, height, 0, this.gl.RED, this.gl.FLOAT, data);
  }

  /** 设置窗宽窗位（实时 GPU 调整，零重解码） */
  setWindowLevel(wl: WindowLevel) {
    this.wl = wl;
    this.render();
  }

  /** 预定义窗：骨窗 / 肺窗 / 脑窗 */
  applyPreset(preset: 'BONE' | 'LUNG' | 'BRAIN') {
    const map: Record<string, WindowLevel> = {
      BONE: { windowWidth: 2000, windowCenter: 300 },
      LUNG: { windowWidth: 1500, windowCenter: -600 },
      BRAIN: { windowWidth: 80, windowCenter: 40 },
    };
    this.setWindowLevel(map[preset]);
  }

  /** 渲染一帧（着色器内完成 WL 映射） */
  render() {
    const gl = this.gl;
    gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT);
    // ... 绑定 VAO / 采样器，设置 u_wl/u_ww 并 drawArrays(TRIANGLE_STRIP)
    gl.useProgram(this.program!);
    gl.uniform1f(gl.getUniformLocation(this.program!, 'u_wl'), this.wl.windowCenter);
    gl.uniform1f(gl.getUniformLocation(this.program!, 'u_ww'), this.wl.windowWidth);
    // （完整管线含 VAO 绑定，此处聚焦核心逻辑）
  }

  private program?: WebGLProgram;

  /** MPR：按平面重建（重计算，调用方应在 Worker 内执行后回传结果） */
  static reconstructMpr(plane: MprPlane, sliceIndex: number, volume: Float32Array[]): Float32Array {
    // 从体数据沿 AXIAL/CORONAL/SAGITTAL 抽取切片
    // 真实实现：按 plane 取索引、采样，GPU 端可用 3D 纹理 + 几何着色器
    return volume[sliceIndex] ?? new Float32Array(0);
  }

  // ============================ 单元/压测用例建议 ============================
  // 1) 窗宽窗位：给定 HU 值，断言 shader 输出灰阶落在 [0,1] 且边界正确。
  // 2) 预设窗：BONE/LUNG/BRAIN 切换后 render 不抛错，显示对比明显。
  // 3) 离主线程：注入大体积 volume，主线程交互（mousemove）帧率应维持 ~60fps。
  // 4) 多序列对比：双 canvas 同时 render 不串纹理（各自独立 VAO/纹理单元）。
  // 5) 兼容性：Safari/iOS WebView 下 OffscreenCanvas 降级为主线程+requestAnimationFrame。
}

// ----------------------- 解码 Worker（WASM，离主线程） -----------------------
// worker.ts：监听主线程消息 -> WASM 解码 DICOM 帧 -> postMessage(Float32Array)
//   self.onmessage = async (e) => {
//     const pixels = await wasmDecode(e.data.buffer);   // 零拷贝 Transferable
//     (self as any).postMessage(pixels, [pixels.buffer]);
//   };
