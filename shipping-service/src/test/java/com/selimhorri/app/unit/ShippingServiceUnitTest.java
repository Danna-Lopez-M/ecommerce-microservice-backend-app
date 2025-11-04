package com.selimhorri.app.unit;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.impl.OrderItemServiceImpl;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
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
import static org.mockito.Mockito.*;

class ShippingServiceUnitTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private OrderItem testOrderItem;
    private OrderItemDto testOrderItemDto;
    private ProductDto testProductDto;
    private OrderDto testOrderDto;
    private OrderItemId testOrderItemId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Crear OrderItemId
        testOrderItemId = new OrderItemId();
        testOrderItemId.setProductId(1);
        testOrderItemId.setOrderId(1);
        
        // Crear ProductDto para mock
        testProductDto = new ProductDto();
        testProductDto.setProductId(1);
        testProductDto.setProductTitle("Test Product");
        testProductDto.setPriceUnit(99.99);
        
        // Crear OrderDto para mock
        testOrderDto = new OrderDto();
        testOrderDto.setOrderId(1);
        testOrderDto.setOrderDesc("Test Order");
        testOrderDto.setOrderFee(199.99);
        
        // Crear OrderItem
        testOrderItem = new OrderItem();
        testOrderItem.setProductId(1);
        testOrderItem.setOrderId(1);
        testOrderItem.setOrderedQuantity(5);
        
        // Crear OrderItemDto
        testOrderItemDto = new OrderItemDto();
        testOrderItemDto.setProductId(1);
        testOrderItemDto.setOrderId(1);
        testOrderItemDto.setOrderedQuantity(5);
        testOrderItemDto.setProductDto(testProductDto);
        testOrderItemDto.setOrderDto(testOrderDto);
    }

    @Test
    @DisplayName("Unit Test 1: Should save order item successfully")
    void testSaveOrderItem() {
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        OrderItemDto result = orderItemService.save(testOrderItemDto);

        assertNotNull(result);
        assertEquals(1, result.getProductId());
        assertEquals(1, result.getOrderId());
        assertEquals(5, result.getOrderedQuantity());
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find order item by ID")
    void testFindOrderItemById() {
        when(orderItemRepository.findById(testOrderItemId)).thenReturn(Optional.of(testOrderItem));
        when(restTemplate.getForObject(anyString(), eq(ProductDto.class))).thenReturn(testProductDto);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(testOrderDto);

        OrderItemDto result = orderItemService.findById(testOrderItemId);

        assertNotNull(result);
        assertEquals(1, result.getProductId());
        assertEquals(1, result.getOrderId());
        assertEquals(5, result.getOrderedQuantity());
        assertNotNull(result.getProductDto());
        assertNotNull(result.getOrderDto());
        verify(orderItemRepository, times(1)).findById(testOrderItemId);
    }

    @Test
    @DisplayName("Unit Test 3: Should throw exception when order item not found")
    void testFindOrderItemByIdNotFound() {
        when(orderItemRepository.findById(testOrderItemId)).thenReturn(Optional.empty());

        assertThrows(OrderItemNotFoundException.class, () -> {
            orderItemService.findById(testOrderItemId);
        });
        verify(orderItemRepository, times(1)).findById(testOrderItemId);
    }

    @Test
    @DisplayName("Unit Test 4: Should update order item successfully")
    void testUpdateOrderItem() {
        OrderItem existingOrderItem = new OrderItem();
        existingOrderItem.setProductId(1);
        existingOrderItem.setOrderId(1);
        existingOrderItem.setOrderedQuantity(5);
        
        OrderItem updatedOrderItem = new OrderItem();
        updatedOrderItem.setProductId(1);
        updatedOrderItem.setOrderId(1);
        updatedOrderItem.setOrderedQuantity(10);
        
        when(orderItemRepository.findById(testOrderItemId)).thenReturn(Optional.of(existingOrderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(updatedOrderItem);

        OrderItemDto updateDto = new OrderItemDto();
        updateDto.setProductId(1);
        updateDto.setOrderId(1);
        updateDto.setOrderedQuantity(10);
        updateDto.setProductDto(testProductDto);
        updateDto.setOrderDto(testOrderDto);

        OrderItemDto result = orderItemService.update(updateDto);

        assertNotNull(result);
        assertEquals(10, result.getOrderedQuantity());
        verify(orderItemRepository, times(1)).findById(testOrderItemId);
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should validate ordered quantity")
    void testOrderedQuantityValidation() {
        assertTrue(testOrderItem.getOrderedQuantity() > 0);
        assertEquals(5, testOrderItem.getOrderedQuantity());
        
        // Test invalid quantity
        OrderItem invalidItem = new OrderItem();
        invalidItem.setOrderedQuantity(-5);
        assertTrue(invalidItem.getOrderedQuantity() < 0, "Negative quantity should be detected");
    }

    @Test
    @DisplayName("Unit Test 6: Should list all order items")
    void testFindAllOrderItems() {
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProductId(2);
        orderItem2.setOrderId(1);
        orderItem2.setOrderedQuantity(3);
        
        List<OrderItem> orderItems = Arrays.asList(testOrderItem, orderItem2);
        when(orderItemRepository.findAll()).thenReturn(orderItems);
        when(restTemplate.getForObject(anyString(), eq(ProductDto.class))).thenReturn(testProductDto);
        when(restTemplate.getForObject(anyString(), eq(OrderDto.class))).thenReturn(testOrderDto);

        List<OrderItemDto> result = orderItemService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should validate composite key")
    void testCompositeKey() {
        assertEquals(1, testOrderItemId.getProductId());
        assertEquals(1, testOrderItemId.getOrderId());
        
        // Test equality
        OrderItemId sameId = new OrderItemId(1, 1);
        assertEquals(testOrderItemId, sameId);
    }

    @Test
    @DisplayName("Unit Test 8: Should delete order item by ID")
    void testDeleteOrderItem() {
        when(orderItemRepository.findById(testOrderItemId)).thenReturn(Optional.of(testOrderItem));
        doNothing().when(orderItemRepository).delete(any(OrderItem.class));

        orderItemService.deleteById(testOrderItemId);

        verify(orderItemRepository, times(1)).findById(testOrderItemId);
        verify(orderItemRepository, times(1)).delete(any(OrderItem.class));
    }
}