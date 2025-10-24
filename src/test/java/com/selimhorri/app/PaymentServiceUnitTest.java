package com.selimhorri.app.service;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private PaymentDto testPaymentDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testPayment = new Payment();
        testPayment.setPaymentId(1);
        testPayment.setOrderId(1);
        testPayment.setAmount(199.99);
        testPayment.setPaymentMethod("CREDIT_CARD");
        testPayment.setStatus("COMPLETED");
        testPayment.setPaymentDate(LocalDateTime.now());
        
        testPaymentDto = new PaymentDto();
        testPaymentDto.setOrderId(1);
        testPaymentDto.setAmount(199.99);
        testPaymentDto.setPaymentMethod("CREDIT_CARD");
        testPaymentDto.setStatus("COMPLETED");
    }

    @Test
    @DisplayName("Unit Test 1: Should process payment successfully")
    void testProcessPayment() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        PaymentDto result = paymentService.processPayment(testPaymentDto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getOrderId());
        assertEquals(199.99, result.getAmount());
        assertEquals("COMPLETED", result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find payment by ID")
    void testFindPaymentById() {
        // Arrange
        when(paymentRepository.findById(1)).thenReturn(Optional.of(testPayment));

        // Act
        PaymentDto result = paymentService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getPaymentId());
        assertEquals(1, result.getOrderId());
        verify(paymentRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Unit Test 3: Should throw exception when payment not found")
    void testFindPaymentByIdNotFound() {
        // Arrange
        when(paymentRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.findById(999);
        });
        verify(paymentRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Unit Test 4: Should validate credit card number")
    void testCreditCardValidation() {
        // Arrange
        String validCard = "4111111111111111";
        String invalidCard = "1234567890123456";

        // Act
        boolean validResult = paymentService.isValidCreditCard(validCard);
        boolean invalidResult = paymentService.isValidCreditCard(invalidCard);

        // Assert
        assertTrue(validResult, "Valid credit card should pass validation");
        assertFalse(invalidResult, "Invalid credit card should fail validation");
    }

    @Test
    @DisplayName("Unit Test 5: Should handle payment failure")
    void testPaymentFailure() {
        // Arrange
        PaymentDto failedPayment = new PaymentDto();
        failedPayment.setOrderId(1);
        failedPayment.setAmount(199.99);
        failedPayment.setPaymentMethod("INVALID_CARD");
        
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        PaymentDto result = paymentService.processPayment(failedPayment);

        // Assert
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Unit Test 6: Should list all payments")
    void testFindAllPayments() {
        // Arrange
        List<Payment> payments = Arrays.asList(testPayment, new Payment());
        when(paymentRepository.findAll()).thenReturn(payments);

        // Act
        List<PaymentDto> result = paymentService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should validate payment amount")
    void testPaymentAmountValidation() {
        // Arrange
        double validAmount = 100.0;
        double invalidAmount = -50.0;

        // Act
        boolean validResult = paymentService.isValidAmount(validAmount);
        boolean invalidResult = paymentService.isValidAmount(invalidAmount);

        // Assert
        assertTrue(validResult, "Valid amount should pass validation");
        assertFalse(invalidResult, "Invalid amount should fail validation");
    }

    @Test
    @DisplayName("Unit Test 8: Should handle payment refund")
    void testPaymentRefund() {
        // Arrange
        when(paymentRepository.findById(1)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        PaymentDto result = paymentService.refundPayment(1, 50.0);

        // Assert
        assertNotNull(result);
        assertEquals("REFUNDED", result.getStatus());
        verify(paymentRepository, times(1)).findById(1);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
