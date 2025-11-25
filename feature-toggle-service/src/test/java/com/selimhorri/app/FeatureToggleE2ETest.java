package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.FeatureToggle;
import com.selimhorri.app.repository.FeatureToggleRepository;

import java.util.Map;

/**
 * End-to-End tests for Feature Toggle Pattern
 * Tests complete feature toggle workflow
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FeatureToggleE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FeatureToggleRepository repository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/features";
        repository.deleteAll();
    }

    @Test
    void testCompleteFeatureToggleWorkflow() {
        // Step 1: Create a new feature toggle
        FeatureToggle newFeature = FeatureToggle.builder()
                .name("e2e-test-feature")
                .enabled(false)
                .description("E2E test feature")
                .environment("dev")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FeatureToggle> createRequest = new HttpEntity<>(newFeature, headers);

        ResponseEntity<FeatureToggle> createResponse = restTemplate.postForEntity(
                baseUrl, createRequest, FeatureToggle.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Long featureId = createResponse.getBody().getId();

        // Step 2: Verify feature is disabled
        ResponseEntity<Map> checkResponse = restTemplate.getForEntity(
                baseUrl + "/check/e2e-test-feature?environment=dev", Map.class);

        assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
        assertFalse((Boolean) checkResponse.getBody().get("enabled"));

        // Step 3: Enable the feature
        ResponseEntity<Void> enableResponse = restTemplate.exchange(
                baseUrl + "/e2e-test-feature/enable",
                HttpMethod.PUT,
                null,
                Void.class);

        assertEquals(HttpStatus.OK, enableResponse.getStatusCode());

        // Step 4: Verify feature is now enabled
        ResponseEntity<Map> checkEnabledResponse = restTemplate.getForEntity(
                baseUrl + "/check/e2e-test-feature?environment=dev", Map.class);

        assertTrue((Boolean) checkEnabledResponse.getBody().get("enabled"));

        // Step 5: Disable the feature
        ResponseEntity<Void> disableResponse = restTemplate.exchange(
                baseUrl + "/e2e-test-feature/disable",
                HttpMethod.PUT,
                null,
                Void.class);

        assertEquals(HttpStatus.OK, disableResponse.getStatusCode());

        // Step 6: Verify feature is disabled again
        ResponseEntity<Map> checkDisabledResponse = restTemplate.getForEntity(
                baseUrl + "/check/e2e-test-feature?environment=dev", Map.class);

        assertFalse((Boolean) checkDisabledResponse.getBody().get("enabled"));

        // Step 7: Delete the feature
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + featureId,
                HttpMethod.DELETE,
                null,
                Void.class);

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Step 8: Verify feature no longer exists
        ResponseEntity<Map> checkDeletedResponse = restTemplate.getForEntity(
                baseUrl + "/check/e2e-test-feature?environment=dev", Map.class);

        assertFalse((Boolean) checkDeletedResponse.getBody().get("enabled"));
    }

    @Test
    void testMultipleEnvironments() {
        // Create same feature in different environments
        FeatureToggle devFeature = FeatureToggle.builder()
                .name("multi-env-feature")
                .enabled(true)
                .environment("dev")
                .build();

        FeatureToggle prodFeature = FeatureToggle.builder()
                .name("multi-env-feature")
                .enabled(false)
                .environment("prod")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.postForEntity(baseUrl, new HttpEntity<>(devFeature, headers), FeatureToggle.class);
        restTemplate.postForEntity(baseUrl, new HttpEntity<>(prodFeature, headers), FeatureToggle.class);

        // Verify dev is enabled
        ResponseEntity<Map> devResponse = restTemplate.getForEntity(
                baseUrl + "/check/multi-env-feature?environment=dev", Map.class);
        assertTrue((Boolean) devResponse.getBody().get("enabled"));

        // Verify prod is disabled
        ResponseEntity<Map> prodResponse = restTemplate.getForEntity(
                baseUrl + "/check/multi-env-feature?environment=prod", Map.class);
        assertFalse((Boolean) prodResponse.getBody().get("enabled"));
    }

    @Test
    void testCachingBehavior() {
        // Create and enable a feature
        FeatureToggle feature = FeatureToggle.builder()
                .name("cached-feature")
                .enabled(true)
                .environment("dev")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(baseUrl, new HttpEntity<>(feature, headers), FeatureToggle.class);

        // First check - should hit database
        ResponseEntity<Map> response1 = restTemplate.getForEntity(
                baseUrl + "/check/cached-feature?environment=dev", Map.class);
        assertTrue((Boolean) response1.getBody().get("enabled"));

        // Second check - should use cache (same result)
        ResponseEntity<Map> response2 = restTemplate.getForEntity(
                baseUrl + "/check/cached-feature?environment=dev", Map.class);
        assertTrue((Boolean) response2.getBody().get("enabled"));

        // Disable feature - should clear cache
        restTemplate.exchange(baseUrl + "/cached-feature/disable", HttpMethod.PUT, null, Void.class);

        // Check again - should reflect new state
        ResponseEntity<Map> response3 = restTemplate.getForEntity(
                baseUrl + "/check/cached-feature?environment=dev", Map.class);
        assertFalse((Boolean) response3.getBody().get("enabled"));
    }
}
