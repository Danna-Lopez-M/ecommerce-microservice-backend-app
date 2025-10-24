package com.selimhorri.app.integration;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.ShippingDto;
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
class OrderPaymentShippingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Integration Test 1: Complete order to payment to shipping flow")
    void testCompleteOrderPaymentShippingFlow() {
        // Step 1: Create order
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(299.99);
        orderDto.setStatus("PENDING");
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        assertNotNull(orderResponse.getBody());
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Step 2: Process payment
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(299.99);
        paymentDto.setPaymentMethod("CREDIT_CARD");
        
        ResponseEntity<PaymentDto> paymentResponse = restTemplate.postForEntity(
            "/api/payments/process", paymentDto, PaymentDto.class
        );
        
        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());
        assertNotNull(paymentResponse.getBody());
        assertEquals("COMPLETED", paymentResponse.getBody().getStatus());
        
        // Step 3: Verify order status updated
        ResponseEntity<OrderDto> updatedOrderResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId, OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, updatedOrderResponse.getStatusCode());
        assertEquals("PAID", updatedOrderResponse.getBody().getStatus());
        
        // Step 4: Create shipping
        ShippingDto shippingDto = new ShippingDto();
        shippingDto.setOrderId(orderId);
        shippingDto.setAddress("123 Main St, City, State 12345");
        shippingDto.setStatus("PENDING");
        
        ResponseEntity<ShippingDto> shippingResponse = restTemplate.postForEntity(
            "/api/shipping", shippingDto, ShippingDto.class
        );
        
        assertEquals(HttpStatus.CREATED, shippingResponse.getStatusCode());
        assertNotNull(shippingResponse.getBody());
        assertNotNull(shippingResponse.getBody().getTrackingNumber());
    }

    @Test
    @DisplayName("Integration Test 2: Payment failure should cancel order and shipping")
    void testPaymentFailureCancelsOrderAndShipping() {
        // Step 1: Create order
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(199.99);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Step 2: Attempt payment with invalid card
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(199.99);
        paymentDto.setPaymentMethod("INVALID_CARD");
        
        ResponseEntity<PaymentDto> paymentResponse = restTemplate.postForEntity(
            "/api/payments/process", paymentDto, PaymentDto.class
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, paymentResponse.getStatusCode());
        
        // Step 3: Verify order is cancelled
        ResponseEntity<OrderDto> cancelledOrderResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId, OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, cancelledOrderResponse.getStatusCode());
        assertEquals("CANCELLED", cancelledOrderResponse.getBody().getStatus());
        
        // Step 4: Verify no shipping was created
        ResponseEntity<ShippingDto> shippingResponse = restTemplate.getForEntity(
            "/api/shipping/order/" + orderId, ShippingDto.class
        );
        
        assertEquals(HttpStatus.NOT_FOUND, shippingResponse.getStatusCode());
    }

    @Test
    @DisplayName("Integration Test 3: Shipping status updates affect order status")
    void testShippingStatusUpdatesAffectOrderStatus() {
        // Step 1: Create order and process payment
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(150.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Process payment
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(150.0);
        
        restTemplate.postForEntity("/api/payments/process", paymentDto, PaymentDto.class);
        
        // Step 2: Create shipping
        ShippingDto shippingDto = new ShippingDto();
        shippingDto.setOrderId(orderId);
        shippingDto.setAddress("456 Oak Ave, City, State 54321");
        
        ResponseEntity<ShippingDto> shippingResponse = restTemplate.postForEntity(
            "/api/shipping", shippingDto, ShippingDto.class
        );
        Integer shippingId = shippingResponse.getBody().getShippingId();
        
        // Step 3: Update shipping status to SHIPPED
        ShippingDto updateShipping = new ShippingDto();
        updateShipping.setStatus("SHIPPED");
        
        ResponseEntity<ShippingDto> updateResponse = restTemplate.putForEntity(
            "/api/shipping/" + shippingId, updateShipping, ShippingDto.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        
        // Step 4: Verify order status updated
        ResponseEntity<OrderDto> updatedOrderResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId, OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, updatedOrderResponse.getStatusCode());
        assertEquals("SHIPPED", updatedOrderResponse.getBody().getStatus());
    }

    @Test
    @DisplayName("Integration Test 4: Order cancellation should refund payment and cancel shipping")
    void testOrderCancellationRefundsPaymentAndCancelsShipping() {
        // Step 1: Create order, payment, and shipping
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(250.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Process payment
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(250.0);
        
        ResponseEntity<PaymentDto> paymentResponse = restTemplate.postForEntity(
            "/api/payments/process", paymentDto, PaymentDto.class
        );
        Integer paymentId = paymentResponse.getBody().getPaymentId();
        
        // Create shipping
        ShippingDto shippingDto = new ShippingDto();
        shippingDto.setOrderId(orderId);
        shippingDto.setAddress("789 Pine St, City, State 98765");
        
        ResponseEntity<ShippingDto> shippingResponse = restTemplate.postForEntity(
            "/api/shipping", shippingDto, ShippingDto.class
        );
        Integer shippingId = shippingResponse.getBody().getShippingId();
        
        // Step 2: Cancel order
        ResponseEntity<String> cancelResponse = restTemplate.postForEntity(
            "/api/orders/" + orderId + "/cancel", null, String.class
        );
        
        assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
        
        // Step 3: Verify payment refunded
        ResponseEntity<PaymentDto> refundedPaymentResponse = restTemplate.getForEntity(
            "/api/payments/" + paymentId, PaymentDto.class
        );
        
        assertEquals(HttpStatus.OK, refundedPaymentResponse.getStatusCode());
        assertEquals("REFUNDED", refundedPaymentResponse.getBody().getStatus());
        
        // Step 4: Verify shipping cancelled
        ResponseEntity<ShippingDto> cancelledShippingResponse = restTemplate.getForEntity(
            "/api/shipping/" + shippingId, ShippingDto.class
        );
        
        assertEquals(HttpStatus.OK, cancelledShippingResponse.getStatusCode());
        assertEquals("CANCELLED", cancelledShippingResponse.getBody().getStatus());
    }

    @Test
    @DisplayName("Integration Test 5: Delivery confirmation updates all services")
    void testDeliveryConfirmationUpdatesAllServices() {
        // Step 1: Create complete order flow
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(175.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Process payment
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setOrderId(orderId);
        paymentDto.setAmount(175.0);
        
        restTemplate.postForEntity("/api/payments/process", paymentDto, PaymentDto.class);
        
        // Create shipping
        ShippingDto shippingDto = new ShippingDto();
        shippingDto.setOrderId(orderId);
        shippingDto.setAddress("321 Elm St, City, State 13579");
        
        ResponseEntity<ShippingDto> shippingResponse = restTemplate.postForEntity(
            "/api/shipping", shippingDto, ShippingDto.class
        );
        Integer shippingId = shippingResponse.getBody().getShippingId();
        
        // Step 2: Confirm delivery
        ResponseEntity<String> deliveryResponse = restTemplate.postForEntity(
            "/api/shipping/" + shippingId + "/deliver", null, String.class
        );
        
        assertEquals(HttpStatus.OK, deliveryResponse.getStatusCode());
        
        // Step 3: Verify all services updated
        // Order status
        ResponseEntity<OrderDto> finalOrderResponse = restTemplate.getForEntity(
            "/api/orders/" + orderId, OrderDto.class
        );
        
        assertEquals(HttpStatus.OK, finalOrderResponse.getStatusCode());
        assertEquals("DELIVERED", finalOrderResponse.getBody().getStatus());
        
        // Shipping status
        ResponseEntity<ShippingDto> finalShippingResponse = restTemplate.getForEntity(
            "/api/shipping/" + shippingId, ShippingDto.class
        );
        
        assertEquals(HttpStatus.OK, finalShippingResponse.getStatusCode());
        assertEquals("DELIVERED", finalShippingResponse.getBody().getStatus());
    }
}
