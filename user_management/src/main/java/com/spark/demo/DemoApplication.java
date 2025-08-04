package com.spark.demo;

import com.spark.demo.model.AuthProvider;
import com.spark.demo.model.Role;
import com.spark.demo.model.User;
import com.spark.demo.repository.UserRepository;
import com.spark.demo.security.jwt.JwtTokenProvider;
import com.spark.demo.service.UserImageService;
import com.spark.demo.service.UserSyncNotifier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@SpringBootApplication
@RequiredArgsConstructor
public class DemoApplication {

	@Value("${upload.base-dir:uploads}")
	private String uploadBaseDir;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner initOnStartup(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			UserSyncNotifier userSyncNotifier // << inject notifier
	) {
		return args -> {
			// 1. Clean uploads directory (but keep the directory itself)
			Path uploads = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
			if (Files.exists(uploads) && Files.isDirectory(uploads)) {
				try (Stream<Path> walk = Files.walk(uploads)) {
					walk.sorted(Comparator.reverseOrder())
							.filter(p -> !p.equals(uploads)) // don't delete the root folder itself
							.forEach(p -> {
								try {
									Files.deleteIfExists(p);
								} catch (IOException e) {
									System.err.println("Failed to delete " + p + ": " + e.getMessage());
								}
							});
				} catch (IOException e) {
					System.err.println("Failed to clean uploads directory: " + e.getMessage());
				}
			}
			// ensure directory exists
			Files.createDirectories(uploads);
			System.out.println("Cleared uploads directory at: " + uploads);

			// 2. Ensure admin user exists and sync it
			String adminEmail = "admin@admin.com";
			Optional<User> existing = userRepository.findByEmail(adminEmail);
			User admin;
			if (existing.isEmpty()) {
				admin = new User();
				admin.setName("admin");
				admin.setEmail(adminEmail);
				admin.setProvider(AuthProvider.LOCAL);
				admin.setPassword(passwordEncoder.encode("admin"));
				admin.setRole(Role.ADMIN);
				admin.setEmailVerified(true);
				userRepository.save(admin);
				System.out.println("Created default admin user: admin@admin.com / password 'admin'");
			} else {
				admin = existing.get();
				System.out.println("Admin user already exists, skipping creation.");
			}

			// 3. Sync admin user to electronics store
			try {
				userSyncNotifier.notifyUpsert(admin);
				System.out.println("Synced admin user to electronics store");
			} catch (Exception e) {
				System.err.println("Failed to sync admin user on startup: " + e.getMessage());
				// Depending on your tolerance you could enqueue for retry or let @Retryable retry automatically
			}
		};
	}
}
