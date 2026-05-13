package edu.hit.library.common.config;

import edu.hit.library.common.trace.TraceIdMdcFilter;
import edu.hit.library.common.web.GlobalExceptionHandler;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

@Configuration
@Import(GlobalExceptionHandler.class)
public class LibraryCommonWebConfiguration {

    @Bean
    public FilterRegistrationBean<TraceIdMdcFilter> traceIdMdcFilter() {
        FilterRegistrationBean<TraceIdMdcFilter> fr = new FilterRegistrationBean<>();
        fr.setFilter(new TraceIdMdcFilter());
        fr.setOrder(Ordered.HIGHEST_PRECEDENCE);
        fr.addUrlPatterns("/*");
        return fr;
    }
}
