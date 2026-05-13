package edu.hit.library.book.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import edu.hit.library.common.security.InternalApiHeaders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 库存调整仅允许携带正确服务间令牌的调用（借阅服务经 Feign 传入），防止直连图书服务绕过业务规则。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class InternalStockProtectionFilter extends OncePerRequestFilter {

    private static final Pattern STOCK_PATH = Pattern.compile("^/api/books/\\d+/stock$");

    private final String expectedToken;

    public InternalStockProtectionFilter(@Value("${app.internal.api-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!"PUT".equalsIgnoreCase(request.getMethod()) || !STOCK_PATH.matcher(request.getRequestURI()).matches()) {
            filterChain.doFilter(request, response);
            return;
        }
        String presented = request.getHeader(InternalApiHeaders.SERVICE_TOKEN);
        if (presented == null || !expectedToken.equals(presented)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"title\":\"Forbidden\",\"status\":403,\"detail\":\"库存接口仅允许受信服务调用\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
