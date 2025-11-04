package com.selimhorri.app.integration;

import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.domain.id.OrderItemId;
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
class OrderItemIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/shipping-service/api/shippings";
    }

    @Test
    @DisplayName("Integration Test 1: Should create and retrieve order item")
    void testCreateAndRetrieveOrderItem() {
        OrderItemDto orderItemDto = createTestOrderItem(1, 1, 5);
        
        ResponseEntity<OrderItemDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), orderItemDto, OrderItemDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals(5, createResponse.getBody().getOrderedQuantity());
    }

    @Test
    @DisplayName("Integration Test 2: Should update order item quantity")
    void testUpdateOrderItem() {
        OrderItemDto orderItemDto = createTestOrderItem(2, 2, 3);
        ResponseEntity<OrderItemDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), orderItemDto, OrderItemDto.class
        );
        
        OrderItemDto createdOrderItem = createResponse.getBody();
        createdOrderItem.setOrderedQuantity(10);
        
        restTemplate.put(getBaseUrl(), createdOrderItem);
        
        OrderItemId id = new OrderItemId(2, 2);
        ResponseEntity<OrderItemDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + id.getOrderId() + "/" + id.getProductId(), 
            OrderItemDto.class
        );
        
        assertEquals(10, getResponse.getBody().getOrderedQuantity());
    }

    @Test
    @DisplayName("Integration Test 3: Should delete order item")
    void testDeleteOrderItem() {
        OrderItemDto orderItemDto = createTestOrderItem(3, 3, 2);
        restTemplate.postForEntity(getBaseUrl(), orderItemDto, OrderItemDto.class);
        
        OrderItemId id = new OrderItemId(3, 3);
        restTemplate.delete(getBaseUrl() + "/" + id.getOrderId() + "/" + id.getProductId());
        
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + id.getOrderId() + "/" + id.getProductId(),
            String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("Integration Test 4: Should list all order items")
    void testListAllOrderItems() {
        ResponseEntity<DtoCollectionResponse<OrderItemDto>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<OrderItemDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
    }

    @Test
    @DisplayName("Integration Test 5: Should validate order item associations")
    void testOrderItemAssociations() {
        OrderItemDto orderItemDto = createTestOrderItem(5, 5, 7);
        
        ResponseEntity<OrderItemDto> response = restTemplate.postForEntity(
            getBaseUrl(), orderItemDto, OrderItemDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getProductDto());
        assertNotNull(response.getBody().getOrderDto());
        assertEquals(5, response.getBody().getProductId());
        assertEquals(5, response.getBody().getOrderId());
    }

    private OrderItemDto createTestOrderItem(Integer productId, Integer orderId, Integer quantity) {
        ProductDto productDto = new ProductDto();
        productDto.setProductId(productId);
        productDto.setProductTitle("Integration Test Product");
        productDto.setPriceUnit(99.99);
        
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(orderId);
        orderDto.setOrderDesc("Integration Test Order");
        orderDto.setOrderFee(199.99);
        
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductId(productId);
        orderItemDto.setOrderId(orderId);
        orderItemDto.setOrderedQuantity(quantity);
        orderItemDto.setProductDto(productDto);
        orderItemDto.setOrderDto(orderDto);
        
        return orderItemDto;
    }
}