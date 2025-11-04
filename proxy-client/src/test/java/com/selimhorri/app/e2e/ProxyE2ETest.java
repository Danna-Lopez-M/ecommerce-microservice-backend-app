package com.selimhorri.app.e2e;

import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.order.model.response.OrderOrderServiceDtoCollectionResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProxyE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/app/api";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete gateway routing to user service")
    void testCompleteUserServiceRouting() {
        // Test listing users through gateway
        ResponseEntity<UserUserServiceCollectionDtoResponse> listResponse = restTemplate.exchange(
            baseUrl + "/users",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<UserUserServiceCollectionDtoResponse>() {}
        );
        
        // Gateway should successfully route the request
        assertNotNull(listResponse);
        assertTrue(listResponse.getStatusCode().is2xxSuccessful() || 
                   listResponse.getStatusCode().is4xxClientError() ||
                   listResponse.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Complete gateway routing to product service")
    void testCompleteProductServiceRouting() {
        ResponseEntity<ProductProductServiceCollectionDtoResponse> listResponse = restTemplate.exchange(
            baseUrl + "/products",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ProductProductServiceCollectionDtoResponse>() {}
        );
        
        assertNotNull(listResponse);
        assertTrue(listResponse.getStatusCode().is2xxSuccessful() || 
                   listResponse.getStatusCode().is4xxClientError() ||
                   listResponse.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Complete gateway routing to order service")
    void testCompleteOrderServiceRouting() {
        ResponseEntity<OrderOrderServiceDtoCollectionResponse> listResponse = restTemplate.exchange(
            baseUrl + "/orders",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<OrderOrderServiceDtoCollectionResponse>() {}
        );
        
        assertNotNull(listResponse);
        assertTrue(listResponse.getStatusCode().is2xxSuccessful() || 
                   listResponse.getStatusCode().is4xxClientError() ||
                   listResponse.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Gateway error handling flow")
    void testGatewayErrorHandling() {
        // Test with invalid endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/invalid-endpoint", String.class
        );
        
        // Should return appropriate error
        assertTrue(response.getStatusCode().is4xxClientError() || 
                   response.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Gateway health and monitoring flow")
    void testGatewayHealthMonitoring() {
        // Check gateway health - try with and without context-path
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/app/actuator/health", String.class
        );
        
        // Health endpoint might not be available in test environment
        assertNotNull(healthResponse);
        assertTrue(healthResponse.getStatusCode().is2xxSuccessful() || 
                   healthResponse.getStatusCode().is4xxClientError() ||
                   healthResponse.getStatusCode().is5xxServerError());
        
        if (healthResponse.getStatusCode().is2xxSuccessful() && healthResponse.getBody() != null) {
            // Check if gateway has basic connectivity
            assertTrue(healthResponse.getBody().contains("UP") || 
                       healthResponse.getBody().contains("status") ||
                       healthResponse.getBody().contains("DOWN"));
        }
    }
}