package edu.hit.library.borrow.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "borrow_records")
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private Instant borrowTime;

    @Column(nullable = false)
    private Instant dueTime;

    private Instant returnTime;

    @Column(nullable = false, length = 20)
    private String status = "BORROWED";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Instant getBorrowTime() {
        return borrowTime;
    }

    public void setBorrowTime(Instant borrowTime) {
        this.borrowTime = borrowTime;
    }

    public Instant getDueTime() {
        return dueTime;
    }

    public void setDueTime(Instant dueTime) {
        this.dueTime = dueTime;
    }

    public Instant getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(Instant returnTime) {
        this.returnTime = returnTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
