package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.UserRepository;

/**
 * Unit tests for Bulkhead Pattern implementation in UserServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class BulkheadPatternTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");

        testUserDto = new UserDto();
        testUserDto.setUserId(1);
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");
    }

    @Test
    void testFindAll_Success() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

        // When
        List<UserDto> result = userService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testFindAll_Fallback_ReturnsEmptyList() {
        // When - Call fallback directly
        List<UserDto> result = userService.findAllFallback(new RuntimeException("Database error"));

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindById_Success() {
        // Given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findById(1);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void testFindById_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserObjectNotFoundException.class, () -> {
            userService.findById(999);
        });
    }

    @Test
    void testFindById_Fallback_ThrowsException() {
        // When & Then
        UserObjectNotFoundException exception = assertThrows(
            UserObjectNotFoundException.class,
            () -> userService.findByIdFallback(1, new RuntimeException("Service overloaded"))
        );
        
        assertTrue(exception.getMessage().contains("Service temporarily unavailable"));
    }

    @Test
    void testSave_Success() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto result = userService.save(testUserDto);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testSave_Fallback_ThrowsException() {
        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.saveFallback(testUserDto, new RuntimeException("Service overloaded"))
        );
        
        assertTrue(exception.getMessage().contains("Service temporarily unavailable"));
    }

    @Test
    void testDeleteById_Success() {
        // Given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        // When
        assertDoesNotThrow(() -> userService.deleteById(1));

        // Then
        verify(userRepository, times(1)).findById(1);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void testDeleteById_Fallback_ThrowsException() {
        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.deleteByIdFallback(1, new RuntimeException("Service overloaded"))
        );
        
        assertTrue(exception.getMessage().contains("Service temporarily unavailable"));
    }

    @Test
    void testFindByUsername_Success() {
        // Given
        when(userRepository.findByCredentialUsername("johndoe")).thenReturn(Optional.of(testUser));

        // When
        UserDto result = userService.findByUsername("johndoe");

        // Then
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository, times(1)).findByCredentialUsername("johndoe");
    }

    @Test
    void testFindByUsername_Fallback_ThrowsException() {
        // When & Then
        UserObjectNotFoundException exception = assertThrows(
            UserObjectNotFoundException.class,
            () -> userService.findByUsernameFallback("johndoe", new RuntimeException("Service overloaded"))
        );
        
        assertTrue(exception.getMessage().contains("Service temporarily unavailable"));
    }
}
