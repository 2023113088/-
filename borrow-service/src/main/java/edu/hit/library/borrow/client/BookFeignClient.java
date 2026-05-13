package edu.hit.library.borrow.client;

import edu.hit.library.borrow.client.dto.BookResponse;
import edu.hit.library.borrow.client.dto.StockDelta;
import edu.hit.library.borrow.feign.BorrowOutboundFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "BOOK-SERVICE",
        contextId = "bookClient",
        fallbackFactory = BookFeignFallbackFactory.class,
        configuration = BorrowOutboundFeignConfig.class)
public interface BookFeignClient {

    @GetMapping("/api/books/{id}")
    BookResponse getBook(@PathVariable("id") Long id);

    @PutMapping("/api/books/{id}/stock")
    BookResponse adjustStock(@PathVariable("id") Long id, @RequestBody StockDelta delta);
}
