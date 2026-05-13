package edu.hit.library.borrow;

import edu.hit.library.borrow.feign.BorrowOutboundFeignConfig;
import edu.hit.library.common.config.LibraryCommonWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableFeignClients
@Import(LibraryCommonWebConfiguration.class)
@ComponentScan(
        basePackages = "edu.hit.library.borrow",
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BorrowOutboundFeignConfig.class))
public class BorrowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BorrowServiceApplication.class, args);
    }
}
