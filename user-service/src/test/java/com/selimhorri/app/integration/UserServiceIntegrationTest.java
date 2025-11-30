package com.selimhorri.app.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Arrays;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.UserServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for User Service
 * Simplified to avoid full Spring Boot context loading
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserServiceImpl.class})
class UserIntegrationTest {

    @MockBean
    private UserRepository userRepository;
    
    @Autowired
    private UserServiceImpl userService;
    
    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@test.com");
        testUser.setPhone("1234567890");
        
        Credential credential = new Credential();
        credential.setUsername("testuser");
        credential.setPassword("Test123!");
        credential.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        testUser.setCredential(credential);
        
        testUserDto = new UserDto();
        testUserDto.setUserId(1);
        testUserDto.setFirstName("Test");
        testUserDto.setLastName("User");
        testUserDto.setEmail("test@test.com");
        testUserDto.setPhone("1234567890");
        
        CredentialDto credentialDto = new CredentialDto();
        credentialDto.setUsername("testuser");
        credentialDto.setPassword("Test123!");
        credentialDto.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        testUserDto.setCredentialDto(credentialDto);
    }

    @Test
    @DisplayName("Integration Test 1: Should create and retrieve user")
    void testCreateAndRetrieveUser() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        
        // When - Create user
        UserDto created = userService.save(testUserDto);
        
        // Then
        assertNotNull(created);
        assertEquals("Test", created.getFirstName());
        
        // When - Retrieve user
        UserDto retrieved = userService.findById(1);
        
        // Then
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.getCredentialDto().getUsername());
        
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Integration Test 2: Should update user information")
    void testUpdateUser() {
        // Given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        testUserDto.setFirstName("Updated");
        testUserDto.setLastName("Name");
        
        // When
        UserDto updated = userService.update(testUserDto);
        
        // Then
        assertNotNull(updated);
        verify(userRepository, times(1)).findById(1);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Integration Test 3: Should delete user")
    void testDeleteUser() {
        // Given
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(any(User.class));
        
        // When
        assertDoesNotThrow(() -> userService.deleteById(1));
        
        // Then
        verify(userRepository, times(1)).findById(1);
        verify(userRepository, times(1)).delete(any(User.class));
    }

    @Test
    @DisplayName("Integration Test 4: Should list all users")
    void testListAllUsers() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));
        
        // When
        var users = userService.findAll();
        
        // Then
        assertNotNull(users);
        assertEquals(1, users.size());
        verify(userRepository, times(1)).findAll();
    }
}