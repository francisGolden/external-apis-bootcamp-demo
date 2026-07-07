package com.accenture.externalapis.demo.dto;

public record BookDto(
        Long id,
        String title,
        String author,
        String genre,
        double price
) {
    public static BookDto fallback(){
        return new BookDto(Long.getLong("id"), "title", "author", "genre", 0.0);
    }
}