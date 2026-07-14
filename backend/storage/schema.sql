-- ============================================================================
-- 多学科会诊中心系统 — 存储 DDL（Oracle / SQL Server 兼容）
-- 约定：影像与非影像数据共用统一就诊唯一标识 PatientVisitUID
--       所有业务表带 TenantId 以支持多租户行级隔离
--       文件以"中间表"关联对象存储路径，索引/报告入关系库
-- ============================================================================

-- -------------------- 患者就诊主索引（统一标识） --------------------
CREATE TABLE PATIENT_VISIT (
    PATIENT_VISIT_UID  VARCHAR2(64)  NOT NULL,   -- 统一就诊唯一标识（核心外键）
    TENANT_ID          VARCHAR2(32)  NOT NULL,   -- 租户（医疗机构）
    PATIENT_ID         VARCHAR2(64)  NOT NULL,   -- PatientID（脱敏后保留）
    ACCESSION_NUMBER   VARCHAR2(64)  NOT NULL,   -- 检查号（脱敏后保留）
    VISIT_TYPE         VARCHAR2(16),             -- INPATIENT / OUTPATIENT
    CREATE_TIME        TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT PK_PATIENT_VISIT PRIMARY KEY (PATIENT_VISIT_UID)
);
-- 索引优化建议：TenantId + PatientId 为最高频查询维度，建组合索引
CREATE INDEX IDX_PV_TENANT_PID ON PATIENT_VISIT (TENANT_ID, PATIENT_ID);
CREATE INDEX IDX_PV_ACCESSION  ON PATIENT_VISIT (TENANT_ID, ACCESSION_NUMBER);

-- -------------------- 检查/影像索引（中间表，关联对象存储） --------------------
CREATE TABLE STUDY_INDEX (
    STUDY_INSTANCE_UID VARCHAR2(64)  NOT NULL,
    PATIENT_VISIT_UID  VARCHAR2(64)  NOT NULL,   -- 统一就诊标识
    TENANT_ID          VARCHAR2(32)  NOT NULL,
    MODALITY           VARCHAR2(16)  NOT NULL,   -- CT/MRI/US/ENDOSCOPY/PATH/ECG
    STUDY_DATE         DATE,
    OBJECT_KEY         VARCHAR2(512),            -- 对象存储路径（影像文件）
    INSTANCE_COUNT     NUMBER(9),
    CONSTRAINT PK_STUDY PRIMARY KEY (STUDY_INSTANCE_UID),
    CONSTRAINT FK_STUDY_VISIT FOREIGN KEY (PATIENT_VISIT_UID)
        REFERENCES PATIENT_VISIT (PATIENT_VISIT_UID)
);
CREATE INDEX IDX_STUDY_VISIT ON STUDY_INDEX (PATIENT_VISIT_UID);
CREATE INDEX IDX_STUDY_TENANT_MOD ON STUDY_INDEX (TENANT_ID, MODALITY); -- 按机构+模态查询

-- -------------------- 报告（放射/超声/内镜/病理/心电） --------------------
CREATE TABLE DIAG_REPORT (
    REPORT_ID          VARCHAR2(64)  NOT NULL,
    PATIENT_VISIT_UID  VARCHAR2(64)  NOT NULL,
    TENANT_ID          VARCHAR2(32)  NOT NULL,
    MODALITY           VARCHAR2(16)  NOT NULL,
    REPORT_CONTENT     CLOB,                     -- 报告正文（字段级权限管控）
    PATHOLOGY_CONCLUSION VARCHAR2(4000),         -- 病理结论（受限字段示例）
    PUBLISH_TIME       TIMESTAMP,
    CONSTRAINT PK_REPORT PRIMARY KEY (REPORT_ID),
    CONSTRAINT FK_REPORT_VISIT FOREIGN KEY (PATIENT_VISIT_UID)
        REFERENCES PATIENT_VISIT (PATIENT_VISIT_UID)
);
CREATE INDEX IDX_REPORT_VISIT ON DIAG_REPORT (PATIENT_VISIT_UID);
CREATE INDEX IDX_REPORT_TENANT ON DIAG_REPORT (TENANT_ID, MODALITY);

