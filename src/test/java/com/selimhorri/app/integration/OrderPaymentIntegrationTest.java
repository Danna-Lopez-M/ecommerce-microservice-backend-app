package com.selimhorri.app.integration;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
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
class OrderPaymentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Integration Test 1: Create order and process payment")
    void testCreateOrderAndProcessPayment() {
        // Step 1: Create Order
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(100.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        assertNotNull(orderResponse.getBody());
        Integer orderId = orderResponse.getBody().getId();
        
        // Step 2: Process Payment
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(100.0);
        paymentDto.setPaymentMethod("CREDIT_CARD");
        
        ResponseEntity<PaymentDto> paymentResponse = restTemplate.postForEntity(
            "/api/payments", paymentDto, PaymentDto.class
        );
        
        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());
        assertNotNull(paymentResponse.getBody());
        assertEquals("COMPLETED", paymentResponse.getBody().getStatus());
    }

    @Test
    @DisplayName("Integration Test 2: Order should update status after payment")
    void testOrderStatusUpdateAfterPayment() {
        // Create and pay for order
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(50.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getId();
        
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(50.0);
        
        restTemplate.postForEntity("/api/payments", paymentDto, PaymentDto.class);
        
        // Verify order status updated
        ResponseEntity<OrderDto> updatedOrder = restTemplate.getForEntity(
            "/api/orders/" + orderId, OrderDto.class
        );
        
        assertEquals("PAID", updatedOrder.getBody().getStatus());
    }

    @Test
    @DisplayName("Integration Test 3: Product inventory decreases after order")
    void testProductInventoryDecreaseAfterOrder() {
        // Get initial inventory
        Integer productId = 1;
        ResponseEntity<ProductDto> initialProduct = restTemplate.getForEntity(
            "/api/products/" + productId, ProductDto.class
        );
        Integer initialStock = initialProduct.getBody().getStock();
        
        // Create order with this product
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        OrderItemDto item = new OrderItemDto();
        item.setProductId(productId);
        item.setQuantity(2);
        orderDto.setItems(List.of(item));
        
        restTemplate.postForEntity("/api/orders", orderDto, OrderDto.class);
        
        // Verify inventory decreased
        ResponseEntity<ProductDto> updatedProduct = restTemplate.getForEntity(
            "/api/products/" + productId, ProductDto.class
        );
        Integer newStock = updatedProduct.getBody().getStock();
        
        assertEquals(initialStock - 2, newStock);
    }

    @Test
    @DisplayName("Integration Test 4: Payment failure should rollback order")
    void testPaymentFailureRollback() {
        // Create order
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(100.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getId();
        
        // Attempt payment with invalid card
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(100.0);
        paymentDto.setPaymentMethod("INVALID_CARD");
        
        ResponseEntity<PaymentDto> paymentResponse = restTemplate.postForEntity(
            "/api/payments", paymentDto, PaymentDto.class
        );
        
        // Verify order status is CANCELLED
        ResponseEntity<OrderDto> cancelledOrder = restTemplate.getForEntity(
            "/api/orders/" + orderId, OrderDto.class
        );
        
        assertEquals("CANCELLED", cancelledOrder.getBody().getStatus());
    }

    @Test
    @DisplayName("Integration Test 5: Shipping creation after successful payment")
    void testShippingCreationAfterPayment() {
        // Create order and process payment
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(75.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getId();
        
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(75.0);
        
        restTemplate.postForEntity("/api/payments", paymentDto, PaymentDto.class);
        
        // Verify shipping was created
        ResponseEntity<ShippingDto> shippingResponse = restTemplate.getForEntity(
            "/api/shipping/order/" + orderId, ShippingDto.class
        );
        
        assertEquals(HttpStatus.OK, shippingResponse.getStatusCode());
        assertNotNull(shippingResponse.getBody());
        assertEquals("PENDING", shippingResponse.getBody().getStatus());
    }
}