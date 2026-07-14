// ============================================================================
// 前端强类型接口定义（Vue3 / React + Composition API + TypeScript）
// 与后端 gRPC / RESTful 对齐；所有患者类型仅含脱敏字段（PatientID + 检查号）
// ============================================================================

/** 全链路追踪上下文 */
export interface Trace {
  traceId: string;       // 网关注入，贯穿所有请求
  tenantId: string;      // 租户（医疗机构）
  operatorId: string;    // 操作人
}

/** 脱敏后的患者唯一引用（禁止姓名/身份证） */
export interface DesensitizedPatientRef {
  patientId: string;          // PatientID
  accessionNumber: string;    // 检查号
  patientVisitUid: string;    // 统一就诊唯一标识
}

/** 模态类型（多模态 PACS） */
export type Modality = 'CT' | 'MRI' | 'CR' | 'DR' | 'DSA' | 'RF' | 'US'
  | 'ENDOSCOPY' | 'PATH' | 'ECG';

/** 检查摘要 */
export interface StudySummary {
  patient: DesensitizedPatientRef;
  modality: Modality;
  studyInstanceUid: string;
  studyDate: string;
  description: string;
}

/** 影像核心引擎：窗宽窗位参数 */
export interface WindowLevel {
  windowWidth: number;   // 窗宽
  windowCenter: number;  // 窗位
}

/** MPR 平面 */
export type MprPlane = 'AXIAL' | 'CORONAL' | 'SAGITTAL';

/** 会诊状态（与后端状态机一致） */
export enum ConsultationStatus {
  APPLIED = 'APPLIED',
  NOTIFIED = 'NOTIFIED',
  CONFIRMED = 'CONFIRMED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

/** 会诊视图 */
export interface ConsultationView {
  consultationId: string;
  status: ConsultationStatus;
  patient: DesensitizedPatientRef;
  confirmedCount: number;
  expertTotal: number;
  conclusion: string;
}

/** 协同：标注元素（序列化前后） */
export interface AnnotationElement {
  elementId: string;
  type: 'LENGTH' | 'ANGLE' | 'TEXT' | 'ARROW' | 'RECT';
  points: { x: number; y: number }[];
  label?: string;
  color: string;
  version: number;        // 乐观锁
}

/** 权限：字段级 RBAC 校验请求 */
export interface FieldPermissionRequest {
  trace: Trace;
  role: string;
  resource: string;       // 如 "study.endoscopy"
  field: string;          // 如 "pathology_conclusion"
}

// -------------------- API 客户端接口（强类型） --------------------
export interface ConsultationApi {
  apply(patient: DesensitizedPatientRef, expertIds: string[], trace: Trace): Promise<ConsultationView>;
  notify(consultationId: string, trace: Trace): Promise<void>;
  confirm(consultationId: string, expertId: string, trace: Trace): Promise<ConsultationView>;
  start(consultationId: string, trace: Trace): Promise<ConsultationView>;
  complete(consultationId: string, conclusion: string, trace: Trace): Promise<ConsultationView>;
}

export interface ImageEngineApi {
  /** 零下载流式取帧（WADO-RS 风格） */
  getFrameStream(req: {
    studyInstanceUid: string; seriesInstanceUid: string;
    instanceNumber: number; frameNumber: number; trace: Trace;
  }): AsyncIterable<Uint8Array>;
  /** MPR 重建（GPU） */
  reconstructMpr(req: {
    seriesInstanceUid: string; plane: MprPlane;
    sliceIndex: number; wl: WindowLevel; trace: Trace;
  }): Promise<Uint8Array>;
}

export interface AuthApi {
  checkFieldPermission(req: FieldPermissionRequest): Promise<boolean>;
}
