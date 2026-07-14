# M3 影像核心引擎 — 完工报告

> 状态：✅ 后端 + 前端 + 自测 + 预览 全部通过
> 里程碑：M3（影像核心引擎）端到端跑通，等待 M4 启动授权

---

## 1. 设计方案说明（200字内）

**自研轻量 WebGL2 体渲染，不依赖 Cornerstone3D**。前端通过 `WebGL2RenderingContext` + `sampler3D` 把后端下发的 Int16 LE 体数据上传为 R32F 3D 纹理，片段着色器按 MPR 三轴分支（轴位 XY / 矢状 YZ / 冠状 XZ）采样并 GPU 端完成窗宽窗位映射，三平面共享一个体数据但各持独立 GL Context（~18MB 显存）。后端用 **`VolumeSource` 适配器缝** 隔离数据来源——研发期由 `SyntheticVolumeSource` 按 `studyUid|modality` 种子确定性合成椭球体 + 骨壳 + 占位灶 + 同心壳纹理 + 噪声，生产期替换为 WADO-RS 拉帧 / dcm4che3 解码即可，零侵入。体数据以原始字节流流式下发，元数据（dims/spacing/ww/wl/modality）置于响应头，前端一次请求拉完全部构建状态。全链路 TraceId 沿 gateway → mdt-image REST → audit log 透传。

---

## 2. 核心接口定义

### 2.1 后端 — Java DDD 端口

```java
// port/VolumeSource.java — 数据源抽象（生产替换缝）
public interface VolumeSource {
    ImageVolume load(String studyUid, String modality);
}

// domain/ImageVolume.java — 领域值对象
public record ImageVolume(
    int[] dims,        // {x, y, z} 体素维度
    double[] spacing,  // {sx, sy, sz} mm
    String modality,   // CT | MRI
    byte[] voxels,     // Int16 LE, length = x*y*z*2
    int recommendedWw, // 推荐窗宽
    int recommendedWl  // 推荐窗位
) {}

// rest/VolumeController.java — REST 出站口
@GetMapping("/volume")
public ResponseEntity<byte[]> getVolume(
    @RequestParam String studyUid,
    @RequestParam(defaultValue = "CT") String modality) { ... }
```

### 2.2 REST 端点契约

| 项目 | 值 |
|------|-----|
| Method/Path | `GET /api/image/volume` |
| Query | `studyUid` (必填) · `modality` (CT/MRI, 默认 CT) |
| 响应头 | `X-Vol-Dims` · `X-Vol-Spacing` · `X-Vol-Modality` · `X-Vol-Ww` · `X-Vol-Wl` · `X-Mdt-TraceId` |
| Content-Type | `application/octet-stream` |
| Body | 原始 Int16 LE 字节流（128³=3,145,728 B） |
| 错误码 | 400 `{"error":"..."}` · 500 `{"error":"traceId=..."}` |

### 2.3 前端 — TypeScript 类

```typescript
// lib/webgl/VolumeRenderer.ts
export type MprAxis = 0 | 1 | 2  // 0=轴位, 1=矢状, 2=冠状
export class VolumeRenderer {
  constructor(canvas: HTMLCanvasElement, axis: MprAxis)
  init(): void
  loadVolume(voxels: ArrayBuffer, dims: [number, number, number], ww: number, wl: number): void
  setSlice(pos: number): void
  setWindowLevel(ww: number, wl: number): void
  setZoom(z: number): void
  setPan(x: number, y: number): void
  render(): void
  resize(): void
  destroy(): void
}
```

---

## 3. 关键代码片段

### 3.1 WebGL2 MPR 片段着色器（GPU 端窗宽窗位）

```glsl
#version 300 es
precision highp float;
precision highp sampler3D;

uniform sampler3D u_volume;
uniform float u_slicePos;   // 0..1 归一化切片
uniform vec2  u_wwl;        // x=窗宽, y=窗位
uniform int   u_axis;       // 0=轴位, 1=矢状, 2=冠状

in vec2 v_texCoord;
out vec4 fragColor;

void main() {
    vec3 coord;
    if (u_axis == 0)      coord = vec3( tc.x,      tc.y,      u_slicePos );
    else if (u_axis == 1) coord = vec3( u_slicePos, tc.x,      tc.y       );
    else                  coord = vec3( tc.x,      u_slicePos, tc.y       );

    float val = texture(u_volume, coord).r;

    float lo = u_wwl.y - u_wwl.x * 0.5;
    float hi = u_wwl.y + u_wwl.x * 0.5;
    float intensity = clamp((val - lo) / (hi - lo), 0.0, 1.0);

    fragColor = vec4(vec3(intensity), 1.0);
}
```

### 3.2 程序化合成体数据（确定性，可联调）

