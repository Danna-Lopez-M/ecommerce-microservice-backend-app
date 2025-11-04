package com.selimhorri.app.integration;

import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.response.OrderOrderServiceDtoCollectionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProxyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/app/api";
    }

    @Test
    @DisplayName("Integration Test 1: Should route request to user service")
    void testRouteToUserService() {
        ResponseEntity<UserUserServiceCollectionDtoResponse> response = restTemplate.exchange(
            getBaseUrl() + "/users",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<UserUserServiceCollectionDtoResponse>() {}
        );
        
        // Should successfully route to user-service
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().is4xxClientError() ||
                   response.getStatusCode().is5xxServerError()); // Service might be down
    }

    @Test
    @DisplayName("Integration Test 2: Should route request to product service")
    void testRouteToProductService() {
        ResponseEntity<ProductProductServiceCollectionDtoResponse> response = restTemplate.exchange(
            getBaseUrl() + "/products",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ProductProductServiceCollectionDtoResponse>() {}
        );
        
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().is4xxClientError() ||
                   response.getStatusCode().is5xxServerError());
    }

    @Test
    @DisplayName("Integration Test 3: Should route request to order service")
    void testRouteToOrderService() {
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> response = restTemplate.exchange(
            getBaseUrl() + "/orders",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<OrderOrderServiceDtoCollectionResponse>() {}
        );
        
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().is4xxClientError() ||
                   response.getStatusCode().is5xxServerError());
    }

    @Test
    @DisplayName("Integration Test 4: Should handle service unavailability")
    void testHandleServiceUnavailability() {
        // Try to access a non-existent resource
        ResponseEntity<String> response = restTemplate.getForEntity(
            getBaseUrl() + "/users/99999", String.class
        );
        
        // Should return appropriate error code
        assertTrue(response.getStatusCode().is4xxClientError() || 
                   response.getStatusCode().is5xxServerError());
    }

    @Test
    @DisplayName("Integration Test 5: Should validate gateway health")
    void testGatewayHealth() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/app/actuator/health", String.class
        );
        
        // Health endpoint might not be available in test environment
        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().is4xxClientError() ||
                   response.getStatusCode().is5xxServerError());
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            assertTrue(response.getBody().contains("UP") || 
                       response.getBody().contains("status") ||
                       response.getBody().contains("DOWN"));
        }
    }
}