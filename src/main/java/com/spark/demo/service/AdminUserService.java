// src/main/java/com/spark/demo/service/AdminUserService.java
package com.spark.demo.service;

import com.spark.demo.dto.AdminUserResponse;
import com.spark.demo.dto.CreateUserRequest;
import com.spark.demo.dto.UpdateUserRequest;
import com.spark.demo.model.User;
import com.spark.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private AdminUserResponse toResponse(User u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .provider(u.getProvider())
                .providerId(u.getProviderId())
                .name(u.getName())
                .email(u.getEmail())
                .imageUrl(u.getImageUrl())
                .emailVerified(u.isEmailVerified())
                .role(u.getRole())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    public List<AdminUserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AdminUserResponse get(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(u);
    }

    public AdminUserResponse create(CreateUserRequest req) {
        String emailNorm = req.getEmail().toLowerCase();
        if (userRepository.findByEmail(emailNorm).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        if (req.getProvider() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider is required");
        }

        if (req.getProvider().name().equalsIgnoreCase("LOCAL")) {
            if (req.getPassword() == null || req.getPassword().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password required and must be >=8 chars for LOCAL user");
            }
        } else {
            if (req.getProviderId() == null || req.getProviderId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "providerId required for OAuth user");
            }
        }

        User user = User.builder()
                .name(req.getName())
                .email(emailNorm)
                .provider(req.getProvider())
                .providerId(req.getProviderId())
                .role(req.getRole())
                .emailVerified(req.isEmailVerified())
                .build();

        if (req.getProvider().name().equalsIgnoreCase("LOCAL")) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public AdminUserResponse update(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String emailNorm = req.getEmail().toLowerCase();
            if (!emailNorm.equalsIgnoreCase(user.getEmail())
                    && userRepository.findByEmail(emailNorm).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
            }
            user.setEmail(emailNorm);
        }

        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName());
        }

        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }

        if (req.getEmailVerified() != null) {
            user.setEmailVerified(req.getEmailVerified());
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (!user.getProvider().name().equalsIgnoreCase("LOCAL")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot set password for non-LOCAL user");
            }
            if (req.getPassword().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }
}
