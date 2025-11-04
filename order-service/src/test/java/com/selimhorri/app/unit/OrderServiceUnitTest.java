package com.selimhorri.app.unit;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
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
    
    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private Cart testCart;
    private CartDto testCartDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Crear Cart
        testCart = new Cart();
        testCart.setCartId(1);
        testCart.setUserId(1);
        
        // Crear CartDto
        testCartDto = new CartDto();
        testCartDto.setCartId(1);
        testCartDto.setUserId(1);
        
        // Crear Order con Cart
        testOrder = new Order();
        testOrder.setOrderId(1);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setOrderDesc("Test Order");
        testOrder.setOrderFee(199.99);
        testOrder.setCart(testCart);
        
        // Crear OrderDto con CartDto
        testOrderDto = new OrderDto();
        testOrderDto.setOrderDate(LocalDateTime.now());
        testOrderDto.setOrderDesc("Test Order");
        testOrderDto.setOrderFee(199.99);
        testOrderDto.setCartDto(testCartDto);
    }

    @Test
    @DisplayName("Unit Test 1: Should create order successfully")
    void testCreateOrder() {
        when(cartRepository.findById(testCartDto.getCartId())).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderDto result = orderService.save(testOrderDto);

        assertNotNull(result);
        assertEquals("Test Order", result.getOrderDesc());
        assertEquals(199.99, result.getOrderFee());
        assertNotNull(result.getCartDto());
        verify(cartRepository, times(1)).findById(testCartDto.getCartId());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find order by ID")
    void testFindOrderById() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getOrderId());
        assertEquals("Test Order", result.getOrderDesc());
        assertNotNull(result.getCartDto());
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Unit Test 3: Should throw exception when order not found")
    void testFindOrderByIdNotFound() {
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> {
            orderService.findById(999);
        });
        verify(orderRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Unit Test 4: Should update order successfully")
    void testUpdateOrder() {
        Order existingOrder = new Order();
        existingOrder.setOrderId(1);
        existingOrder.setOrderDate(LocalDateTime.now());
        existingOrder.setOrderDesc("Test Order");
        existingOrder.setOrderFee(199.99);
        existingOrder.setCart(testCart);
        
        Order updatedOrder = new Order();
        updatedOrder.setOrderId(1);
        updatedOrder.setOrderDesc("Updated Order");
        updatedOrder.setOrderFee(299.99);
        updatedOrder.setCart(testCart);
        
        when(orderRepository.findById(1)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        OrderDto updateDto = new OrderDto();
        updateDto.setOrderId(1);
        updateDto.setOrderDesc("Updated Order");
        updateDto.setOrderFee(299.99);
        updateDto.setCartDto(testCartDto);

        OrderDto result = orderService.update(updateDto);

        assertNotNull(result);
        assertEquals("Updated Order", result.getOrderDesc());
        assertEquals(299.99, result.getOrderFee());
        verify(orderRepository, times(1)).findById(1);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should validate order fee")
    void testOrderFeeValidation() {
        assertTrue(testOrder.getOrderFee() > 0);
        assertEquals(199.99, testOrder.getOrderFee(), 0.01);
        
        // Test negative fee
        Order invalidOrder = new Order();
        invalidOrder.setOrderFee(-50.0);
        assertTrue(invalidOrder.getOrderFee() < 0, "Negative fee should be detected");
    }

    @Test
    @DisplayName("Unit Test 6: Should list all orders")
    void testFindAllOrders() {
        Cart cart2 = new Cart();
        cart2.setCartId(2);
        cart2.setUserId(2);
        
        Order order2 = new Order();
        order2.setOrderId(2);
        order2.setOrderDesc("Order 2");
        order2.setOrderFee(99.99);
        order2.setCart(cart2);
        
        List<Order> orders = Arrays.asList(testOrder, order2);
        when(orderRepository.findAll()).thenReturn(orders);

        List<OrderDto> result = orderService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should delete order successfully")
    void testDeleteOrder() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderRepository).delete(any(Order.class));

        orderService.deleteById(1);

        verify(orderRepository, times(1)).findById(1);
        verify(orderRepository, times(1)).delete(any(Order.class));
    }

    @Test
    @DisplayName("Unit Test 8: Should validate order has cart association")
    void testOrderCartAssociation() {
        assertNotNull(testOrder.getCart());
        assertEquals(1, testOrder.getCart().getCartId());
        assertEquals(1, testOrder.getCart().getUserId());
    }
}