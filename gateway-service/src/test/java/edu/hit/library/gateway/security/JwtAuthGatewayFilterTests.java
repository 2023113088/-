package edu.hit.library.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthGatewayFilterTests {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";
    private final JwtAuthGatewayFilter filter = new JwtAuthGatewayFilter(SECRET, new ObjectMapper());

    @Test
    void rejectsProtectedRequestWithoutToken() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/borrows").build());

        filter.filter(exchange, noopChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsReaderManagingBooks() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("1", "READER"))
                        .build());

        filter.filter(exchange, noopChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void allowsAdminManagingBooksAndPropagatesHeaders() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("2", "ADMIN"))
                        .build());

        filter.filter(exchange, ex -> {
            invoked.set(true);
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("2");
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN");
            return Mono.empty();
        }).block();

        assertThat(invoked).isTrue();
    }

    private static GatewayFilterChain noopChain() {
        return exchange -> Mono.empty();
    }

    private static String token(String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();
    }
}
