package edu.hit.library.book.web.dto;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String publisher,
        String category,
        int stock,
        double price) {}
