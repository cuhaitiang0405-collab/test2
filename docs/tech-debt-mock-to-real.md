# 技术债记录：M2 研发态 Mock → 生产真实数据接口切换

> 状态：**未开始**（研发阶段刻意不做）。触发时机：**转生产 / 接真实 HIS·EMR·PACS 时**。
> 记录目的：避免切换真实数据时被研发态 mock 埋坑；研发阶段新增代码必须遵守文末"不挖坑"约束。

## 背景

M1/M2 为"能测、能看"刻意采用纯 Java 模拟层：
- DICOM：`PureJavaDicomAdapter` + `MockPacs` + `DicomSimulator`（非 dcm4che3）
- 临床数据：`MOCK_HIS_DEMO` / `MOCK_EMR_ENCOUNTER` / `MOCK_LIS_RESULT` 中间表 + `DataInitializer` 种子
- 读模型：`SchemaInit` 建 `V_PATIENT_STUDIES` / `V_CLINICAL_SUMMARY`（Postgres 专属）

当前架构**领域核心已与 mock 解耦**（`IntegrationService.ingest(IngestCommand)` 源无关），切换可行且成本可控，但**缺环境开关与接口化**，无法靠配置一键翻转。

## 边界抽象质量

| 边界 | 抽象质量 | 切换成本 |
|---|---|---|
| DICOM 源 | 高：`DicomAdapter` 接口 + `IngestCommand` DTO | 低 |
| 入库落库 | 高：`IntegrationService.ingest` 源无关 | 无 |
| 临床 HIS/EMR | 中：经 `V_CLINICAL_SUMMARY`，但视图 DDL 绑 `mock_his_demo` | 中 |
| 临床 LIS | 低：`ClinicalDataAdapter` 直接注入 `MockLisResultRepository` 具体类 | 中 |
| 种子数据 | — | 低（需手动关） |
| 视图 DDL | 中：Postgres 专属 + 绑 mock 表名 | 中 |

## 已知缺口（技术债清单）

1. **无环境/Profile 切换开关**：`PureJavaDicomAdapter`、`DataInitializer`、`SchemaInit` 的 mock 视图均为无条件 `@Component`。
2. **`ClinicalDataAdapter` 耦合具体 mock 仓储**：直接 `@Autowired MockLisResultRepository`（非接口）。
3. **`V_CLINICAL_SUMMARY` 视图 DDL 写死 `mock_his_demo`/`mock_emr_encounter` 表名**。
4. **`DataInitializer` 种子无条件执行**，生产需禁用。
5. **`SchemaInit` 视图为 Postgres 专属语法**，未提供 Oracle/SQL Server 兼容版。

## 转生产实施清单（届时执行）

1. 引入 `mdt.integration.mock=true/false` 或 Spring Profile（`dev`/`prod`）。
2. mock 侧组件统一 `@Profile("dev")` / `@ConditionalOnProperty`；真实实现 `@Profile("prod")`。
3. 抽 `LisRepository` 接口，`ClinicalDataAdapter` 仅依赖接口；mock/real 双实现。
4. 明确 **`MOCK_*` 表 = 集成 ODS 暂存表契约**：真实 HIS/EMR/LIS 经 ETL 写入同名表，视图 DDL 可不变；或提供 Oracle/SQL Server 兼容 DDL。
5. 预置 `RealDicomAdapter`（dcm4che3 SCU/SCP）、`RealClinicalAdapter`（HL7-FHIR）骨架。

## 研发阶段"不挖坑"约束（当前及后续 dev 必须遵守）

- 新增 dev 代码**不得**对 `Mock*` 类 / `mock_*` 表名做新的直接依赖；一律经 `DicomAdapter` / 未来 `ClinicalDataSource` 接口。
- `IntegrationService.ingest(IngestCommand)` **必须保持源无关**，不得出现 mock 专用分支。
- 新增读模型视图时，`SELECT` 列即契约，底层源表须可替换；**禁止在 Java 业务代码里散落 `mock_*` 表名**。
- mock 组件内**禁止写真实业务逻辑**；真实逻辑只允许进"真实实现"类（暂未实现也留 TODO）。
- 端口 / AE Title / 密钥等生产配置参数，从一开始用 `@Value`/配置项占位，**禁止硬编码**。

## 代码锚点（TODO 标记位置）

- DICOM 接口（切换缝）：`mdt-integration/.../dicom/DicomAdapter.java`
- DICOM mock 实现（生产移除）：`mdt-integration/.../dicom/PureJavaDicomAdapter.java`
- 临床 LIS 耦合点：`mdt-integration/.../adapter/ClinicalDataAdapter.java`（注入 `MockLisResultRepository` 处）
- 临床视图绑 mock 表：`mdt-integration/.../bootstrap/SchemaInit.java`（`v_clinical_summary` 定义）
- 种子组件（需禁用）：`mdt-integration/.../bootstrap/DataInitializer.java`
