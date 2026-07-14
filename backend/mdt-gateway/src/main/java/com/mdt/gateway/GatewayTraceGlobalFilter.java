package com.mdt.gateway;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关全局过滤器（响应式 WebFlux）：
 * 生成/透传 X-Mdt-TraceId，并向下游服务转发，保证跨域全链路追踪。
 * 实现 Ordered 使过滤器在最高优先级执行（最外层注入 TraceId）。
 */
@Configuration
public class GatewayTraceGlobalFilter {

    @Bean
    public GlobalFilter traceFilter() {
        return new TraceGlobalFilter();
    }

    /** 具名过滤器：同时实现 GlobalFilter 与 Ordered */
    public static class TraceGlobalFilter implements GlobalFilter, Ordered {
        @Override
        public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                                 org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
            String incoming = exchange.getRequest().getHeaders().getFirst("X-Mdt-TraceId");
            final String tid = (incoming == null || incoming.isBlank())
                    ? UUID.randomUUID().toString().replace("-", "") : incoming;
            MDC.put("X-Mdt-TraceId", tid);
            // 响应头回写 + 向下游请求头透传
            exchange.getResponse().getHeaders().add("X-Mdt-TraceId", tid);
            var mutated = exchange.mutate()
                    .request(r -> r.headers(h -> h.add("X-Mdt-TraceId", tid)))
                    .build();
            return chain.filter(mutated).then(Mono.fromRunnable(() -> MDC.remove("X-Mdt-TraceId")));
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
