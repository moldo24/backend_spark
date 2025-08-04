package com.spark.demo.service;

import com.spark.demo.dto.UserSyncDto;
import com.spark.demo.model.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSyncNotifier {

    private static final Logger log = LoggerFactory.getLogger(UserSyncNotifier.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sync.store-base-url}")
    private String storeBaseUrl;

    @Value("${sync.shared-secret}")
    private String sharedSecret;

    @Retryable(value = RestClientException.class, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 20000))
    public void notifyUpsert(User user) {
        UserSyncDto dto = new UserSyncDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getTokenVersion(),
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sharedSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserSyncDto> req = new HttpEntity<>(dto, headers);

        String url = storeBaseUrl + "/internal/sync/users";
        restTemplate.postForEntity(url, req, Void.class);
        log.debug("Synced user upsert to store service: {}", user.getId());
    }

    @Recover
    public void recover(RestClientException ex, User user) {
        log.error("Failed to sync upsert for user {} after retries", user.getId(), ex);
        // Optionally enqueue to a durable retry queue or alert.
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 20000))
    public void notifyDelete(UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sharedSecret);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        String url = storeBaseUrl + "/internal/sync/users/" + userId;
        restTemplate.exchange(url, HttpMethod.DELETE, req, Void.class);
        log.debug("Synced user deletion to store service: {}", userId);
    }

    @Recover
    public void recoverDelete(RestClientException ex, UUID userId) {
        log.error("Failed to sync delete for user {} after retries", userId, ex);
    }
}
