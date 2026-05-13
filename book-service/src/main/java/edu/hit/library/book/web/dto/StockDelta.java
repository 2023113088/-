package edu.hit.library.book.web.dto;

import jakarta.validation.constraints.NotNull;

public record StockDelta(@NotNull Integer delta) {}
