package edu.hit.library.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private static final Pattern BOOK_GET_BY_ID = Pattern.compile("^/api/books/\\d+$");
    private static final Pattern BOOK_MANAGEMENT_PATH = Pattern.compile("^/api/books(?:/\\d+)?$");

    private final SecretKey key;
    private final ObjectMapper objectMapper;

    public JwtAuthGatewayFilter(@Value("${app.jwt.secret}") String secret, ObjectMapper objectMapper) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 256 bits for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.objectMapper = objectMapper;
    }

    private static boolean permitsAnonymous(HttpMethod method, String path) {
        if (method == HttpMethod.OPTIONS) {
            return true;
        }
        if (method == HttpMethod.POST && ("/api/users/register".equals(path) || "/api/auth/login".equals(path))) {
            return true;
        }
        if (method == HttpMethod.GET
                && ("/api/books".equals(path) || "/api/books/search".equals(path) || "/api/books/instance".equals(path))) {
            return true;
        }
        return method == HttpMethod.GET && BOOK_GET_BY_ID.matcher(path).matches();
    }

    private static boolean requiresAdmin(HttpMethod method, String path) {
        return (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.DELETE)
                && BOOK_MANAGEMENT_PATH.matcher(path).matches();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        if (permitsAnonymous(method, path)) {
            return chain.filter(exchange);
        }
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange, "需要登录：缺少或格式错误的 Authorization Bearer 令牌");
        }
        String token = auth.substring(7).trim();
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (requiresAdmin(method, path) && !"ADMIN".equals(claims.get("role", String.class))) {
                return forbidden(exchange, "Only administrators can manage books");
            }
            String sub = claims.getSubject();
            String role = claims.get("role", String.class);
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", sub)
                    .header("X-User-Role", role != null ? role : "")
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return unauthorized(exchange, "令牌无效或已过期");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String detail) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/problem+json"));
        try {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
            pd.setTitle("Unauthorized");
            byte[] bytes = objectMapper.writeValueAsBytes(pd);
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String detail) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/problem+json"));
        try {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
            pd.setTitle("Forbidden");
            byte[] bytes = objectMapper.writeValueAsBytes(pd);
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
