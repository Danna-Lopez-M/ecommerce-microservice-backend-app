package com.selimhorri.app.unit;

import com.selimhorri.app.domain.User;
import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;

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
    private Credential testCredential;
    private CredentialDto testCredentialDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testCredential = new Credential();
        testCredential.setCredentialId(1);
        testCredential.setUsername("testuser");
        testCredential.setPassword("testpassword");
        testCredential.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        testCredential.setIsEnabled(true);
        testCredential.setIsAccountNonExpired(true);
        testCredential.setIsAccountNonLocked(true);
        testCredential.setIsCredentialsNonExpired(true);

        testUser = new User();
        testUser.setUserId(1);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCredential(testCredential);
        
        // Crear DTOs
        testUserDto = new UserDto();
        testUserDto.setUserId(1);
        testUserDto.setFirstName("Test");
        testUserDto.setLastName("User");
        testUserDto.setEmail("test@example.com");

        testCredentialDto = new CredentialDto();
        testCredentialDto.setUsername("testuser");
        testCredentialDto.setPassword("testpassword");
        testCredentialDto.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        testCredentialDto.setIsEnabled(true);
        testCredentialDto.setIsAccountNonExpired(true);
        testCredentialDto.setIsAccountNonLocked(true);
        testCredentialDto.setIsCredentialsNonExpired(true);

        testCredentialDto.setUserDto(testUserDto);
        testUserDto.setCredentialDto(testCredentialDto);
    }

    @Test
    @DisplayName("Test 1: Should create user successfully")
    void testCreateUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserDto result = userService.save(testUserDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test", result.getFirstName());
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
        assertEquals(1, result.getUserId());
        assertEquals("Test", result.getFirstName());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Test 3: Should throw exception when user not found")
    void testFindUserByIdNotFound() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserObjectNotFoundException.class, () -> {
            userService.findById(999);
        });
        verify(userRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Test 4: Should update user successfully")
    void testUpdateUser() {
        // Arrange
        Credential updatedCredential = new Credential();
        updatedCredential.setCredentialId(1);
        updatedCredential.setUsername("updateduser");
        updatedCredential.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        
        User updatedUser = new User();
        updatedUser.setUserId(1);
        updatedUser.setFirstName("UpdatedName");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setCredential(updatedCredential);
        
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDto updateDto = testUserDto;
        updateDto.setFirstName("UpdatedName");
        updateDto.setEmail("updated@example.com");

        // Act
        UserDto result = userService.update(updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("UpdatedName", result.getFirstName());
        assertEquals("updated@example.com", result.getEmail());
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
        Credential credential2 = new Credential();
        credential2.setCredentialId(2);
        credential2.setUsername("user2");
        credential2.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        
        User user2 = new User();
        user2.setUserId(2);
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setEmail("user2@example.com");
        user2.setCredential(credential2);
        
        List<User> users = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<UserDto> result = userService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }
}