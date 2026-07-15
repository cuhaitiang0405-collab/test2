// 与 API 网关（8080）对接的强类型 API 封装；前端只认网关，域间由网关路由
import { auth } from './store/auth'

const BASE = '/api'

async function req<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init.headers || {}) }
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json() as Promise<T>
}

export interface LoginResult { token: string; username: string; tenantId: string; role: string }

/** 数据接入层：统一影像索引记录（脱敏后的患者检查视图） */
export interface StudyRecord {
  patientVisitUid: string
  patientId: string
  accessionNumber: string
  studyInstanceUid: string
  modality: string
  studyDate: string | null
  reportId: string | null
  hasPathology: boolean
}

/** 数据接入层：脱敏临床数据（HIS/EMR/LIS 聚合，无姓名/身份证） */
export interface ClinicalSummary {
  success: boolean
  emrJson: string
}

/** GAP-9 诊疗数据细类（结构化分类；后端 ClinicalDataAdapter.buildCategories 产出） */
export interface ClinicalCategory {
  type: string
  items: { name: string; freq: string }[]
}
export interface ClinicalDetail {
  gender?: string
  birthDate?: string | null
  dept?: string
  attending?: string
  chiefComplaint?: string
  diagnosis?: string
  allergy?: string
}
export interface ClinicalCategories {
  admissionRecord: ClinicalDetail
  longTermOrders: ClinicalCategory
  tempOrders: ClinicalCategory
}

/** 数据接入层：模拟一次接入（SCU 拉取 / SCP 推送）的结果 */
export interface SimulateResult {
  mode: 'scu' | 'scp'
  success: boolean
  patientVisitUid?: string
  modality?: string
  instanceCount?: number
  message?: string
}

/* ---------- M4 业务流程层：会诊状态机 ---------- */
export interface ConsultationExpert {
  expertId: string
  expertName: string
  confirmed: boolean
}
export interface Consultation {
  consultationId: string
  patientVisitUid: string
  patientId: string
  accessionNumber?: string
  status: 'APPLIED' | 'NOTIFIED' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  title: string
  reason: string
  applicant: string
  experts: ConsultationExpert[]
  summaryText?: string
  createdAt: string | null
}
export type ConsultationStats = Record<string, number>

/* ---------- M5 协同通讯层：房间/标注 ---------- */
export interface CollabRoom { consultationId: string; roomToken: string; sfuEndpoint: string; wsPath: string }
export interface AnnotationItem {
  version: number
  author: string
  ts: number
  payload: string   // 序列化后的标注操作 JSON
}
// 标注操作（白板一笔），与后端 AnnotationSerializer.AnnotationOp 对齐
export interface AnnotationOp {
  id: string
  type: 'pen' | 'rect' | 'arrow' | 'text'
  points: [number, number][]
  color: string
  author: string
  t: number
  version?: number
}

// 携带操作用户/租户头（与后端登录态对齐；缺省后端回退 WEB/T001）
function authHeaders(): Record<string, string> {
  return {
    'X-Mdt-Operator': auth.state.username || 'WEB',
    'X-Mdt-Tenant': auth.state.tenantId || 'T001'
  }
}

export const consultationApi = {
  apply(p: { patientVisitUid: string; patientId: string; accessionNumber?: string;
            title: string; reason: string; applicant: string;
            expertIds: string[]; expertNames: string[] }) {
    return req('/workflow/consultations', {
      method: 'POST', headers: authHeaders(), body: JSON.stringify(p)
    })
  },
  notify(id: string) { return req<Consultation>(`/workflow/consultations/${id}/notify`, { method: 'POST', headers: authHeaders() }) },
  confirm(id: string, expertId: string) {
    return req<Consultation>(`/workflow/consultations/${id}/confirm?expertId=${encodeURIComponent(expertId)}`, { method: 'POST', headers: authHeaders() })
  },
  start(id: string) { return req<Consultation>(`/workflow/consultations/${id}/start`, { method: 'POST', headers: authHeaders() }) },
  complete(id: string, summaryText: string) {
    return req<Consultation>(`/workflow/consultations/${id}/complete`, { method: 'POST', headers: authHeaders(), body: JSON.stringify({ summaryText }) })
  },
  cancel(id: string) { return req<Consultation>(`/workflow/consultations/${id}/cancel`, { method: 'POST', headers: authHeaders() }) },
  list(status?: string) {
    const q = status ? `?status=${status}` : ''
    return reqTrace<Consultation[]>(`/workflow/consultations${q}`)
  },
  get(id: string) { return reqTrace<Consultation>(`/workflow/consultations/${id}`) },
  // 后端返回结构为 { stats: { APPLIED: n, ... } }，此处解开一层
  stats() { return reqTrace<{ stats: ConsultationStats }>(`/workflow/consultations/stats`) }
}

