package edu.hit.library.borrow.web.dto;

import edu.hit.library.borrow.entity.BorrowRecord;

import java.time.Instant;

public record BorrowResponse(
        Long id,
        Long userId,
        Long bookId,
        Instant borrowTime,
        Instant dueTime,
        Instant returnTime,
        String status) {

    public static BorrowResponse fromEntity(BorrowRecord r) {
        String s = r.getStatus();
        if ("BORROWED".equals(s) && Instant.now().isAfter(r.getDueTime())) {
            s = "OVERDUE";
        }
        return new BorrowResponse(
                r.getId(), r.getUserId(), r.getBookId(), r.getBorrowTime(), r.getDueTime(), r.getReturnTime(), s);
    }
}
