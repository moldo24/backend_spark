package com.spark.electronics_store.repository;

import com.spark.electronics_store.model.BrandRequest;
import com.spark.electronics_store.model.BrandRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRequestRepository extends JpaRepository<BrandRequest, UUID> {
    Optional<BrandRequest> findFirstBySlugIgnoreCaseAndStatus(String slug, BrandRequestStatus status);
    Optional<BrandRequest> findFirstByNameIgnoreCaseAndStatus(String name, BrandRequestStatus status);
    List<BrandRequest> findByStatus(BrandRequestStatus status);
}
