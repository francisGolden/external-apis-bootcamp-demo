package com.accenture.externalapis.demo.dto;

public record BookDto(
        Long id,
        String title,
        String author,
        String genre,
        double price) {

}