package com.spark.electronics_store.service;
import com.spark.electronics_store.dto.UserSyncDto;
import com.spark.electronics_store.model.Role;
import com.spark.electronics_store.model.UserSync;
import com.spark.electronics_store.repository.UserSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSyncService {
    private final UserSyncRepository repository;

    public void upsert(UserSyncDto dto) {
        UserSync u = repository.findById(dto.id())
                .orElse(UserSync.builder().id(dto.id()).build());

        u.setEmail(dto.email());
        u.setName(dto.name());
        u.setRole(Role.valueOf(dto.role()));
        u.setTokenVersion(dto.tokenVersion());
        u.setDeleted(dto.deleted());
        repository.save(u);
    }
    public Optional<UserSync> getWithBrand(UUID id) {
        return repository.findWithBrandById(id);
    }
    public void markDeleted(UUID id) {
        repository.findById(id).ifPresent(u -> {
            u.setDeleted(true);
            repository.save(u);
        });

    }

    public Optional<UserSync> get(UUID id) {
        return repository.findById(id);
    }
}
