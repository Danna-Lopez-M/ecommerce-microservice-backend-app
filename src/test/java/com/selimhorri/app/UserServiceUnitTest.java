package com.selimhorri.app.service;

import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        
        testUserDto = new UserDto();
        testUserDto.setUsername("testuser");
        testUserDto.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Test 1: Should create user successfully")
    void testCreateUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDto result = userService.createUser(testUserDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Test 2: Should find user by ID")
    void testFindUserById() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        UserDto result = userService.findById(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Test 3: Should throw exception when user not found")
    void testFindUserByIdNotFound() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.findById(999);
        });
        verify(userRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Test 4: Should update user successfully")
    void testUpdateUser() {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId(1);
        updatedUser.setUsername("updateduser");
        updatedUser.setEmail("updated@example.com");
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDto updateDto = new UserDto();
        updateDto.setUsername("updateduser");
        updateDto.setEmail("updated@example.com");

        // Act
        UserDto result = userService.updateUser(1, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", result.getUsername());
        verify(userRepository, times(1)).findById(1);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Test 5: Should validate email format correctly")
    void testEmailValidation() {
        // Arrange
        String validEmail = "test@example.com";
        String invalidEmail = "invalid-email";

        // Act
        boolean validResult = userService.isValidEmail(validEmail);
        boolean invalidResult = userService.isValidEmail(invalidEmail);

        // Assert
        assertTrue(validResult, "Valid email should pass validation");
        assertFalse(invalidResult, "Invalid email should fail validation");
    }

    @Test
    @DisplayName("Test 6: Should list all users")
    void testFindAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser, new User());
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserDto> result = userService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }
}