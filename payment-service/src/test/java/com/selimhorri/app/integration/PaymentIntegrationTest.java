package com.selimhorri.app.integration;

import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.domain.PaymentStatus;
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
class PaymentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/payment-service/api/payments";
    }

    @Test
    @DisplayName("Integration Test 1: Should process payment successfully")
    void testProcessPayment() {
        PaymentDto paymentDto = createTestPayment(1, PaymentStatus.NOT_STARTED);
        
        ResponseEntity<PaymentDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), paymentDto, PaymentDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals(PaymentStatus.NOT_STARTED, createResponse.getBody().getPaymentStatus());
    }

    @Test
    @DisplayName("Integration Test 2: Should retrieve payment by ID")
    void testRetrievePayment() {
        PaymentDto paymentDto = createTestPayment(2, PaymentStatus.IN_PROGRESS);
        ResponseEntity<PaymentDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), paymentDto, PaymentDto.class
        );
        
        Integer paymentId = createResponse.getBody().getPaymentId();
        
        ResponseEntity<PaymentDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + paymentId, PaymentDto.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(PaymentStatus.IN_PROGRESS, getResponse.getBody().getPaymentStatus());
    }

    @Test
    @DisplayName("Integration Test 3: Should update payment status")
    void testUpdatePaymentStatus() {
        PaymentDto paymentDto = createTestPayment(3, PaymentStatus.NOT_STARTED);
        ResponseEntity<PaymentDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), paymentDto, PaymentDto.class
        );
        
        PaymentDto createdPayment = createResponse.getBody();
        createdPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        createdPayment.setIsPayed(true);
        
        restTemplate.put(getBaseUrl(), createdPayment);
        
        ResponseEntity<PaymentDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + createdPayment.getPaymentId(), PaymentDto.class
        );
        
        assertEquals(PaymentStatus.COMPLETED, getResponse.getBody().getPaymentStatus());
        assertTrue(getResponse.getBody().getIsPayed());
    }

    @Test
    @DisplayName("Integration Test 4: Should list all payments")
    void testListAllPayments() {
        ResponseEntity<DtoCollectionResponse<PaymentDto>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<PaymentDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
    }

    @Test
    @DisplayName("Integration Test 5: Should handle payment for order")
    void testPaymentOrderAssociation() {
        PaymentDto paymentDto = createTestPayment(5, PaymentStatus.COMPLETED);
        paymentDto.setIsPayed(true);
        
        ResponseEntity<PaymentDto> response = restTemplate.postForEntity(
            getBaseUrl(), paymentDto, PaymentDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getOrderDto());
        assertEquals(5, response.getBody().getOrderDto().getOrderId());
    }

    private PaymentDto createTestPayment(Integer orderId, PaymentStatus status) {
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(orderId);
        orderDto.setOrderDesc("Test Order " + orderId);
        orderDto.setOrderFee(199.99);
        
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setIsPayed(false);
        paymentDto.setPaymentStatus(status);
        paymentDto.setOrderDto(orderDto);
        
        return paymentDto;
    }
}