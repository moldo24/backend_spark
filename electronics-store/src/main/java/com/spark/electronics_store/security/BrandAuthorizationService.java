package com.spark.electronics_store.security;

import com.spark.electronics_store.model.Role;
import com.spark.electronics_store.model.UserSync;
import com.spark.electronics_store.repository.UserSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BrandAuthorizationService {

    private final UserSyncRepository userSyncRepository;

    public UserSync requireUserSync(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        String email = authentication.getName().toLowerCase();
        return userSyncRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Synced user not found"));
    }

    public void requireAdmin(Authentication authentication) {
        UserSync user = requireUserSync(authentication);
        if (user.isDeleted() || user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
    }

    public void requireBrandSellerForBrand(UUID brandId, Authentication authentication) {
        UserSync user = requireUserSync(authentication);
        if (user.isDeleted()) {
            throw new AccessDeniedException("User deleted");
        }
        if (user.getRole() != Role.BRAND_SELLER) {
            throw new AccessDeniedException("Only brand sellers can operate on products");
        }
        if (user.getBrand() == null || !user.getBrand().getId().equals(brandId)) {
            throw new AccessDeniedException("Brand seller not assigned to this brand");
        }
    }

    // (Optional) if you ever need a relaxed access that allows admin OR assigned seller:
    public void authorizeBrandAccess(UUID brandId, Authentication authentication) {
        UserSync user = requireUserSync(authentication);
        if (user.isDeleted()) throw new AccessDeniedException("User deleted");
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.BRAND_SELLER && user.getBrand() != null
                && user.getBrand().getId().equals(brandId)) return;
        throw new AccessDeniedException("Not authorized for brand");
    }
}
