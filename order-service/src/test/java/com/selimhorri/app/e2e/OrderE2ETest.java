package com.selimhorri.app.e2e;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static Integer orderId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/order-service/api/orders";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete order creation flow")
    void testCompleteOrderCreation() {
        CartDto cartDto = new CartDto();
        cartDto.setCartId(1);
        cartDto.setUserId(1);
        
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setOrderDesc("E2E Test Order - Laptop Purchase");
        orderDto.setOrderFee(999.99);
        orderDto.setCartDto(cartDto);
        
        ResponseEntity<OrderDto> response = restTemplate.postForEntity(
            baseUrl, orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        orderId = response.getBody().getOrderId();
        assertEquals(999.99, response.getBody().getOrderFee(), 0.01);
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Order modification flow")
    void testOrderModification() {
        assertNotNull(orderId, "Order must be created first");
        
        ResponseEntity<OrderDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + orderId, OrderDto.class
        );
        
        OrderDto order = getResponse.getBody();
        order.setOrderDesc("E2E Test Order - Laptop Purchase (Updated)");
        order.setOrderFee(899.99);
        
        restTemplate.put(baseUrl, order);
        
        ResponseEntity<OrderDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + orderId, OrderDto.class
        );
        
        assertTrue(updatedResponse.getBody().getOrderDesc().contains("Updated"));
        assertEquals(899.99, updatedResponse.getBody().getOrderFee(), 0.01);
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Order tracking flow")
    void testOrderTracking() {
        ResponseEntity<OrderDto> response = restTemplate.getForEntity(
            baseUrl + "/" + orderId, OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        OrderDto order = response.getBody();
        assertNotNull(order.getOrderDate());
        assertNotNull(order.getCartDto());
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Order history retrieval flow")
    void testOrderHistoryRetrieval() {
        ResponseEntity<DtoCollectionResponse<OrderDto>> allOrdersResponse = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<OrderDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, allOrdersResponse.getStatusCode());
        assertNotNull(allOrdersResponse.getBody());
        assertNotNull(allOrdersResponse.getBody().getCollection());
        assertTrue(allOrdersResponse.getBody().getCollection().size() > 0);
        
        boolean orderFound = false;
        for (OrderDto order : allOrdersResponse.getBody().getCollection()) {
            if (order.getOrderId().equals(orderId)) {
                orderFound = true;
                break;
            }
        }
        assertTrue(orderFound, "Created order should be in the list");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Order cancellation flow")
    void testOrderCancellation() {
        restTemplate.delete(baseUrl + "/" + orderId);
        
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/" + orderId, String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}