-- -------------------- 会诊主表（状态机持久化） --------------------
CREATE TABLE CONSULTATION (
    CONSULTATION_ID    VARCHAR2(64)  NOT NULL,
    PATIENT_VISIT_UID  VARCHAR2(64)  NOT NULL,
    TENANT_ID          VARCHAR2(32)  NOT NULL,
    STATUS             VARCHAR2(16)  NOT NULL,   -- APPLIED/NOTIFIED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED
    EXPERT_TOTAL       NUMBER(4),
    CONFIRMED_COUNT    NUMBER(4)     DEFAULT 0,
    CONCLUSION         CLOB,
    CREATE_TIME        TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT PK_CONSULTATION PRIMARY KEY (CONSULTATION_ID)
);
CREATE INDEX IDX_CONS_STATUS ON CONSULTATION (STATUS, TENANT_ID); -- 待办列表高频
CREATE INDEX IDX_CONS_VISIT  ON CONSULTATION (PATIENT_VISIT_UID);

-- -------------------- 审计表（TraceId + 脱敏后标识） --------------------
CREATE TABLE AUDIT_LOG (
    AUDIT_ID           VARCHAR2(64)  NOT NULL,
    TRACE_ID           VARCHAR2(64)  NOT NULL,   -- 全链路追踪
    TENANT_ID          VARCHAR2(32)  NOT NULL,
    OPERATOR_ID        VARCHAR2(64)  NOT NULL,
    PATIENT_VISIT_UID  VARCHAR2(64),             -- 脱敏后仅统一标识
    ACTION             VARCHAR2(32),
    DETAIL             VARCHAR2(1000),
    TS                 TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT PK_AUDIT PRIMARY KEY (AUDIT_ID)
);
CREATE INDEX IDX_AUDIT_TRACE ON AUDIT_LOG (TRACE_ID);
CREATE INDEX IDX_AUDIT_TENANT_TS ON AUDIT_LOG (TENANT_ID, TS DESC); -- 审计检索

-- -------------------- 视图：患者全量影像+报告统一视图 --------------------
-- 前端"一键调阅"同一患者历史检查与报告时直接查此视图
CREATE VIEW V_PATIENT_STUDIES AS
SELECT v.PATIENT_VISIT_UID, v.TENANT_ID, v.PATIENT_ID, v.ACCESSION_NUMBER,
       s.STUDY_INSTANCE_UID, s.MODALITY, s.STUDY_DATE, s.OBJECT_KEY,
       r.REPORT_ID, r.REPORT_CONTENT, r.PATHOLOGY_CONCLUSION, r.PUBLISH_TIME
FROM PATIENT_VISIT v
JOIN STUDY_INDEX s  ON s.PATIENT_VISIT_UID = v.PATIENT_VISIT_UID
LEFT JOIN DIAG_REPORT r ON r.PATIENT_VISIT_UID = v.PATIENT_VISIT_UID
                       AND r.MODALITY = s.MODALITY;

-- -------------------- 生命周期：归档/清理定时任务 --------------------
-- 归档：将超过留存期（如 3650 天）的影像冷存，OBJECT_KEY 指向归档桶
-- 清理：超长留存（如 5475 天）且已归档者物理删除，删除动作写 AUDIT_LOG
-- 建议用 DBMS_SCHEDULER（Oracle）/ SQL Agent（SQL Server）每日凌晨执行：
--   UPDATE STUDY_INDEX SET OBJECT_KEY = REPLACE(OBJECT_KEY,'hot','cold')
--     WHERE STUDY_DATE < ADD_MONTHS(SYSDATE,-120) AND OBJECT_KEY NOT LIKE '%cold%';
--   DELETE FROM STUDY_INDEX WHERE STUDY_DATE < ADD_MONTHS(SYSDATE,-180)
--     AND OBJECT_KEY LIKE '%cold%';  -- 物理清理（先审计）

-- ============================ SQL 优化建议 ============================
-- 1) 所有多租户查询必带 TENANT_ID，组合索引把 TENANT_ID 放首位。
-- 2) 影像取帧走对象存储直链，关系库只存 OBJECT_KEY，避免大字段进库。
-- 3) 高频"待确认会诊列表"走 IDX_CONS_STATUS(STATUS,TENANT_ID)。
-- 4) 报告大文本用 CLOB + 字段级权限，越权字段在应用层拦截（见 RbacTenantService）。
-- 5) 视图 V_PATIENT_STUDIES 用于只读聚合，写操作仍走基表。
