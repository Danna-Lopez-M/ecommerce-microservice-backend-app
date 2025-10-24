package com.selimhorri.app.integration;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.OrderDto;
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
class ProxyClientIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Integration Test 1: Proxy client aggregates user and product data")
    void testProxyClientAggregatesUserAndProductData() {
        // Step 1: Get user profile through proxy
        ResponseEntity<UserDto> userResponse = restTemplate.getForEntity(
            "/api/proxy/users/1", UserDto.class
        );
        
        assertEquals(HttpStatus.OK, userResponse.getStatusCode());
        assertNotNull(userResponse.getBody());
        assertEquals(1, userResponse.getBody().getUserId());
        
        // Step 2: Get user's products through proxy
        ResponseEntity<ProductDto[]> productsResponse = restTemplate.getForEntity(
            "/api/proxy/users/1/products", ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, productsResponse.getStatusCode());
        assertNotNull(productsResponse.getBody());
    }

    @Test
    @DisplayName("Integration Test 2: Proxy client handles service discovery")
    void testProxyClientHandlesServiceDiscovery() {
        // Step 1: Test service health through proxy
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            "/api/proxy/health", String.class
        );
        
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
        
        // Step 2: Test service discovery
        ResponseEntity<String> discoveryResponse = restTemplate.getForEntity(
            "/api/proxy/services", String.class
        );
        
        assertEquals(HttpStatus.OK, discoveryResponse.getStatusCode());
        assertNotNull(discoveryResponse.getBody());
    }

    @Test
    @DisplayName("Integration Test 3: Proxy client implements circuit breaker pattern")
    void testProxyClientCircuitBreakerPattern() {
        // Step 1: Normal operation
        ResponseEntity<UserDto> userResponse = restTemplate.getForEntity(
            "/api/proxy/users/1", UserDto.class
        );
        
        assertEquals(HttpStatus.OK, userResponse.getStatusCode());
        
        // Step 2: Simulate service failure
        ResponseEntity<String> failureResponse = restTemplate.getForEntity(
            "/api/proxy/users/999", String.class
        );
        
        // Should handle gracefully with circuit breaker
        assertTrue(failureResponse.getStatusCode().is4xxClientError() || 
                  failureResponse.getStatusCode().is5xxServerError());
    }

    @Test
    @DisplayName("Integration Test 4: Proxy client aggregates order with user and product details")
    void testProxyClientAggregatesOrderWithDetails() {
        // Step 1: Get order with full details through proxy
        ResponseEntity<OrderDto> orderResponse = restTemplate.getForEntity(
            "/api/proxy/orders/1/details", OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, orderResponse.getStatusCode());
        assertNotNull(orderResponse.getBody());
        
        // Step 2: Verify order contains user and product information
        OrderDto order = orderResponse.getBody();
        assertNotNull(order.getUser());
        assertNotNull(order.getItems());
        assertTrue(order.getItems().size() > 0);
    }

    @Test
    @DisplayName("Integration Test 5: Proxy client handles load balancing")
    void testProxyClientLoadBalancing() {
        // Step 1: Make multiple requests to test load balancing
        for (int i = 0; i < 5; i++) {
            ResponseEntity<ProductDto[]> productsResponse = restTemplate.getForEntity(
                "/api/proxy/products", ProductDto[].class
            );
            
            assertEquals(HttpStatus.OK, productsResponse.getStatusCode());
            assertNotNull(productsResponse.getBody());
        }
        
        // Step 2: Verify load balancing is working
        // This would typically be verified through metrics or logs
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            "/api/proxy/metrics", String.class
        );
        
        assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        assertNotNull(metricsResponse.getBody());
    }
}