```java
// adapter/SyntheticVolumeSource.java — 同一 studyUid 必出同一段数据
@Override
public ImageVolume load(String studyUid, String modality) {
    long seed = hashSeed(studyUid + "|" + mod);
    // 椭球体表 (r<=1) + 骨壳 (r∈[0.86,0.95], val=900)
    //   + 偏心占位灶 (dl<1, val=130) + 同心壳纹理 + xorshift 噪声
    // CT: HU range (-1024..3071); MRI: 重映射至 200~800 亮度
    return new ImageVolume(dims, spacing, mod, voxels, ww, wl);
}
```

### 3.3 REST 出站 + 异常兜底

```java
@GetMapping("/volume")
public ResponseEntity<byte[]> getVolume(
        @RequestParam String studyUid,
        @RequestParam(defaultValue = "CT") String modality) {
    ImageVolume v = volumeService.getVolume(studyUid, modality);  // 校验+审计
    HttpHeaders h = new HttpHeaders();
    h.set("X-Vol-Dims",      v.dims()[0]+","+v.dims()[1]+","+v.dims()[2]);
    h.set("X-Vol-Spacing",   v.spacing()[0]+","+v.spacing()[1]+","+v.spacing()[2]);
    h.set("X-Vol-Modality",  v.modality());
    h.set("X-Vol-Ww",        String.valueOf(v.recommendedWw()));
    h.set("X-Vol-Wl",        String.valueOf(v.recommendedWl()));
    h.set(TraceContext.HEADER, Optional.ofNullable(TraceContext.get()).orElse(""));
    return ResponseEntity.ok().headers(h)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(v.voxels().length).body(v.voxels());
}
```

### 3.4 前端 ImagingView：3 画布 + 鼠标交互 + 预设

```typescript
// 三正交平面独立渲染器
renderers = [0, 1, 2].map((axis, i) => {
  const r = new VolumeRenderer(domCanvases[i], axis)
  r.init(); r.loadVolume(data.voxels, data.dims, data.ww, data.wl)
  return r
})

// 鼠标驱动：左键拖切片，右键拖窗位，Ctrl+滚轮缩放
onMousemove(e) {
  if (mouse.mode === 'slice') onSliceChange(axis, pos + dy * 0.004)
  else applyWl(ww + dx*2, wl + dy*2)  // WW/WL 同步三平面
}
```

---

## 4. 单元/压测用例建议

| 用例 | 关键断言 | 工具 |
|------|----------|------|
| **API 端点 200** | HTTP 200, `content-length == 3145728`, `X-Vol-Dims=128,128,96` | curl + JUnit |
| **缺 studyUid** | 400 + `{"error":"studyUid 必填"}`, **不**触达体素计算 | curl + MockMvc |
| **TraceId 透传** | 请求头 `X-Mdt-TraceId` 与响应头一致；`AUDIT_LOG` 表新增行携带同一 traceId | DB rowcheck |
| **审计合规** | `VOLUME_FETCH` action 写入；detail 经 `Desensitizer.mask`（无姓名/身份证） | DB rowcheck |
| **确定性** | 同一 `studyUid` 两次拉取 byte 完全一致（SHA256 相等） | sha256sum |
| **不同 modality** | `modality=CT` 200 / `=MRI` 200，recommended WW/WL 不同 | curl |
| **WebGL 初始化** | 3 canvas 创建成功，`getContext('webgl2')` 非空 | Playwright |
| **切片响应** | 设置 axial slider=0.3，shader uniform 更新；`drawArrays` 无 GL error | Playwright |
| **窗位交互** | 右键拖动改变 ww/wl，3 canvas 同步刷新 | Playwright |
| **显存上限** | 128³ 渲染器 ~6MB × 3 = 18MB，连续加载 100 次不增（destroy 干净） | K6 / Lighthouse |
| **响应体大小** | 128³: 3.1MB；256³: 12.6MB；512³: 50.3MB（gzip 后 0.6/2.5/10MB） | curl -w |

---

## 5. 部署注意事项

### 端口 / 网络

| 服务 | 端口 | 备注 |
|------|------|------|
| mdt-gateway | 8080 | 唯一对外入口 |
| mdt-image | 8083 | 经 `/api/image/**` 路由，不直连 |
| mdt-image gRPC | 50056 | 研发态占位，生产期暴露 VolumeService gRPC |

### 环境变量 / 配置

