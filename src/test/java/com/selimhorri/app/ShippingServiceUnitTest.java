package com.selimhorri.app.service;

import com.selimhorri.app.domain.Shipping;
import com.selimhorri.app.dto.ShippingDto;
import com.selimhorri.app.repository.ShippingRepository;
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

class ShippingServiceUnitTest {

    @Mock
    private ShippingRepository shippingRepository;

    @InjectMocks
    private ShippingServiceImpl shippingService;

    private Shipping testShipping;
    private ShippingDto testShippingDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testShipping = new Shipping();
        testShipping.setShippingId(1);
        testShipping.setOrderId(1);
        testShipping.setTrackingNumber("TRK123456789");
        testShipping.setStatus("PENDING");
        testShipping.setShippingDate(LocalDateTime.now());
        testShipping.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
        
        testShippingDto = new ShippingDto();
        testShippingDto.setOrderId(1);
        testShippingDto.setTrackingNumber("TRK123456789");
        testShippingDto.setStatus("PENDING");
    }

    @Test
    @DisplayName("Unit Test 1: Should create shipping successfully")
    void testCreateShipping() {
        // Arrange
        when(shippingRepository.save(any(Shipping.class))).thenReturn(testShipping);

        // Act
        ShippingDto result = shippingService.createShipping(testShippingDto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getOrderId());
        assertEquals("TRK123456789", result.getTrackingNumber());
        assertEquals("PENDING", result.getStatus());
        verify(shippingRepository, times(1)).save(any(Shipping.class));
    }

    @Test
    @DisplayName("Unit Test 2: Should find shipping by ID")
    void testFindShippingById() {
        // Arrange
        when(shippingRepository.findById(1)).thenReturn(Optional.of(testShipping));

        // Act
        ShippingDto result = shippingService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getShippingId());
        assertEquals(1, result.getOrderId());
        verify(shippingRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Unit Test 3: Should throw exception when shipping not found")
    void testFindShippingByIdNotFound() {
        // Arrange
        when(shippingRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ShippingNotFoundException.class, () -> {
            shippingService.findById(999);
        });
        verify(shippingRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Unit Test 4: Should update shipping status")
    void testUpdateShippingStatus() {
        // Arrange
        Shipping updatedShipping = new Shipping();
        updatedShipping.setShippingId(1);
        updatedShipping.setStatus("SHIPPED");
        
        when(shippingRepository.findById(1)).thenReturn(Optional.of(testShipping));
        when(shippingRepository.save(any(Shipping.class))).thenReturn(updatedShipping);

        ShippingDto updateDto = new ShippingDto();
        updateDto.setStatus("SHIPPED");

        // Act
        ShippingDto result = shippingService.updateStatus(1, "SHIPPED");

        // Assert
        assertNotNull(result);
        assertEquals("SHIPPED", result.getStatus());
        verify(shippingRepository, times(1)).findById(1);
        verify(shippingRepository, times(1)).save(any(Shipping.class));
    }

    @Test
    @DisplayName("Unit Test 5: Should generate tracking number")
    void testGenerateTrackingNumber() {
        // Act
        String trackingNumber = shippingService.generateTrackingNumber();

        // Assert
        assertNotNull(trackingNumber);
        assertTrue(trackingNumber.startsWith("TRK"));
        assertEquals(12, trackingNumber.length());
    }

    @Test
    @DisplayName("Unit Test 6: Should list all shippings")
    void testFindAllShippings() {
        // Arrange
        List<Shipping> shippings = Arrays.asList(testShipping, new Shipping());
        when(shippingRepository.findAll()).thenReturn(shippings);

        // Act
        List<ShippingDto> result = shippingService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(shippingRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit Test 7: Should validate shipping address")
    void testShippingAddressValidation() {
        // Arrange
        String validAddress = "123 Main St, City, State 12345";
        String invalidAddress = "";

        // Act
        boolean validResult = shippingService.isValidAddress(validAddress);
        boolean invalidResult = shippingService.isValidAddress(invalidAddress);

        // Assert
        assertTrue(validResult, "Valid address should pass validation");
        assertFalse(invalidResult, "Invalid address should fail validation");
    }

    @Test
    @DisplayName("Unit Test 8: Should calculate estimated delivery")
    void testCalculateEstimatedDelivery() {
        // Arrange
        LocalDateTime shippingDate = LocalDateTime.now();

        // Act
        LocalDateTime estimatedDelivery = shippingService.calculateEstimatedDelivery(shippingDate);

        // Assert
        assertNotNull(estimatedDelivery);
        assertTrue(estimatedDelivery.isAfter(shippingDate));
    }
}
