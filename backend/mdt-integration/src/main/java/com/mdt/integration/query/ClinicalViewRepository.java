package com.mdt.integration.query;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 视图查询仓储（只读）：直接查 V_PATIENT_STUDIES / V_CLINICAL_SUMMARY。
 * 视图由 SchemaInit 在启动时 CREATE OR REPLACE（研发态用 Postgres 语法；生产按库型替换）。
 */
@Repository
public class ClinicalViewRepository {

    private final JdbcTemplate jdbc;

    public ClinicalViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    /** 按租户查询患者全量影像+报告统一视图（供数据接入看板） */
    public List<PatientStudyRow> listPatientStudies(String tenantId) {
        String sql = "SELECT patient_visit_uid, tenant_id, patient_id, accession_number, " +
                "study_instance_uid, modality, study_date, object_key, " +
                "report_id, report_content, pathology_conclusion, publish_time " +
                "FROM v_patient_studies WHERE tenant_id = ? ORDER BY study_date DESC NULLS LAST";
        return jdbc.query(sql, (rs, i) -> mapStudy(rs), tenantId);
    }

    /** 按统一就诊标识查询临床汇总视图（HIS + EMR） */
    public ClinicalSummaryRow fetchClinicalSummary(String patientVisitUid) {
        String sql = "SELECT patient_visit_uid, tenant_id, patient_id, accession_number, " +
                "gender, birth_date, dept, attending, " +
                "chief_complaint, diagnosis, allergy, medication " +
                "FROM v_clinical_summary WHERE patient_visit_uid = ?";
        List<ClinicalSummaryRow> rows = jdbc.query(sql, (rs, i) -> mapClinical(rs), patientVisitUid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private PatientStudyRow mapStudy(ResultSet rs) throws SQLException {
        return new PatientStudyRow(
                rs.getString("patient_visit_uid"), rs.getString("tenant_id"), rs.getString("patient_id"),
                rs.getString("accession_number"), rs.getString("study_instance_uid"), rs.getString("modality"),
                rs.getDate("study_date") == null ? null : rs.getDate("study_date").toLocalDate(),
                rs.getString("object_key"), rs.getString("report_id"), rs.getString("report_content"),
                rs.getString("pathology_conclusion"),
                rs.getTimestamp("publish_time") == null ? null : rs.getTimestamp("publish_time").toLocalDateTime());
    }

    private ClinicalSummaryRow mapClinical(ResultSet rs) throws SQLException {
        return new ClinicalSummaryRow(
                rs.getString("patient_visit_uid"), rs.getString("tenant_id"), rs.getString("patient_id"),
                rs.getString("accession_number"), rs.getString("gender"),
                rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toLocalDate(),
                rs.getString("dept"), rs.getString("attending"),
                rs.getString("chief_complaint"), rs.getString("diagnosis"),
                rs.getString("allergy"), rs.getString("medication"));
    }
}
