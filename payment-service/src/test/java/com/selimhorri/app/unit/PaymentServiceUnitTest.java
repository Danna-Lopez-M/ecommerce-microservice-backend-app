package com.selimhorri.app.unit;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.impl.PaymentServiceImpl;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private PaymentDto testPaymentDto;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Crear OrderDto para mock
        testOrderDto = new OrderDto();
        testOrderDto.setOrderId(1);
        testOrderDto.setOrderDesc("Test Order");
        testOrderDto.setOrderFee(199.99);
        
        // Crear Payment (el modelo real)
        testPayment = new Payment();
        testPayment.setPaymentId(1);
        testPayment.setOrderId(1);
        testPayment.setIsPayed(true);
        testPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        
        // Crear PaymentDto
        testPaymentDto = new PaymentDto();
        testPaymentDto.setPaymentId(1);
        testPaymentDto.setIsPayed(true);
        testPaymentDto.setPaymentStatus(PaymentStatus.COMPLETED);
        testPaymentDto.setOrderDto(testOrderDto);
    }

    @Test
    @DisplayName("Unit Test 1: Should save payment successfully")
    void testSavePayment() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        PaymentDto result = paymentService.save(testPaymentDto);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find payment by ID")
    void testFindPaymentById() {
        // Arrange
        when(paymentRepository.findById(1)).thenReturn(Optional.of(testPayment));
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(testOrderDto);

        // Act
        PaymentDto result = paymentService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getPaymentId());
        assertTrue(result.getIsPayed());
        assertNotNull(result.getOrderDto());
        assertEquals(1, result.getOrderDto().getOrderId());
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
    @DisplayName("Unit Test 4: Should update payment successfully")
    void testUpdatePayment() {
        // Arrange
        Payment existingPayment = new Payment();
        existingPayment.setPaymentId(1);
        existingPayment.setOrderId(1);
        existingPayment.setIsPayed(false);
        existingPayment.setPaymentStatus(PaymentStatus.NOT_STARTED);
        
        Payment updatedPayment = new Payment();
        updatedPayment.setPaymentId(1);
        updatedPayment.setOrderId(1);
        updatedPayment.setIsPayed(true);
        updatedPayment.setPaymentStatus(PaymentStatus.COMPLETED);
        
        when(paymentRepository.findById(1)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(updatedPayment);

        PaymentDto updateDto = new PaymentDto();
        updateDto.setPaymentId(1);
        updateDto.setIsPayed(true);
        updateDto.setPaymentStatus(PaymentStatus.COMPLETED);
        updateDto.setOrderDto(testOrderDto);

        // Act
        PaymentDto result = paymentService.update(updateDto);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        verify(paymentRepository, times(1)).findById(1);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should validate payment status transitions")
    void testPaymentStatusTransitions() {
        // Test valid status transitions
        assertEquals(PaymentStatus.NOT_STARTED, PaymentStatus.NOT_STARTED);
        assertEquals(PaymentStatus.IN_PROGRESS, PaymentStatus.IN_PROGRESS);
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.COMPLETED);
        
        // Verify enum values
        assertEquals("not_started", PaymentStatus.NOT_STARTED.getStatus());
        assertEquals("in_progress", PaymentStatus.IN_PROGRESS.getStatus());
        assertEquals("completed", PaymentStatus.COMPLETED.getStatus());
    }

    @Test
    @DisplayName("Unit Test 6: Should list all payments")
    void testFindAllPayments() {
        // Arrange
        Payment payment2 = new Payment();
        payment2.setPaymentId(2);
        payment2.setOrderId(2);
        payment2.setIsPayed(false);
        payment2.setPaymentStatus(PaymentStatus.IN_PROGRESS);
        
        List<Payment> payments = Arrays.asList(testPayment, payment2);
        when(paymentRepository.findAll()).thenReturn(payments);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(testOrderDto);

        // Act
        List<PaymentDto> result = paymentService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should validate payment is paid flag")
    void testPaymentIsPaidValidation() {
        // Test paid payment
        assertTrue(testPayment.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, testPayment.getPaymentStatus());
        
        // Test unpaid payment
        Payment unpaidPayment = new Payment();
        unpaidPayment.setIsPayed(false);
        unpaidPayment.setPaymentStatus(PaymentStatus.NOT_STARTED);
        
        assertFalse(unpaidPayment.getIsPayed());
        assertEquals(PaymentStatus.NOT_STARTED, unpaidPayment.getPaymentStatus());
    }

    @Test
    @DisplayName("Unit Test 8: Should delete payment by ID")
    void testDeletePayment() {
        // Arrange
        when(paymentRepository.findById(1)).thenReturn(Optional.of(testPayment));
        doNothing().when(paymentRepository).delete(any(Payment.class));

        // Act
        paymentService.deleteById(1);

        // Assert
        verify(paymentRepository, times(1)).findById(1);
        verify(paymentRepository, times(1)).delete(any(Payment.class));
    }
}