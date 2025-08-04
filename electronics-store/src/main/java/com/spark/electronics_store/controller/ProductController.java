package com.spark.electronics_store.controller;

import com.spark.electronics_store.dto.CreateProductRequest;
import com.spark.electronics_store.model.Product;
import com.spark.electronics_store.model.ProductStatus;
import com.spark.electronics_store.model.Brand;
import com.spark.electronics_store.repository.BrandRepository;
import com.spark.electronics_store.repository.ProductRepository;
import com.spark.electronics_store.security.BrandAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/brands/{brandId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandAuthorizationService authService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@PathVariable UUID brandId,
                                                 @RequestBody CreateProductRequest req,
                                                 Authentication authentication) {

        authService.requireBrandSellerForBrand(brandId, authentication);

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .name(req.name())
                .slug(req.slug())
                .description(req.description())
                .brand(brand)
                .price(new java.math.BigDecimal(req.price()))
                .currency(req.currency())
                .build();

        productRepository.save(product);
        return ResponseEntity.status(201).body(product);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID brandId,
                                                 @PathVariable UUID productId,
                                                 @RequestBody CreateProductRequest req,
                                                 Authentication authentication) {

        authService.requireBrandSellerForBrand(brandId, authentication);

        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!existing.getBrand().getId().equals(brandId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand mismatch");
        }

        existing.setName(req.name());
        existing.setSlug(req.slug());
        existing.setDescription(req.description());
        existing.setPrice(new java.math.BigDecimal(req.price()));
        existing.setCurrency(req.currency());
        productRepository.save(existing);
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID brandId,
                                              @PathVariable UUID productId,
                                              Authentication authentication) {

        authService.requireBrandSellerForBrand(brandId, authentication);

        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (!existing.getBrand().getId().equals(brandId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brand mismatch");
        }

        existing.setDeleted(true);
        productRepository.save(existing);
        return ResponseEntity.noContent().build();
    }
}
