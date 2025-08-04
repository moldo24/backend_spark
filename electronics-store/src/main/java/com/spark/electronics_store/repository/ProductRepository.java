package com.spark.electronics_store.repository;

import com.spark.electronics_store.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByBrandIdAndDeletedFalse(UUID brandId);
    Optional<Product> findBySlug(String slug);
}
