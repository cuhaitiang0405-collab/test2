package com.mdt.auth;

import com.mdt.common.security.Desensitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * M1 自测：日志脱敏 —— 姓名/身份证必须被掩码，仅保留 PatientID + 检查号。
 */
class DesensitizerTest {

    @Test
    void masksIdCardAndName() {
        String raw = "患者 张三 身份证 110105199003071234 检查号 A456";
        String masked = Desensitizer.mask(raw);
        // 身份证 18 位中间打码
        assertThat(masked).doesNotContain("110105199003071234");
        assertThat(masked).contains("110105********1234");
        // 姓名打码
        assertThat(masked).doesNotContain("张三");
        assertThat(masked).contains("**");
        // 检查号（脱敏后保留）仍在
        assertThat(masked).contains("A456");
        // 断言：脱敏后无明文身份证
        assertThat(Desensitizer.isClean(masked)).isTrue();
    }

    @Test
    void keepsPatientIdAndAccession() {
        String raw = "PatientID=P123 AccessionNumber=A456";
        assertThat(Desensitizer.mask(raw)).isEqualTo(raw); // 本就无 PII，保持不变
    }
}
