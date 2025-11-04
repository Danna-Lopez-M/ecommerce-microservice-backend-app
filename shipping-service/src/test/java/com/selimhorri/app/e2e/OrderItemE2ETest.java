package com.selimhorri.app.e2e;

import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.domain.id.OrderItemId;
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
class OrderItemE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static Integer productId = 100;
    private static Integer orderId = 100;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/shipping-service/api/shippings";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete order item creation flow")
    void testCompleteOrderItemCreation() {
        ProductDto productDto = new ProductDto();
        productDto.setProductId(productId);
        productDto.setProductTitle("E2E Test Laptop");
        productDto.setSku("E2E-LAP-001");
        productDto.setPriceUnit(999.99);
        productDto.setQuantity(50);
        
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(orderId);
        orderDto.setOrderDesc("E2E Test Order");
        orderDto.setOrderFee(1999.98);
        
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setProductId(productId);
        orderItemDto.setOrderId(orderId);
        orderItemDto.setOrderedQuantity(2);
        orderItemDto.setProductDto(productDto);
        orderItemDto.setOrderDto(orderDto);
        
        ResponseEntity<OrderItemDto> response = restTemplate.postForEntity(
            baseUrl, orderItemDto, OrderItemDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getOrderedQuantity());
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Order item quantity update flow")
    void testOrderItemQuantityUpdate() {
        OrderItemId id = new OrderItemId(productId, orderId);
        
        ResponseEntity<OrderItemDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + id.getOrderId() + "/" + id.getProductId(),
            OrderItemDto.class
        );
        
        OrderItemDto orderItem = getResponse.getBody();
        orderItem.setOrderedQuantity(5);
        
        restTemplate.put(baseUrl, orderItem);
        
        ResponseEntity<OrderItemDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + id.getOrderId() + "/" + id.getProductId(),
            OrderItemDto.class
        );
        
        assertEquals(5, updatedResponse.getBody().getOrderedQuantity());
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Order item shipping preparation flow")
    void testOrderItemShippingPreparation() {
        OrderItemId id = new OrderItemId(productId, orderId);
        
        ResponseEntity<OrderItemDto> response = restTemplate.getForEntity(
            baseUrl + "/" + id.getOrderId() + "/" + id.getProductId(),
            OrderItemDto.class
        );
        
        OrderItemDto orderItem = response.getBody();
        assertNotNull(orderItem);
        assertNotNull(orderItem.getProductDto());
        assertNotNull(orderItem.getOrderDto());
        assertTrue(orderItem.getOrderedQuantity() > 0);
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Order items inventory tracking flow")
    void testOrderItemsInventoryTracking() {
        ResponseEntity<DtoCollectionResponse<OrderItemDto>> allOrderItemsResponse = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<OrderItemDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, allOrderItemsResponse.getStatusCode());
        assertNotNull(allOrderItemsResponse.getBody());
        assertNotNull(allOrderItemsResponse.getBody().getCollection());
        assertTrue(allOrderItemsResponse.getBody().getCollection().size() > 0);
        
        boolean itemFound = false;
        for (OrderItemDto item : allOrderItemsResponse.getBody().getCollection()) {
            if (item.getProductId().equals(productId) && item.getOrderId().equals(orderId)) {
                itemFound = true;
                assertEquals(5, item.getOrderedQuantity());
                break;
            }
        }
        assertTrue(itemFound, "Order item should be in the list");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Order item removal flow")
    void testOrderItemRemoval() {
        OrderItemId id = new OrderItemId(productId, orderId);
        
        restTemplate.delete(baseUrl + "/" + id.getOrderId() + "/" + id.getProductId());
        
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/" + id.getOrderId() + "/" + id.getProductId(),
            String.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}