package edu.hit.library.borrow.web;

import edu.hit.library.borrow.entity.BorrowRecord;
import edu.hit.library.borrow.service.BorrowService;
import edu.hit.library.borrow.web.dto.BorrowResponse;
import edu.hit.library.borrow.web.dto.CreateBorrowRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    private static Long userIdFromHeader(@RequestHeader(value = "X-User-Id", required = false) String uid) {
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少登录信息");
        }
        try {
            return Long.parseLong(uid.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户标识无效");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowResponse borrow(@RequestHeader("X-User-Id") String uidHeader, @Valid @RequestBody CreateBorrowRequest body) {
        Long userId = userIdFromHeader(uidHeader);
        if (body.bookId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookId 必填");
        }
        BorrowRecord r = borrowService.borrow(userId, body.bookId());
        return BorrowResponse.fromEntity(r);
    }

    @PutMapping("/{id}/return")
    public BorrowResponse returnBook(@RequestHeader("X-User-Id") String uidHeader, @PathVariable Long id) {
        Long userId = userIdFromHeader(uidHeader);
        BorrowRecord r = borrowService.returnBook(userId, id);
        return BorrowResponse.fromEntity(r);
    }

    @GetMapping
    public List<BorrowResponse> list(@RequestHeader("X-User-Id") String uidHeader) {
        Long userId = userIdFromHeader(uidHeader);
        return borrowService.listForUser(userId).stream().map(BorrowResponse::fromEntity).toList();
    }
}
