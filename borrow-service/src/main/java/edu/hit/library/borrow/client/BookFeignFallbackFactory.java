package edu.hit.library.borrow.client;

import edu.hit.library.borrow.client.dto.BookResponse;
import edu.hit.library.borrow.client.dto.StockDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class BookFeignFallbackFactory implements FallbackFactory<BookFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(BookFeignFallbackFactory.class);

    @Override
    public BookFeignClient create(Throwable cause) {
        return new BookFeignClient() {
            @Override
            public BookResponse getBook(Long id) {
                log.warn("BookFeign 熔断/降级 getBook id={} : {}", id, cause.toString());
                return null;
            }

            @Override
            public BookResponse adjustStock(Long id, StockDelta delta) {
                log.warn("BookFeign 熔断/降级 adjustStock id={} delta={} : {}", id, delta.delta(), cause.toString());
                return null;
            }
        };
    }
}
