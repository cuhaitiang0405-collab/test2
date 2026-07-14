package com.mdt.common.trace;

import io.grpc.*;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

/**
 * 客户端 TraceId 透传拦截器：把当前 REST 线程的 TraceId（MDC）写入 gRPC metadata，
 * 供对端 GrpcTraceInterceptor 接收，保证「网关 REST → 域内 gRPC」全链路一致。
 */
public class ClientTraceInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> TRACE_KEY =
            Metadata.Key.of("x-mdt-traceid", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        String tid = TraceContext.get();
        if (tid == null || tid.isBlank()) tid = TraceContext.newTraceId();
        Metadata headers = new Metadata();
        headers.put(TRACE_KEY, tid);
        return MetadataUtils.newAttachHeadersInterceptor(headers).interceptCall(method, callOptions, next);
    }
}
