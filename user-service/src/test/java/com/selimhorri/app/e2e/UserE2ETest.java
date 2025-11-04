package com.selimhorri.app.e2e;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.domain.RoleBasedAuthority;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static Integer userId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/user-service/api/users";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete user registration flow")
    void testCompleteUserRegistration() {
        CredentialDto credentialDto = new CredentialDto();
        credentialDto.setUsername("e2euser");
        credentialDto.setPassword("E2ETest123!");
        credentialDto.setRoleBasedAuthority(RoleBasedAuthority.ROLE_USER);
        credentialDto.setIsEnabled(true);
        credentialDto.setIsAccountNonExpired(true);
        credentialDto.setIsAccountNonLocked(true);
        credentialDto.setIsCredentialsNonExpired(true);
        
        UserDto userDto = new UserDto();
        userDto.setFirstName("E2E");
        userDto.setLastName("Test");
        userDto.setEmail("e2e@test.com");
        userDto.setPhone("9876543210");
        userDto.setCredentialDto(credentialDto);
        
        ResponseEntity<UserDto> response = restTemplate.postForEntity(
            baseUrl, userDto, UserDto.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        userId = response.getBody().getUserId();
        assertEquals("e2euser", response.getBody().getCredentialDto().getUsername());
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: User profile update flow")
    void testUserProfileUpdate() {
        assertNotNull(userId, "User must be created first");
        
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
            baseUrl + "/" + userId, UserDto.class
        );
        
        UserDto user = getResponse.getBody();
        user.setFirstName("Updated");
        user.setPhone("1111111111");
        
        restTemplate.put(baseUrl, user);
        
        ResponseEntity<UserDto> updatedResponse = restTemplate.getForEntity(
            baseUrl + "/" + userId, UserDto.class
        );
        
        assertEquals("Updated", updatedResponse.getBody().getFirstName());
        assertEquals("1111111111", updatedResponse.getBody().getPhone());
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: User search and retrieval flow")
    void testUserSearchAndRetrieval() {
        ResponseEntity<DtoCollectionResponse<UserDto>> allUsersResponse = restTemplate.exchange(
            baseUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<DtoCollectionResponse<UserDto>>() {}
        );
        
        assertEquals(HttpStatus.OK, allUsersResponse.getStatusCode());
        assertNotNull(allUsersResponse.getBody());
        assertNotNull(allUsersResponse.getBody().getCollection());
        assertTrue(allUsersResponse.getBody().getCollection().size() > 0);
        
        ResponseEntity<UserDto> specificUserResponse = restTemplate.getForEntity(
            baseUrl + "/" + userId, UserDto.class
        );
        
        assertEquals(HttpStatus.OK, specificUserResponse.getStatusCode());
        assertEquals(userId, specificUserResponse.getBody().getUserId());
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: User credential validation flow")
    void testUserCredentialValidation() {
        ResponseEntity<UserDto> response = restTemplate.getForEntity(
            baseUrl + "/" + userId, UserDto.class
        );
        
        UserDto user = response.getBody();
        assertNotNull(user.getCredentialDto());
        assertTrue(user.getCredentialDto().getIsEnabled());
        assertTrue(user.getCredentialDto().getIsAccountNonExpired());
        assertTrue(user.getCredentialDto().getIsAccountNonLocked());
        assertEquals(RoleBasedAuthority.ROLE_USER, user.getCredentialDto().getRoleBasedAuthority());
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: User deletion flow")
    void testUserDeletion() {
        restTemplate.delete(baseUrl + "/" + userId);
        
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/" + userId, String.class
        );
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}