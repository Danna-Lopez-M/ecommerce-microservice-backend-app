package com.selimhorri.app.unit;

import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.business.product.service.ProductClientService;
import com.selimhorri.app.business.order.service.OrderClientService;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.order.model.OrderDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
//import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProxyClientUnitTest {

    @MockBean
    private UserClientService userClientService;

    @MockBean
    private ProductClientService productClientService;

    @MockBean
    private OrderClientService orderClientService;

    @Test
    @DisplayName("Unit Test 1: Should call user service via Feign client")
    void testUserServiceFeignClient() {
        // Arrange
        UserDto testUser = new UserDto();
        testUser.setUserId(1);
        testUser.setFirstName("Test");
        testUser.setEmail("test@example.com");
        
        when(userClientService.findById("1"))
            .thenReturn(ResponseEntity.ok(testUser));

        // Act
        ResponseEntity<UserDto> response = userClientService.findById("1");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getUserId());
        verify(userClientService, times(1)).findById("1");
    }

    @Test
    @DisplayName("Unit Test 2: Should call product service via Feign client")
    void testProductServiceFeignClient() {
        // Arrange
        ProductDto testProduct = new ProductDto();
        testProduct.setProductId(1);
        testProduct.setProductTitle("Test Product");
        testProduct.setPriceUnit(99.99);
        
        when(productClientService.findById("1"))
            .thenReturn(ResponseEntity.ok(testProduct));

        // Act
        ResponseEntity<ProductDto> response = productClientService.findById("1");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test Product", response.getBody().getProductTitle());
        verify(productClientService, times(1)).findById("1");
    }

    @Test
    @DisplayName("Unit Test 3: Should call order service via Feign client")
    void testOrderServiceFeignClient() {
        // Arrange
        OrderDto testOrder = new OrderDto();
        testOrder.setOrderId(1);
        
        when(orderClientService.findById("1"))
            .thenReturn(ResponseEntity.ok(testOrder));

        // Act
        ResponseEntity<OrderDto> response = orderClientService.findById("1");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getOrderId());
        verify(orderClientService, times(1)).findById("1");
    }

    @Test
    @DisplayName("Unit Test 4: Should handle user not found")
    void testUserNotFound() {
        // Arrange
        when(userClientService.findById("999"))
            .thenReturn(ResponseEntity.notFound().build());

        // Act
        ResponseEntity<UserDto> response = userClientService.findById("999");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userClientService, times(1)).findById("999");
    }

    @Test
    @DisplayName("Unit Test 5: Should create new user")
    void testCreateUser() {
        // Arrange
        UserDto newUser = new UserDto();
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setEmail("new@example.com");
        
        UserDto savedUser = new UserDto();
        savedUser.setUserId(1);
        savedUser.setFirstName("New");
        savedUser.setEmail("new@example.com");
        
        when(userClientService.save(newUser))
            .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(savedUser));

        // Act
        ResponseEntity<UserDto> response = userClientService.save(newUser);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getUserId());
        verify(userClientService, times(1)).save(newUser);
    }

    @Test
    @DisplayName("Unit Test 6: Should get all users")
    void testGetAllUsers() {
        // Arrange
        UserUserServiceCollectionDtoResponse usersResponse = 
            new UserUserServiceCollectionDtoResponse();
        
        when(userClientService.findAll())
            .thenReturn(ResponseEntity.ok(usersResponse));

        // Act
        ResponseEntity<UserUserServiceCollectionDtoResponse> response = 
            userClientService.findAll();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userClientService, times(1)).findAll();
    }
}