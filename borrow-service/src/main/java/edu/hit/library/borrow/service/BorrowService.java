package edu.hit.library.borrow.service;

import edu.hit.library.borrow.client.BookFeignClient;
import edu.hit.library.borrow.client.UserFeignClient;
import edu.hit.library.borrow.client.dto.BookResponse;
import edu.hit.library.borrow.client.dto.StockDelta;
import edu.hit.library.borrow.entity.BorrowRecord;
import edu.hit.library.borrow.repo.BorrowRecordRepository;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class BorrowService {

    private static final Duration LOAN_PERIOD = Duration.ofDays(30);

    private final BorrowRecordRepository borrows;
    private final BookFeignClient books;
    private final UserFeignClient users;

    public BorrowService(BorrowRecordRepository borrows, BookFeignClient books, UserFeignClient users) {
        this.borrows = borrows;
        this.books = books;
        this.users = users;
    }

    @Transactional
    public BorrowRecord borrow(Long userId, Long bookId) {
        ensureUserExists(userId);
        if (borrows.existsByUserIdAndBookIdAndStatus(userId, bookId, "BORROWED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该图书尚未归还，不能重复借阅");
        }
        BookResponse book = books.getBook(bookId);
        if (book == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "图书服务不可用（熔断/降级），请稍后重试");
        }
        if (book.stock() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存不足");
        }
        BookResponse after = books.adjustStock(bookId, new StockDelta(-1));
        if (after == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "扣减库存失败：图书服务不可用（熔断/降级）");
        }
        Instant now = Instant.now();
        BorrowRecord r = new BorrowRecord();
        r.setUserId(userId);
        r.setBookId(bookId);
        r.setBorrowTime(now);
        r.setDueTime(now.plus(LOAN_PERIOD));
        r.setStatus("BORROWED");
        return borrows.save(r);
    }

    @Transactional
    public BorrowRecord returnBook(Long userId, Long borrowId) {
        ensureUserExists(userId);
        BorrowRecord r = borrows.findById(borrowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "借阅记录不存在"));
        if (!r.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权归还该记录");
        }
        if (!"BORROWED".equals(r.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该记录已归还");
        }
        BookResponse after = books.adjustStock(r.getBookId(), new StockDelta(1));
        if (after == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "归还失败：图书服务不可用（熔断/降级）");
        }
        Instant now = Instant.now();
        boolean late = now.isAfter(r.getDueTime());
        r.setReturnTime(now);
        r.setStatus(late ? "RETURNED_LATE" : "RETURNED");
        return borrows.save(r);
    }

    @Transactional(readOnly = true)
    public List<BorrowRecord> listForUser(Long userId) {
        ensureUserExists(userId);
        return borrows.findByUserIdOrderByBorrowTimeDesc(userId);
    }

    private void ensureUserExists(Long userId) {
        try {
            users.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或登录信息无效");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "用户服务不可用，请稍后重试");
        }
    }
}
