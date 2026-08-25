package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class GlobusTokenRefreshService {
    @Value("${globus.clientId}")
    private String clientId;

    @Value("${globus.clientSecret}")
    private String clientSecret;

    @Value("${globus.refreshToken}")
    private String refreshToken;

    @Value("${globus.token.endpoint}")
    private String tokenEndpoint;

    private String accessToken;

    // Support concurrent reads that are albeit locked by writes
    private final ReadWriteLock accessTokenLock = new ReentrantReadWriteLock();

    public String getAccessToken() {
        accessTokenLock.readLock().lock();
        try {
            if (!Objects.isNull(accessToken) && !accessToken.isEmpty()) {
                return accessToken;
            }
        } finally {
            accessTokenLock.readLock().unlock();
        }

        // Refresh outside the read lock so the write lock can be acquired safely.
        refreshToken();

        accessTokenLock.readLock().lock();
        try {
            return accessToken;
        } finally {
            accessTokenLock.readLock().unlock();
        }
    }

    private void setAccessToken(String newAccessToken) {
        accessTokenLock.writeLock().lock();
        try {
            accessToken = newAccessToken;
        } finally {
            accessTokenLock.writeLock().unlock();
        }
    }

    // Default expiration for Globus tokens is 2 days i.e., 48 hours
    // Out of abundance of caution, we begin the token refresh process 1 hour ahead of this
    @Scheduled(fixedDelay = 47 * 60 * 60 * 1000)
    public void refreshToken() {
        // Create a RestTemplate instance
        RestTemplate restTemplate = new RestTemplate();

        // Prepare the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        // Prepare the request body
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "refresh_token");
        requestBody.add("refresh_token", refreshToken);

        // Create the HTTP entity with headers and body
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Make the POST request to the token endpoint
        ResponseEntity<String> response = restTemplate.exchange(tokenEndpoint, HttpMethod.POST, requestEntity, String.class);

        // Parse the response JSON to extract the new access token
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            accessTokenLock.writeLock().lock();
            try {
                accessToken = responseJson.get("access_token").asText();
            } finally {
                accessTokenLock.writeLock().unlock();
            }
        } catch (Exception e) {
            // Handle errors while parsing the response JSON
            e.printStackTrace();
        }
    }
}
