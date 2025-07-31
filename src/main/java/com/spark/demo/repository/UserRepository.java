package com.spark.demo.repository;

import com.spark.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(com.spark.demo.model.AuthProvider provider, String providerId);
}
