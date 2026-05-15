package edu.hit.library.gateway.trace;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 入口生成/透传链路 ID，与业务微服务中 MDC 的 traceId 对齐，便于全链路排障。
 */
@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Trace-Id";
    private static final Logger log = LoggerFactory.getLogger(TraceIdGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tid = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (tid == null || tid.isBlank()) {
            tid = UUID.randomUUID().toString().replace("-", "");
        }
        String traceId = tid;
        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER, traceId).build();
        exchange.getResponse().getHeaders().add(HEADER, traceId);
        long startedAt = System.currentTimeMillis();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    int status = mutatedExchange.getResponse().getStatusCode() != null
                            ? mutatedExchange.getResponse().getStatusCode().value()
                            : 0;
                    log.info(
                            "gateway request traceId={} method={} path={} status={} durationMs={}",
                            traceId,
                            request.getMethod(),
                            request.getURI().getPath(),
                            status,
                            System.currentTimeMillis() - startedAt
                    );
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
