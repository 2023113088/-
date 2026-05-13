package edu.hit.library.borrow.feign;

import edu.hit.library.common.security.InternalApiHeaders;
import edu.hit.library.common.trace.TraceIds;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 出站：链路追踪 + 服务间库存接口令牌。
 */
@Configuration(proxyBeanMethods = false)
public class BorrowOutboundFeignConfig {

    @Bean
    public RequestInterceptor tracePropagatingInterceptor() {
        return template -> {
            var ra = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (ra instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String tid = sra.getRequest().getHeader(TraceIds.HEADER);
                if (tid != null && !tid.isBlank()) {
                    template.header(TraceIds.HEADER, tid);
                }
            }
        };
    }

    @Bean
    public RequestInterceptor internalServiceTokenInterceptor(@Value("${app.internal.api-token}") String token) {
        return template -> template.header(InternalApiHeaders.SERVICE_TOKEN, token);
    }
}
