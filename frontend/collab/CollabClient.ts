// ============================================================================
// 协同通讯层 — 标注序列化 + WebRTC SFU 信令 + 电子白板
// 标注以增量消息经 WebSocket/DataChannel 广播；序列化后支持回放与持久化。
// ============================================================================

import type { AnnotationElement, Trace } from '../types/mdt';

/** 标注序列化器：对象 <-> Protobuf/JSON（与后端 AnnotationMessage.payload 对齐） */
export class AnnotationSerializer {
  /** 序列化（增量广播用，二进制更省带宽） */
  static encode(el: AnnotationElement): Uint8Array {
    // 真实实现用 protobufjs；此处用紧凑 JSON + Transferable
    return new TextEncoder().encode(JSON.stringify(el));
  }
  /** 反序列化 */
  static decode(buf: Uint8Array): AnnotationElement {
    return JSON.parse(new TextDecoder().decode(buf)) as AnnotationElement;
  }
}

/** 协同房间客户端：白板/桌面共享 + SFU 音视频 */
export class CollabClient {
  private ws: WebSocket;
  private pc?: RTCPeerConnection;     // WebRTC 连接到 SFU
  private localStream?: MediaStream;

  constructor(private consultationId: string, private trace: Trace,
              private sfuEndpoint: string) {
    // 标注/白板增量走 WebSocket（可靠有序），媒体走 WebRTC（SFU 转发）
    this.ws = new WebSocket(`wss://${location.host}/collab/${consultationId}`);
    this.ws.onmessage = (ev) => this.onAnnotation(AnnotationSerializer.decode(
      new TextEncoder().encode(ev.data)));
  }

  /** 推送标注增量（乐观锁 version 防冲突） */
  pushAnnotation(el: AnnotationElement) {
    this.ws.send(new TextEncoder().decode(AnnotationSerializer.encode(el)));
  }

  private onAnnotation(el: AnnotationElement) {
    // 渲染到电子白板 / 同步到其他专家画布
    console.debug('[collab] recv annotation', el.elementId, 'v=', el.version);
  }

  /** 加入 SFU：获取媒体地址与房间令牌后建立 PeerConnection */
  async joinWithMedia(video: boolean, audio: boolean) {
    this.localStream = await navigator.mediaDevices.getUserMedia({ video, audio });
    this.pc = new RTCPeerConnection({
      iceServers: [{ urls: 'stun:stun.mdt.local:3478' }],
    });
    this.localStream.getTracks().forEach(t => this.pc!.addTrack(t, this.localStream!));
    // 与 SFU 协商：仅上行一路，下行 N-1 路（SFU 转发）
    const offer = await this.pc.createOffer();
    await this.pc.setLocalDescription(offer);
    // 将 offer 经信令发给 SFU（/sfu/signal），换取 answer
  }

  /** 桌面共享：替换视频轨道为 getDisplayMedia 流 */
  async shareDesktop() {
    const display = await navigator.mediaDevices.getDisplayMedia({ video: true });
    const sender = this.pc?.getSenders().find(s => s.track?.kind === 'video');
    await sender?.replaceTrack(display.getVideoTracks()[0]);
  }

  /** 带宽自适应：根据上行质量切换 simulcast 层级 */
  reportQuality(uplinkKbps: number, packetLoss: number) {
    const layer = packetLoss > 5 ? 'low' : uplinkKbps < 800 ? 'mid' : 'high';
    this.ws.send(JSON.stringify({ type: 'quality', layer, packetLoss }));
  }

  dispose() {
    this.pc?.close();
    this.ws.close();
    this.localStream?.getTracks().forEach(t => t.stop());
  }

  // ============================ 单元/压测用例建议 ============================
  // 1) AnnotationSerializer：encode->decode 往返后字段完全一致（含 points/version）。
  // 2) 并发标注：A/B 同时画同 elementId，乐观锁 version 保证后者覆盖或合并。
  // 3) SFU 断线：pc.onconnectionstatechange=disconnected 触发自动重连（joinWithMedia 重试）。
  // 4) 桌面共享：shareDesktop 后远端收到 display 轨道，stop 后回退摄像头。
  // 5) 质量自适应：注入高 packetLoss，断言 reportQuality 切到 low 层。
}
