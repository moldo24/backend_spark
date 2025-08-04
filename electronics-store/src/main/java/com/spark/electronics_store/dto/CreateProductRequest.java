package com.spark.electronics_store.dto;
public record CreateProductRequest(
        String name,
        String slug,
        String description,
        String price,
        String currency
) {}