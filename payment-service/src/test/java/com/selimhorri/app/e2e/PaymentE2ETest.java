package com.selimhorri.app.e2e;

import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.domain.PaymentStatus;
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
class PaymentE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static Integer paymentId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/payment-service/api/payments";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete payment initialization flow")
    void testPaymentInitialization() {
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(100);
        orderDto.setOrderDesc("E2E Test Order");
        orderDto.setOrderFee(299.99);
        
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setIsPayed(false);
        paymentDto.setPaymentStatus(PaymentStatus.NOT_STARTED);
        paymentDto.setOrderDto(orderDto);
        
        ResponseEntity<PaymentDto> response = restTemplate.postForEntity(
            baseUrl, paymentDto, PaymentDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        paymentId = response.getBody().getPaymentId();
        assertEquals(PaymentStatus.NOT_STARTED, response.getBody().getPaymentStatus());
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Payment processing flow")
    void testPaymentProcessing() {
        assertNotNull(paymentId, "Payment must be initialized first");
        
        ResponseEntity<PaymentDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + paymentId, PaymentDto.class
        );
        
        PaymentDto payment = getResponse.getBody();
        payment.setPaymentStatus(PaymentStatus.IN_PROGRESS);
        
        restTemplate.put(baseUrl, payment);
        
        ResponseEntity<PaymentDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + paymentId, PaymentDto.class
        );
        
        assertEquals(PaymentStatus.IN_PROGRESS, updatedResponse.getBody().getPaymentStatus());
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Payment completion flow")
    void testPaymentCompletion() {
        ResponseEntity<PaymentDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + paymentId, PaymentDto.class
        );
        
        PaymentDto payment = getResponse.getBody();
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setIsPayed(true);
        
        restTemplate.put(baseUrl, payment);
        
        ResponseEntity<PaymentDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + paymentId, PaymentDto.class
        );
        
        assertEquals(PaymentStatus.COMPLETED, updatedResponse.getBody().getPaymentStatus());
        assertTrue(updatedResponse.getBody().getIsPayed());
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Payment history retrieval flow")
    void testPaymentHistoryRetrieval() {
        ResponseEntity<DtoCollectionResponse<PaymentDto>> allPaymentsResponse = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<PaymentDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, allPaymentsResponse.getStatusCode());
        assertNotNull(allPaymentsResponse.getBody());
        assertNotNull(allPaymentsResponse.getBody().getCollection());
        assertTrue(allPaymentsResponse.getBody().getCollection().size() > 0);
        
        ResponseEntity<PaymentDto> specificPaymentResponse = restTemplate.getForEntity(
            baseUrl + "/" + paymentId, PaymentDto.class
        );
        
        assertEquals(HttpStatus.OK, specificPaymentResponse.getStatusCode());
        assertEquals(paymentId, specificPaymentResponse.getBody().getPaymentId());
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Payment verification flow")
    void testPaymentVerification() {
        ResponseEntity<PaymentDto> response = restTemplate.getForEntity(
            baseUrl + "/" + paymentId, PaymentDto.class
        );
        
        PaymentDto payment = response.getBody();
        assertNotNull(payment);
        assertTrue(payment.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, payment.getPaymentStatus());
        assertNotNull(payment.getOrderDto());
        assertEquals(100, payment.getOrderDto().getOrderId());
    }
}