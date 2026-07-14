package com.mdt.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * REST 入口过滤器（网关/各服务共用）：
 * 1) 若请求无 X-Mdt-TraceId 则生成；2) 写入 MDC；3) 回写响应头，保证全链路透传。
 */
@Component
@Order(-100)
public class RestTraceFilter implements jakarta.servlet.Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest req,
                         jakarta.servlet.ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String traceId = request.getHeader(TraceContext.HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceContext.newTraceId();
        }
        TraceContext.set(traceId);
        response.setHeader(TraceContext.HEADER, traceId);   // 透传给调用方
        try {
            chain.doFilter(req, res);
        } finally {
            TraceContext.clear();
        }
    }
}
