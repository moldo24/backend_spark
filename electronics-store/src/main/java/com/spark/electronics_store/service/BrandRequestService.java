package com.spark.electronics_store.service;

import com.spark.electronics_store.dto.BrandRequestCreateDto;
import com.spark.electronics_store.dto.BrandRequestResponse;
import com.spark.electronics_store.model.Brand;
import com.spark.electronics_store.model.BrandRequest;
import com.spark.electronics_store.model.BrandRequestStatus;
import com.spark.electronics_store.model.Role;
import com.spark.electronics_store.repository.BrandRepository;
import com.spark.electronics_store.repository.BrandRequestRepository;
import com.spark.electronics_store.repository.UserSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class BrandRequestService {

    private final BrandRequestRepository brandRequestRepository;
    private final BrandRepository brandRepository;
    private final BrandService brandService;
    private final UserSyncRepository userSyncRepository;
    public BrandRequest submitRequest(BrandRequestCreateDto dto, UUID applicantId) {
        String normalizedSlug = dto.getSlug().trim().toLowerCase();
        String normalizedName = dto.getName().trim();

        // duplicate against existing brand
        if (brandRepository.existsBySlugIgnoreCase(normalizedSlug) || brandRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(CONFLICT, "Brand with that name or slug already exists");
        }

        // duplicate pending request
        if (brandRequestRepository.findFirstBySlugIgnoreCaseAndStatus(normalizedSlug, BrandRequestStatus.PENDING).isPresent()
                || brandRequestRepository.findFirstByNameIgnoreCaseAndStatus(normalizedName, BrandRequestStatus.PENDING).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "A pending request with that name or slug already exists");
        }

        BrandRequest req = BrandRequest.builder()
                .name(normalizedName)
                .slug(normalizedSlug)
                .logoUrl(dto.getLogoUrl())
                .status(BrandRequestStatus.PENDING)
                .applicantId(applicantId)
                .build();

        return brandRequestRepository.save(req);
    }

    public java.util.List<BrandRequest> list(Optional<BrandRequestStatus> statusOpt) {
        if (statusOpt.isPresent()) {
            return brandRequestRepository.findByStatus(statusOpt.get());
        }
        return brandRequestRepository.findAll();
    }

    public BrandRequest approve(UUID requestId, String adminIdentifier) {
        BrandRequest req = brandRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Request not found"));

        if (req.getStatus() != BrandRequestStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot approve a non-pending request");
        }

        if (brandRepository.existsBySlugIgnoreCase(req.getSlug()) ||
                brandRepository.existsByNameIgnoreCase(req.getName())) {
            throw new ResponseStatusException(CONFLICT, "Brand already exists (possibly created concurrently)");
        }

        // Step 1: create brand
        Brand created = brandService.createBrand(req.getName(), req.getSlug(), req.getLogoUrl());

        // Step 2: update user (assign brand + change role)
        if (req.getApplicantId() != null) {
            userSyncRepository.findById(req.getApplicantId()).ifPresent(user -> {
                user.setBrand(created);
                user.setRole(Role.BRAND_SELLER); // You can make this conditional if needed
                userSyncRepository.save(user);
            });
        }

        // Step 3: update brand request
        req.setStatus(BrandRequestStatus.APPROVED);
        req.setReviewedBy(adminIdentifier);
        req.setApprovedBrandId(created.getId());
        return brandRequestRepository.save(req);
    }


    public BrandRequest reject(UUID requestId, String reason, String adminIdentifier) {
        BrandRequest req = brandRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Request not found"));

        if (req.getStatus() != BrandRequestStatus.PENDING) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot reject a non-pending request");
        }

        req.setStatus(BrandRequestStatus.REJECTED);
        req.setReason(reason);
        req.setReviewedBy(adminIdentifier);
        return brandRequestRepository.save(req);
    }
}
