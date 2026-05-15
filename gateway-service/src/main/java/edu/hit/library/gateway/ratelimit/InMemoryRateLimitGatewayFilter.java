package edu.hit.library.gateway.ratelimit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimitGatewayFilter implements GlobalFilter, Ordered {

    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodSeconds;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryRateLimitGatewayFilter(
            @Value("${app.rate-limit.capacity:20}") int capacity,
            @Value("${app.rate-limit.refill-tokens:20}") int refillTokens,
            @Value("${app.rate-limit.refill-period-seconds:60}") long refillPeriodSeconds,
            ObjectMapper objectMapper
    ) {
        this(capacity, refillTokens, refillPeriodSeconds, objectMapper, Clock.systemUTC());
    }

    InMemoryRateLimitGatewayFilter(
            int capacity,
            int refillTokens,
            long refillPeriodSeconds,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodSeconds = refillPeriodSeconds;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = clientKey(exchange.getRequest());
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, clock.instant()));
        if (!bucket.tryConsume(clock.instant(), capacity, refillTokens, refillPeriodSeconds)) {
            return tooManyRequests(exchange);
        }
        return chain.filter(exchange);
    }

    private static String clientKey(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String host = "unknown";
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            host = "unknown";
        } else {
            host = remoteAddress.getAddress().getHostAddress();
        }
        return host + ":" + request.getMethod() + ":" + request.getURI().getPath();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/problem+json"));
        try {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests, please retry later"
            );
            pd.setTitle("Too Many Requests");
            byte[] bytes = objectMapper.writeValueAsBytes(pd);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    private static final class Bucket {
        private int tokens;
        private Instant lastRefillAt;

        private Bucket(int tokens, Instant lastRefillAt) {
            this.tokens = tokens;
            this.lastRefillAt = lastRefillAt;
        }

        private synchronized boolean tryConsume(
                Instant now,
                int capacity,
                int refillTokens,
                long refillPeriodSeconds
        ) {
            long elapsedSeconds = now.getEpochSecond() - lastRefillAt.getEpochSecond();
            if (elapsedSeconds >= refillPeriodSeconds) {
                long periods = elapsedSeconds / refillPeriodSeconds;
                long replenished = periods * refillTokens;
                tokens = (int) Math.min(capacity, tokens + replenished);
                lastRefillAt = lastRefillAt.plusSeconds(periods * refillPeriodSeconds);
            }
            if (tokens <= 0) {
                return false;
            }
            tokens--;
            return true;
        }
    }
}
