package edu.hit.library.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimitGatewayFilterTests {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);
    private final InMemoryRateLimitGatewayFilter filter =
            new InMemoryRateLimitGatewayFilter(2, 2, 60, new ObjectMapper(), fixedClock);

    @Test
    void rejectsRequestsAfterBucketIsExhausted() {
        AtomicInteger invocations = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            invocations.incrementAndGet();
            return Mono.empty();
        };

        MockServerWebExchange first = exchange();
        MockServerWebExchange second = exchange();
        MockServerWebExchange third = exchange();

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();
        filter.filter(third, chain).block();

        assertThat(invocations).hasValue(2);
        assertThat(third.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/books")
                        .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 18080))
                        .build()
        );
    }
}
