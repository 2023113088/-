package edu.hit.library.borrow;

import edu.hit.library.borrow.client.BookFeignClient;
import edu.hit.library.borrow.client.UserFeignClient;
import edu.hit.library.borrow.client.dto.BookResponse;
import edu.hit.library.borrow.client.dto.UserResponse;
import edu.hit.library.borrow.entity.BorrowRecord;
import edu.hit.library.borrow.repo.BorrowRecordRepository;
import edu.hit.library.borrow.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
class BorrowServiceTests {

    @Autowired private BorrowService borrowService;
    @Autowired private BorrowRecordRepository borrows;

    @MockBean private BookFeignClient books;
    @MockBean private UserFeignClient users;

    @BeforeEach
    void clean() {
        borrows.deleteAll();
        when(users.getUser(1L)).thenReturn(new UserResponse(1L, "reader1", "reader1@test.com", "READER"));
        when(books.getBook(1L)).thenReturn(new BookResponse(1L, "Cloud Native", "Author", "isbn", "pub", "cat", 2, 0));
        when(books.adjustStock(eq(1L), any())).thenReturn(new BookResponse(1L, "Cloud Native", "Author", "isbn", "pub", "cat", 1, 0));
    }

    @Test
    void borrowsReturnsAndRejectsDuplicateBorrow() {
        BorrowRecord record = borrowService.borrow(1L, 1L);
        assertThat(record.getStatus()).isEqualTo("BORROWED");

        assertThatThrownBy(() -> borrowService.borrow(1L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        BorrowRecord returned = borrowService.returnBook(1L, record.getId());
        assertThat(returned.getStatus()).isEqualTo("RETURNED");
    }

    @Test
    void reportsUnavailableBookServiceWhenFallbackReturnsNull() {
        when(books.getBook(1L)).thenReturn(null);

        assertThatThrownBy(() -> borrowService.borrow(1L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503 SERVICE_UNAVAILABLE");
    }
}