| 变量 | 默认 | 说明 |
|------|------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/mdt` | 审计落库用，**audit_log 表必存在** |
| `SERVER_PORT` | 8083 | REST 端口 |
| `GRPC_SERVER_PORT` | 50056 | gRPC 端口（研发期占位） |

### AE Title / 影像源

- **研发期**：使用 `SyntheticVolumeSource`，无需 PACS / DICOM 上游
- **生产期**：把 `SyntheticVolumeSource` 上的 `@Component` 移除，新增实现（如 `WadoRsVolumeSource`），`VolumeSource` 缝保留，业务零改动
- **DICOM TLS**：网关启用 HTTPS / DICOM TLS（独立于 M3 范围，由 M2 网关迭代）
- **CORS**：研发期 Vite dev 直连 8080 网关即可；生产期走同源

### 启动顺序

```bash
# 1. 数据库（已就绪：mdt-postgres 容器）
docker start mdt-postgres  # 包含 audit_log 表

# 2. 启动 mdt-image
cd /workspace/backend
nohup java -jar mdt-image/target/mdt-image-1.0.0.jar > /tmp/mdt-image.log 2>&1 &

# 3. 启动网关（含 /api/image/** 路由）
nohup java -jar mdt-gateway/target/mdt-gateway-1.0.0.jar > /tmp/gateway.log 2>&1 &

# 4. 启动前端
cd /workspace/frontend && nohup npx vite --host 0.0.0.0 --port 5173 > /tmp/vite.log 2>&1 &

# 5. 自测
curl -s -o /dev/null -w "%{http_code} %{size_download}\n" \
  "http://localhost:8080/api/image/volume?studyUid=PV-T001-P1001-AccCT001&modality=CT"
# 期望: 200 3145728
```

### WebGL2 客户端要求

- Chrome 90+ / Firefox / Safari（**Mobile Safari 15+** 也支持）
- 零下载、无 ActiveX/NPAPI
- 桌面端 WebGL2 GPU 加速；移动端 fallback 为 Canvas2D（可后续补）

---

## 6. M3 自测与预览

| 项 | 状态 | 备注 |
|----|------|------|
| 后端 mvn install | ✅ | `mdt-image-1.0.0.jar` 包含 VolumeController / Service / Adapter / Domain |
| 后端启动 | ✅ | 端口 8083, gRPC 50056, 3.7s 启动，JPA + audit_log 联通 |
| 端点自测 | ✅ | 200 + 3,145,728 bytes + 全部元数据响应头 + TraceId |
| 错误路径 | ✅ | 400 `{"error":"studyUid 必填"}` |
| 网关路由 | ✅ | `/api/image/** → http://localhost:8083` 已加 |
| 前端构建 | ✅ | vite build 598ms, 47 modules, ImagingView chunk 9.75KB (gzip 4.37KB) |
| 类型检查 | ✅ | vue-tsc 零错误 |
| 端到端预览 | ✅ | xvfb-run 虚拟 X display，Puppeteer 检测到 **3 个 canvas 全部渲染** |
| 截图证据 | ✅ | `/workspace/docs/m3-mpr-view.png`（1440×900 截图，三平面 + 控件 + TraceId） |

### 截图说明

`/workspace/docs/m3-mpr-view.png` 展示了：
- 左侧栏：六域导航，**"影像核心引擎"** 已落地为 M3（绿点 + M3 角标）
- 顶部：患者脱敏信息（`PV-T001-P1001-AccCT001` / `P1001` / `AccCT001`）
- 三正交平面：轴位 30% / 矢状 50% / 冠状 50%，体数据在 GPU 完成窗宽窗位映射
- 控制栏：窗宽 1500、窗位 -500（**肺窗预设已应用**）、缩放 1.0×、CT 预设（腹部/肺部/骨窗/脑部）
- 底部：操作提示 + TraceId（505a0d4b5276…）

---

## 7. 与 M2 的衔接

- **入口**：数据接入层 (`/data-ingestion`) 列表新增 **"阅片"** 按钮，链接到 `/imaging?studyUid=…&patientId=…&accessionNumber=…&patientVisitUid=…&modality=…`
- **统一标识**：`patientVisitUid` 从 M2 一路透传到 mdt-image 审计
- **脱敏一致**：阅片页只显示 `studyUid`/`patientId`/`accessionNumber`，**无姓名/身份证**

---

## 8. 待办 / 已知技术债

- **生产替换缝**：`SyntheticVolumeSource` 待替换为 `WadoRsVolumeSource`（WADO-RS 拉帧 + dcm4che3 解码）；本里程碑不实现，仅保留接口
- **3D MIP / 体积重建**：当前仅 MPR（轴位/矢状/冠状），MIP 列入后续迭代
- **WebGL2 Canvas2D 兜底**：移动端低版本浏览器降级为 Canvas2D 单切片（当前直接抛错"WebGL2 不可用"）
- **传输加密**：研发期 REST 明文；生产期须 HTTPS + DICOM TLS（独立网关迭代）
- **WebGL 头显自动化**：headless Chrome 默认不支持 WebGL2，自动化截图需 `xvfb-run` 提供虚拟 X display（已在 `scripts/m3-e2e-preview.mjs` 验证）
