package com.selimhorri.app.service;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.repository.OrderRepository;
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

class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testOrder = new Order();
        testOrder.setOrderId(1);
        testOrder.setUserId(1);
        testOrder.setTotalAmount(199.99);
        testOrder.setStatus("PENDING");
        testOrder.setOrderDate(LocalDateTime.now());
        
        testOrderDto = new OrderDto();
        testOrderDto.setUserId(1);
        testOrderDto.setTotalAmount(199.99);
        testOrderDto.setStatus("PENDING");
    }

    @Test
    @DisplayName("Unit Test 1: Should create order successfully")
    void testCreateOrder() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderDto result = orderService.save(testOrderDto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals(199.99, result.getTotalAmount());
        assertEquals("PENDING", result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find order by ID")
    void testFindOrderById() {
        // Arrange
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));

        // Act
        OrderDto result = orderService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getOrderId());
        assertEquals(1, result.getUserId());
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Unit Test 3: Should throw exception when order not found")
    void testFindOrderByIdNotFound() {
        // Arrange
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.findById(999);
        });
        verify(orderRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Unit Test 4: Should update order status")
    void testUpdateOrderStatus() {
        // Arrange
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(1);
        updatedOrder.setStatus("PAID");
        
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderDto updateDto = new OrderDto();
        updateDto.setStatus("PAID");

        // Act
        OrderDto result = orderService.update(1, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("PAID", result.getStatus());
        verify(orderRepository, times(1)).findById(1);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should validate order amount")
    void testOrderAmountValidation() {
        // Arrange
        OrderDto validOrder = new OrderDto();
        validOrder.setTotalAmount(100.0);
        
        OrderDto invalidOrder = new OrderDto();
        invalidOrder.setTotalAmount(-50.0);

        // Act
        boolean validResult = orderService.isValidAmount(validOrder.getTotalAmount());
        boolean invalidResult = orderService.isValidAmount(invalidOrder.getTotalAmount());

        // Assert
        assertTrue(validResult, "Valid amount should pass validation");
        assertFalse(invalidResult, "Invalid amount should fail validation");
    }

    @Test
    @DisplayName("Unit Test 6: Should list all orders")
    void testFindAllOrders() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder, new Order());
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<OrderDto> result = orderService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should handle order cancellation")
    void testCancelOrder() {
        // Arrange
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderDto result = orderService.cancelOrder(1);

        // Assert
        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());
        verify(orderRepository, times(1)).findById(1);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 8: Should validate order status transition")
    void testOrderStatusTransition() {
        // Arrange
        String currentStatus = "PENDING";
        String newStatus = "PAID";

        // Act
        boolean validTransition = orderService.isValidStatusTransition(currentStatus, newStatus);
        boolean invalidTransition = orderService.isValidStatusTransition("CANCELLED", "PAID");

        // Assert
        assertTrue(validTransition, "Valid status transition should be allowed");
        assertFalse(invalidTransition, "Invalid status transition should be rejected");
    }
}
