package edu.hit.library.book.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String author,
        @Size(max = 32) String isbn,
        @Size(max = 120) String publisher,
        @Size(max = 80) String category,
        @Min(0) int stock,
        @Min(0) double price) {}
