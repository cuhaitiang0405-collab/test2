package com.mdt.integration.bootstrap;

import com.mdt.common.jpa.PatientVisit;
import com.mdt.common.jpa.PatientVisitRepository;
import com.mdt.integration.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 研发态种子数据：写入 Mock HIS/EMR/LIS 中间表 + PATIENT_VISIT + STUDY_INDEX + DIAG_REPORT。
 * 幂等（按主键 exists 判断），重复启动不重复插入。生产环境由真实源替换，此组件可禁用。
 */
// TODO(MOCK-SWITCH): 转生产须加 @Profile("dev") 或 @ConditionalOnProperty 禁用本种子组件；
//   真实数据由 HIS/EMR/LIS 经 ETL 写入同名 ODS 暂存表（见 docs/tech-debt-mock-to-real.md）。
@Component
@Order(2)
@Profile("!prod")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final PatientVisitRepository pvRepo;
    private final MockHisDemoRepository hisRepo;
    private final MockEmrEncounterRepository emrRepo;
    private final MockLisResultRepository lisRepo;
    private final StudyIndexRepository studyRepo;
    private final DiagReportRepository reportRepo;

    public DataInitializer(PatientVisitRepository pvRepo, MockHisDemoRepository hisRepo,
                           MockEmrEncounterRepository emrRepo, MockLisResultRepository lisRepo,
                           StudyIndexRepository studyRepo, DiagReportRepository reportRepo) {
        this.pvRepo = pvRepo; this.hisRepo = hisRepo; this.emrRepo = emrRepo; this.lisRepo = lisRepo;
        this.studyRepo = studyRepo; this.reportRepo = reportRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedPatient("T001", "P1001", "A202407001", "M", LocalDate.of(1965, 3, 12), "肿瘤科", "张主任",
                "腹痛伴消瘦", "胰腺占位待排", "青霉素", "胰酶肠溶胶囊",
                List.of(
                        new Study("1.2.840.113619.2.1.CT001", "CT", LocalDate.of(2024, 7, 1),
                                "hot://pacs/ct001", 320, "CT 上腹部增强，动脉期可见强化灶。", null),
                        new Study("1.2.840.113619.2.1.MR001", "MRI", LocalDate.of(2024, 7, 2),
                                "hot://pacs/mr001", 240, "MRI 胰腺，胰头见占位信号。", null)),
                List.of(
                        new Lis("L1001", "WBC", "11.2", "10^9/L", "3.5-9.5"),
                        new Lis("L1002", "CA19-9", "86", "U/mL", "0-37")));

        seedPatient("T001", "P1002", "A202407002", "F", LocalDate.of(1972, 9, 8), "消化内科", "李主任",
                "便血伴贫血", "结肠占位", "无", "琥珀酸亚铁",
                List.of(
                        new Study("1.2.840.113619.2.1.US001", "US", LocalDate.of(2024, 7, 3),
                                "hot://pacs/us001", 120, "超声内镜见黏膜下隆起。", null),
                        new Study("1.2.840.113619.2.1.PA001", "PATH", LocalDate.of(2024, 7, 5),
                                "hot://pacs/pa001", 1, "镜下见腺体异型增生。", "结肠腺癌（中分化）")),
                List.of(
                        new Lis("L2001", "Hb", "82", "g/L", "115-150"),
                        new Lis("L2002", "CEA", "12.4", "ng/mL", "0-5")));

        log.info("[SEED] 研发态 Mock 数据已就绪");
    }

    private void seedPatient(String tenant, String pid, String acc, String gender, LocalDate birth,
                             String dept, String attending, String cc, String dx, String allergy, String med,
                             List<Study> studies, List<Lis> lis) {
        String uid = "PV-" + tenant + "-" + pid + "-" + acc;   // 与解析规则一致

        if (!pvRepo.existsById(uid)) {
            pvRepo.save(new PatientVisit(uid, tenant, pid, acc, "OUTPATIENT"));
        }
        if (!hisRepo.existsById(uid)) {
            hisRepo.save(new MockHisDemo(uid, tenant, pid, acc, "OUTPATIENT", gender, birth, dept, attending));
        }
        if (!emrRepo.existsById(uid)) {
            emrRepo.save(new MockEmrEncounter(uid, tenant, cc, dx, allergy, med));
        }
        for (Lis l : lis) {
            if (!lisRepo.existsById(l.id())) {
                lisRepo.save(new MockLisResult(l.id(), uid, tenant, l.item(), l.value(), l.unit(), l.ref(), LocalDateTime.now()));
            }
        }
        for (Study s : studies) {
            if (!studyRepo.existsById(s.uid())) {
                studyRepo.save(new StudyIndex(s.uid(), uid, tenant, s.modality(), s.date(), s.objectKey(), s.count()));
            }
            String reportId = "R-" + s.uid();
            if (!reportRepo.existsById(reportId) && (s.content() != null || s.pathology() != null)) {
                reportRepo.save(new DiagReport(reportId, uid, tenant, s.modality(),
                        s.content(), s.pathology(), LocalDateTime.now()));
            }
        }
    }

    private record Study(String uid, String modality, LocalDate date, String objectKey,
                         int count, String content, String pathology) {}
    private record Lis(String id, String item, String value, String unit, String ref) {}
}
