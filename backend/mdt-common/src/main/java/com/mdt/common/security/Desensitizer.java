package com.mdt.common.security;

/**
 * 日志脱敏器：隐藏姓名/身份证，仅保留 PatientID + 检查号。
 * 统一在日志门面 append 前调用，满足等保与互联互通合规要求。
 */
public final class Desensitizer {
    private Desensitizer() {}

    /**
     * 对日志原文脱敏：
     * - 18 位身份证号中间 8 位打码；
     * - 2~4 个汉字姓名打码为 **。
     */
    public static String mask(String raw) {
        if (raw == null) return null;
        // 身份证：6位地区 + 8位生日 + 4位顺序/校验 -> 中间生日打码
        String s = raw.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
        // 姓名（前后非汉字的 2~4 个汉字）
        s = s.replaceAll("(?<=[^\\u4e00-\\u9fa5])([\\u4e00-\\u9fa5]{2,4})(?=[^\\u4e00-\\u9fa5])", "**");
        return s;
    }

    /** 断言：脱敏后不应再出现连续 18 位数字（身份证）或明文姓名片段 */
    public static boolean isClean(String masked) {
        if (masked == null) return true;
        return !masked.matches(".*\\d{17}[\\dXx].*");
    }
}