/** M5 协同通讯层：建房 / 标注回放 / 标注落库 */
export const collabApi = {
  joinRoom(p: { consultationId: string; user: string; tenant?: string; patientVisitUid?: string }) {
    return req<CollabRoom>('/collab/rooms', { method: 'POST', headers: authHeaders(), body: JSON.stringify(p) })
  },
  getAnnotations(consultationId: string) {
    return req<AnnotationItem[]>(`/collab/rooms/${consultationId}/annotations`)
  },
  pushAnnotation(consultationId: string, op: AnnotationOp, author: string) {
    return req<{ success: boolean; version: number; author: string }>(
      `/collab/rooms/${consultationId}/annotations`,
      { method: 'POST', headers: authHeaders(), body: JSON.stringify({ op, author }) }
    )
  }
}

/** 统一封装：返回业务数据 + 响应头中的全链路 TraceId */
export interface ApiResult<T> { data: T; traceId?: string }

/** 发起请求并提取响应头中的 X-Mdt-TraceId（全链路追踪透传） */
async function reqTrace<T>(path: string, init: RequestInit = {}): Promise<ApiResult<T>> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init.headers || {}) }
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const data = (await res.json()) as T
  const tid = res.headers.get('X-Mdt-TraceId')
  return { data, traceId: tid || undefined }
}

export const api = {
  /** 登录（网关透传 X-Mdt-TraceId，返回头中携带） */
  async login(username: string, password: string): Promise<LoginResult> {
    const res = await fetch(`${BASE}/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    if (!res.ok) throw new Error('用户名或密码错误')
    const data = await res.json() as LoginResult
    data.token = data.token // token 来自 body
    // TraceId 取自响应头（全链路追踪）
    const tid = res.headers.get('X-Mdt-TraceId')
    return { ...data, ...(tid ? { traceId: tid } as any : {}) } as LoginResult & { traceId?: string }
  },
  /** 患者就诊索引：写入（字段更新自测） */
  async upsertPatientVisit(pv: any) { return req('/patient-visit', { method: 'POST', body: JSON.stringify(pv) }) },
  async getPatientVisit(uid: string) { return req(`/patient-visit/${uid}`) },

  /* ---------- M2 数据接入层 ---------- */
  /** 模拟一次接入：mode=scu（SCU 拉取）/ scp（SCP 推送），均落 STUDY_INDEX */
  async simulateIngestion(mode: 'scu' | 'scp'): Promise<ApiResult<SimulateResult>> {
    return reqTrace<SimulateResult>(`/integration/simulate?mode=${mode}`, { method: 'POST' })
  },
  /** 数据接入看板：患者全量影像+报告统一视图（脱敏） */
  async listStudies(): Promise<ApiResult<StudyRecord[]>> {
    return reqTrace<StudyRecord[]>('/integration/studies')
  },
  /** 拉取某患者的脱敏临床数据（HIS/EMR/LIS）；emrJson 内含 GAP-9 结构化诊疗细类 */
  async fetchClinical(uid: string): Promise<ApiResult<ClinicalSummary>> {
    return reqTrace<ClinicalSummary>('/integration/clinical', {
      method: 'POST', body: JSON.stringify({ patientVisitUid: uid })
    })
  },
  /** GAP-7 跨机构影像发布（研发期 mock 端点） */
  async publishStudy(p: {
    studyInstanceUid: string; patientId: string; accessionNumber: string; tenantId?: string
  }) {
    return req<{ success: boolean; publishId: string; target: string }>('/integration/publish', {
      method: 'POST', body: JSON.stringify(p)
    })
  },

  /* ---------- M3 影像核心引擎 ---------- */
  /** 获取体数据（GET → Int16 LE 字节流，元数据在响应头） */
  async fetchVolume(studyUid: string, modality = 'CT'): Promise<{
    voxels: ArrayBuffer
    dims: [number, number, number]
    spacing: [number, number, number]
    modality: string
    ww: number
    wl: number
    traceId?: string
  }> {
    const res = await fetch(
      `${BASE}/image/volume?studyUid=${encodeURIComponent(studyUid)}&modality=${encodeURIComponent(modality)}`
    )
    if (!res.ok) throw new Error(`HTTP ${res.status} 获取体数据失败`)

    const dims = res.headers.get('X-Vol-Dims')!.split(',').map(Number) as [number, number, number]
    const spacing = res.headers.get('X-Vol-Spacing')!.split(',').map(Number) as [number, number, number]
    const mod = res.headers.get('X-Vol-Modality')!
    const ww = Number(res.headers.get('X-Vol-Ww'))
    const wl = Number(res.headers.get('X-Vol-Wl'))
    const traceId = res.headers.get('X-Mdt-TraceId') || undefined
    const voxels = await res.arrayBuffer()

    return { voxels, dims, spacing, modality: mod, ww, wl, traceId }
  }
}
