package edu.hit.library.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 将网关下发的 {@value TraceIds#HEADER} 写入 MDC，便于日志与排障；若缺失则生成。
 */
public class TraceIdMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String tid = request.getHeader(TraceIds.HEADER);
        if (tid == null || tid.isBlank()) {
            tid = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TraceIds.MDC_KEY, tid);
        response.setHeader(TraceIds.HEADER, tid);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIds.MDC_KEY);
        }
    }
}
