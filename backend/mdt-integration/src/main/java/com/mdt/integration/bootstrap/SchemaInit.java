package com.mdt.integration.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动期建视图（研发态）：V_PATIENT_STUDIES 与 V_CLINICAL_SUMMARY。
 * 用 CREATE OR REPLACE VIEW（Postgres 语法）；生产按 Oracle/SQL Server 替换 DDL。
 * 必须在基础表（Hibernate ddl-auto=update 创建）就绪后执行，故用 ApplicationRunner。
 */
@Component
@Order(1)
public class SchemaInit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInit.class);
    private final JdbcTemplate jdbc;

    public SchemaInit(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String vStudies = "CREATE OR REPLACE VIEW v_patient_studies AS " +
                "SELECT v.patient_visit_uid, v.tenant_id, v.patient_id, v.accession_number, " +
                "       s.study_instance_uid, s.modality, s.study_date, s.object_key, " +
                "       r.report_id, r.report_content, r.pathology_conclusion, r.publish_time " +
                "FROM patient_visit v " +
                "JOIN study_index s ON s.patient_visit_uid = v.patient_visit_uid " +
                "LEFT JOIN diag_report r ON r.patient_visit_uid = v.patient_visit_uid " +
                "       AND r.modality = s.modality";

        // TODO(MOCK-SWITCH): 视图底表写死 mock_his_demo/mock_emr_encounter；转生产时要么让真实 ETL
        //   写入同名 ODS 表（视图不变），要么提供 Oracle/SQL Server 兼容 DDL。当前为 Postgres 专属语法。
        String vClinical = "CREATE OR REPLACE VIEW v_clinical_summary AS " +
                "SELECT h.patient_visit_uid, h.tenant_id, h.patient_id, h.accession_number, " +
                "       h.gender, h.birth_date, h.dept, h.attending, " +
                "       e.chief_complaint, e.diagnosis, e.allergy, e.medication " +
                "FROM mock_his_demo h " +
                "LEFT JOIN mock_emr_encounter e ON e.patient_visit_uid = h.patient_visit_uid";

        try {
            jdbc.execute(vStudies);
            jdbc.execute(vClinical);
            log.info("[SCHEMA] 视图 v_patient_studies / v_clinical_summary 已就绪");
        } catch (Exception e) {
            log.warn("[SCHEMA] 建视图失败（可忽略/需排查）: {}", e.getMessage());
        }
    }
}
