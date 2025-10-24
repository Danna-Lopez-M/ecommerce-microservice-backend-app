package com.selimhorri.app.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ServiceDiscoveryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Integration Test 1: Service discovery registers all microservices")
    void testServiceDiscoveryRegistersAllMicroservices() {
        // Step 1: Check if all services are registered
        ResponseEntity<String> servicesResponse = restTemplate.getForEntity(
            "/api/discovery/services", String.class
        );
        
        assertEquals(HttpStatus.OK, servicesResponse.getStatusCode());
        assertNotNull(servicesResponse.getBody());
        
        // Step 2: Verify specific services are registered
        String services = servicesResponse.getBody();
        assertTrue(services.contains("user-service"));
        assertTrue(services.contains("product-service"));
        assertTrue(services.contains("order-service"));
        assertTrue(services.contains("payment-service"));
        assertTrue(services.contains("shipping-service"));
        assertTrue(services.contains("proxy-client"));
    }

    @Test
    @DisplayName("Integration Test 2: Service discovery handles service health checks")
    void testServiceDiscoveryHandlesHealthChecks() {
        // Step 1: Check service health
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            "/api/discovery/health", String.class
        );
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
        
        // Step 2: Check individual service health
        ResponseEntity<String> userServiceHealthResponse = restTemplate.getForEntity(
            "/api/discovery/services/user-service/health", String.class
        );
        
        assertEquals(HttpStatus.OK, userServiceHealthResponse.getStatusCode());
        assertTrue(userServiceHealthResponse.getBody().contains("UP"));
    }

    @Test
    @DisplayName("Integration Test 3: Service discovery handles service failures")
    void testServiceDiscoveryHandlesServiceFailures() {
        // Step 1: Simulate service failure
        ResponseEntity<String> failureResponse = restTemplate.getForEntity(
            "/api/discovery/services/non-existent-service/health", String.class
        );
        
        // Should handle gracefully
        assertTrue(failureResponse.getStatusCode().is4xxClientError() || 
                  failureResponse.getStatusCode().is5xxServerError());
    }

    @Test
    @DisplayName("Integration Test 4: Service discovery provides service metadata")
    void testServiceDiscoveryProvidesServiceMetadata() {
        // Step 1: Get service metadata
        ResponseEntity<String> metadataResponse = restTemplate.getForEntity(
            "/api/discovery/services/user-service/metadata", String.class
        );
        
        assertEquals(HttpStatus.OK, metadataResponse.getStatusCode());
        assertNotNull(metadataResponse.getBody());
        
        // Step 2: Verify metadata contains expected information
        String metadata = metadataResponse.getBody();
        assertTrue(metadata.contains("version"));
        assertTrue(metadata.contains("endpoints"));
    }

    @Test
    @DisplayName("Integration Test 5: Service discovery handles service updates")
    void testServiceDiscoveryHandlesServiceUpdates() {
        // Step 1: Register a new service
        String newServiceJson = """
            {
                "serviceName": "test-service",
                "host": "localhost",
                "port": 8080,
                "status": "UP"
            }
            """;
        
        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
            "/api/discovery/services/register", newServiceJson, String.class
        );
        
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        
        // Step 2: Verify service is registered
        ResponseEntity<String> servicesResponse = restTemplate.getForEntity(
            "/api/discovery/services", String.class
        );
        
        assertEquals(HttpStatus.OK, servicesResponse.getStatusCode());
        assertTrue(servicesResponse.getBody().contains("test-service"));
        
        // Step 3: Update service status
        String updateServiceJson = """
            {
                "serviceName": "test-service",
                "status": "DOWN"
            }
            """;
        
        ResponseEntity<String> updateResponse = restTemplate.putForEntity(
            "/api/discovery/services/test-service", updateServiceJson, String.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        
        // Step 4: Verify service status updated
        ResponseEntity<String> updatedServiceResponse = restTemplate.getForEntity(
            "/api/discovery/services/test-service/health", String.class
        );
        
        assertEquals(HttpStatus.OK, updatedServiceResponse.getStatusCode());
        assertTrue(updatedServiceResponse.getBody().contains("DOWN"));
    }
}
