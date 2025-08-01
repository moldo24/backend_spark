package com.spark.demo;

import com.spark.demo.model.AuthProvider;
import com.spark.demo.model.Role;
import com.spark.demo.model.User;
import com.spark.demo.repository.UserRepository;
import com.spark.demo.security.jwt.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
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
	public CommandLineRunner initOnStartup(UserRepository userRepository, PasswordEncoder passwordEncoder) {
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

			// 2. Ensure admin user exists
			String adminEmail = "admin@admin.com";
			if (userRepository.findByEmail(adminEmail).isEmpty()) {
				User admin = new User();
				admin.setName("admin");
				admin.setEmail(adminEmail);
				admin.setProvider(AuthProvider.LOCAL);
				admin.setPassword(passwordEncoder.encode("admin"));
				admin.setRole(Role.ADMIN);
				admin.setEmailVerified(true);
				// no image/icon assigned (null)
				userRepository.save(admin);
				System.out.println("Created default admin user: admin@admin.com / password 'admin'");
			} else {
				System.out.println("Admin user already exists, skipping creation.");
			}
		};
	}
}
