package com.mdt.common.trace;

import java.util.UUID;

/**
 * 全链路追踪上下文：封装 TraceId 的生成与 MDC 存取。
 * 所有域的日志/审计均携带此 TraceId，便于跨服务串联。
 */
public final class TraceContext {
    public static final String HEADER = "X-Mdt-TraceId";
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    private TraceContext() {}

    /** 生成新的 TraceId（UUID，无连字符） */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** 绑定当前线程 TraceId 并写入 MDC（日志框架可输出） */
    public static void set(String traceId) {
        TL.set(traceId);
        org.slf4j.MDC.put(HEADER, traceId);
    }

    public static String get() {
        return TL.get();
    }

    /** 清除（请求结束务必调用，避免线程复用串号） */
    public static void clear() {
        TL.remove();
        org.slf4j.MDC.remove(HEADER);
    }
}
