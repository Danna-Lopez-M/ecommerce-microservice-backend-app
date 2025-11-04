package com.selimhorri.app.integration;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.domain.RoleBasedAuthority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/user-service/api/users";
    }

    @Test
    @DisplayName("Integration Test 1: Should create and retrieve user")
    void testCreateAndRetrieveUser() {
        // Create user
        UserDto userDto = createTestUser("integration1");
        
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), userDto, UserDto.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Integer userId = createResponse.getBody().getUserId();
        
        // Retrieve user
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + userId, UserDto.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("integration1", getResponse.getBody().getCredentialDto().getUsername());
    }

    @Test
    @DisplayName("Integration Test 2: Should update user information")
    void testUpdateUser() {
        // Create user
        UserDto userDto = createTestUser("integration2");
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), userDto, UserDto.class
        );
        
        UserDto createdUser = createResponse.getBody();
        createdUser.setFirstName("Updated");
        createdUser.setLastName("Name");
        
        // Update user
        restTemplate.put(getBaseUrl(), createdUser);
        
        // Verify update
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + createdUser.getUserId(), UserDto.class
        );
        
        assertEquals("Updated", getResponse.getBody().getFirstName());
        assertEquals("Name", getResponse.getBody().getLastName());
    }

    @Test
    @DisplayName("Integration Test 3: Should delete user")
    void testDeleteUser() {
        // Create user
        UserDto userDto = createTestUser("integration3");
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
            getBaseUrl(), userDto, UserDto.class
        );
        
        Integer userId = createResponse.getBody().getUserId();
        
        // Delete user
        restTemplate.delete(getBaseUrl() + "/" + userId);
        
        // Verify deletion
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            getBaseUrl() + "/" + userId, String.class
        );
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, getResponse.getStatusCode());
    }

    @Test
    @DisplayName("Integration Test 4: Should list all users")
    void testListAllUsers() {
        ResponseEntity<DtoCollectionResponse<UserDto>> response = restTemplate.exchange(
            getBaseUrl(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<UserDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getCollection());
    }


    private UserDto createTestUser(String username) {
        CredentialDto credentialDto = new CredentialDto();
        credentialDto.setUsername(username);
        credentialDto.setPassword("Test123!");
        credentialDto.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        credentialDto.setIsEnabled(true);
        credentialDto.setIsAccountNonExpired(true);
        credentialDto.setIsAccountNonLocked(true);
        credentialDto.setIsCredentialsNonExpired(true);
        
        UserDto userDto = new UserDto();
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail(username + "@test.com");
        userDto.setPhone("1234567890");
        userDto.setCredentialDto(credentialDto);
        
        return userDto;
    }
}