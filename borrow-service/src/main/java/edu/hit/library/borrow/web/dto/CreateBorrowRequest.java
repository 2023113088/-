package edu.hit.library.borrow.web.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBorrowRequest(@NotNull Long bookId) {}
