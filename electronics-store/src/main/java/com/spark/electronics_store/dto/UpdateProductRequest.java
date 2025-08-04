package com.spark.electronics_store.dto;

import com.spark.electronics_store.model.ProductStatus;

public record UpdateProductRequest(
        String name,
        String description,
        String price,
        String currency,
        ProductStatus status
) {}