/**
 * M5 Mesh WebRTC 管理器：每对端一个 RTCPeerConnection，信令经 CollabSignal 中继。
 * 生产可换 SFU：把 addPeer/handleSignal 的转发目标从「对端」改为「SFU 媒体地址」即可，前端零改。
 */
export class MeshPeerManager {
  private pcs = new Map<string, RTCPeerConnection>()
  private localStream: MediaStream | null = null
  private rtcConfig: RTCConfiguration = {
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
  }

  /** 远端流回调（peerId → MediaStream），由视图绑定到 <video> */
  onRemote: (peerId: string, stream: MediaStream) => void = () => {}
  /** 信令发送（注入 CollabSignal.send） */
  send: (msg: any) => void = () => {}

  setLocalStream(s: MediaStream) { this.localStream = s }
  setSend(fn: (m: any) => void) { this.send = fn }

  /** 主动向某对端发起连接（收到 welcome/peer-join 时调用） */
  async addPeer(peerId: string) {
    const pc = this.createPc(peerId)
    this.localStream?.getTracks().forEach(t => pc.addTrack(t, this.localStream!))
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    this.send({ type: 'offer', to: peerId, sdp: pc.localDescription })
  }

  /** 处理来自信令服务器的 offer/answer/ice */
  async handleSignal(m: any) {
    const from = m.from as string
    try {
      if (m.type === 'offer') {
        let pc = this.pcs.get(from)
        if (!pc) { pc = this.createPc(from); this.localStream?.getTracks().forEach(t => pc!.addTrack(t, this.localStream!)) }
        await pc.setRemoteDescription(new RTCSessionDescription(m.sdp))
        const answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)
        this.send({ type: 'answer', to: from, sdp: pc.localDescription })
      } else if (m.type === 'answer') {
        const pc = this.pcs.get(from)
        if (pc) await pc.setRemoteDescription(new RTCSessionDescription(m.sdp))
      } else if (m.type === 'ice' && m.candidate) {
        const pc = this.pcs.get(from)
        if (pc) await pc.addIceCandidate(new RTCIceCandidate(m.candidate)).catch(() => {})
      }
    } catch (e) {
      console.warn('[webrtc] handleSignal error', e)
    }
  }

  /** 桌面共享：用屏幕轨道替换所有 PC 的视频发送轨 */
  async replaceVideoTrack(track: MediaStreamTrack | null) {
    for (const pc of this.pcs.values()) {
      const sender = pc.getSenders().find(s => s.track && s.track.kind === 'video')
      if (sender && track) await sender.replaceTrack(track)
    }
  }

  private createPc(peerId: string): RTCPeerConnection {
    const pc = new RTCPeerConnection(this.rtcConfig)
    this.pcs.set(peerId, pc)
    pc.onicecandidate = (e) => {
      if (e.candidate) this.send({ type: 'ice', to: peerId, candidate: e.candidate })
    }
    pc.ontrack = (e) => {
      this.onRemote(peerId, e.streams[0])
    }
    return pc
  }

  close() {
    this.pcs.forEach(pc => pc.close())
    this.pcs.clear()
  }
}
