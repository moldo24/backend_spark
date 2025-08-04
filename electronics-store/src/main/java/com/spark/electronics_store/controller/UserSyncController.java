package com.spark.electronics_store.controller;

import com.spark.electronics_store.dto.BrandSummaryResponse;
import com.spark.electronics_store.dto.UserSyncDto;
import com.spark.electronics_store.dto.UserSyncWithBrandResponse;
import com.spark.electronics_store.model.Brand;
import com.spark.electronics_store.model.Role;
import com.spark.electronics_store.model.UserSync;
import com.spark.electronics_store.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/internal/sync/users")
@RequiredArgsConstructor
public class UserSyncController {

    private final UserSyncService userSyncService;

    // Hardcoded internal secret (must match what user management sends)
    private static final String SHARED_SECRET = "moldo"; // <- hardcoded

    private boolean authorized(String header) {
        if (header == null) return false;
        String token = header.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }
        return "moldo".equals(token);
    }

    @PostMapping
    public ResponseEntity<Void> upsert(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody UserSyncDto dto) {

        String normalized = (auth == null) ? null : auth.trim();
        String token = null;
        if (normalized != null && normalized.toLowerCase().startsWith("bearer ")) {
            token = normalized.substring(7).trim();
        } else if (normalized != null) {
            token = normalized;
        }
        System.out.printf("Sync upsert called. Raw header=[%s], normalized token=[%s]%n", auth, token);

        if (!"moldo".equals(token)) {
            System.out.printf("Authorization failed: expected 'moldo' got [%s]%n", token);
            return ResponseEntity.status(401).build();
        }

        userSyncService.upsert(dto);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable UUID id) {

        if (!authorized(auth)) {
            return ResponseEntity.status(401).build();
        }

        userSyncService.markDeleted(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}")
    public ResponseEntity<UserSyncWithBrandResponse> get(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable UUID id) {

        if (!authorized(auth)) {
            return ResponseEntity.status(401).build();
        }

        return userSyncService.get(id)
                .map(u -> ResponseEntity.ok(UserSyncWithBrandResponse.from(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


}
