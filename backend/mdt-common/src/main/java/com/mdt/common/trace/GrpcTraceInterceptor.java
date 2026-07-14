package com.mdt.common.trace;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * gRPC 服务端拦截器：从 metadata 读取 trace-id 并写入 MDC，
 * 使 gRPC 调用与 REST 调用共享同一套全链路追踪。缺失时自动生成。
 */
public class GrpcTraceInterceptor implements ServerInterceptor {
    public static final Metadata.Key<String> TRACE_KEY =
            Metadata.Key.of("x-mdt-traceid", Metadata.ASCII_STRING_MARSHALLER);

    private static final Context.Key<String> TRACE_CTX = Context.key("traceId");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String traceId = headers.get(TRACE_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceContext.newTraceId();
        }
        final String finalTraceId = traceId;
        // gRPC Context 会随调用自动跨线程 attach，比 MDC(ThreadLocal) 更适合服务端线程切换
        Context ctx = Context.current().withValue(TRACE_CTX, finalTraceId);
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    /** 取当前 TraceId：优先 gRPC Context，其次 REST 的 MDC，最后生成（保证非空） */
    public static String currentTraceId() {
        String v = TRACE_CTX.get();
        if (v != null) return v;
        v = TraceContext.get();
        return v != null ? v : TraceContext.newTraceId();
    }
}
