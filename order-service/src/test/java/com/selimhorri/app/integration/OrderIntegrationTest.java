package com.selimhorri.app.integration;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/order-service/api/orders";
    }

    @Test
    @DisplayName("Integration Test 1: Should create and retrieve order")
    void testCreateAndRetrieveOrder() {
        OrderDto orderDto = createTestOrder(1, 199.99);
        
        ResponseEntity<OrderDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer orderId = createResponse.getBody().getOrderId();
        
        ResponseEntity<OrderDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + orderId, OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(199.99, getResponse.getBody().getOrderFee(), 0.01);
    }

    @Test
    @DisplayName("Integration Test 2: Should update order details")
    void testUpdateOrder() {
        OrderDto orderDto = createTestOrder(2, 299.99);
        ResponseEntity<OrderDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), orderDto, OrderDto.class
        );
        
        OrderDto createdOrder = createResponse.getBody();
        createdOrder.setOrderDesc("Updated Order Description");
        createdOrder.setOrderFee(349.99);
        
        restTemplate.put(getBaseUrl(), createdOrder);
        
        ResponseEntity<OrderDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + createdOrder.getOrderId(), OrderDto.class
        );
        
        assertEquals("Updated Order Description", getResponse.getBody().getOrderDesc());
        assertEquals(349.99, getResponse.getBody().getOrderFee(), 0.01);
    }

    @Test
    @DisplayName("Integration Test 3: Should delete order")
    void testDeleteOrder() {
        OrderDto orderDto = createTestOrder(3, 99.99);
        ResponseEntity<OrderDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), orderDto, OrderDto.class
        );
        
        Integer orderId = createResponse.getBody().getOrderId();
        
        restTemplate.delete(getBaseUrl() + "/" + orderId);
        
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + orderId, String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("Integration Test 4: Should list all orders")
    void testListAllOrders() {
        ResponseEntity<DtoCollectionResponse<OrderDto>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<OrderDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
    }

    @Test
    @DisplayName("Integration Test 5: Should validate order with cart")
    void testOrderCartAssociation() {
        OrderDto orderDto = createTestOrder(5, 499.99);
        
        ResponseEntity<OrderDto> response = restTemplate.postForEntity(
            getBaseUrl(), orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getCartDto());
        assertEquals(5, response.getBody().getCartDto().getUserId());
    }

    private OrderDto createTestOrder(Integer userId, Double orderFee) {
        CartDto cartDto = new CartDto();
        cartDto.setCartId(userId);
        cartDto.setUserId(userId);
        
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderDate(LocalDateTime.now());
        orderDto.setOrderDesc("Integration Test Order");
        orderDto.setOrderFee(orderFee);
        orderDto.setCartDto(cartDto);
        
        return orderDto;
    }
}