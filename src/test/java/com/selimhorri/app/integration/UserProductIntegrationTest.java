package com.selimhorri.app.integration;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserProductIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Integration Test 1: User can browse products after authentication")
    void testUserBrowseProductsAfterAuth() {
        // Step 1: Create user
        UserDto userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        
        ResponseEntity<UserDto> userResponse = restTemplate.postForEntity(
            "/api/users/register", userDto, UserDto.class
        );
        
        assertEquals(HttpStatus.CREATED, userResponse.getStatusCode());
        assertNotNull(userResponse.getBody());
        Integer userId = userResponse.getBody().getUserId();
        
        // Step 2: Login user
        String loginJson = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;
        
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
            "/api/auth/login", loginJson, String.class
        );
        
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("token"));
        
        // Step 3: Browse products with authentication
        ResponseEntity<ProductDto[]> productsResponse = restTemplate.getForEntity(
            "/api/products", ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, productsResponse.getStatusCode());
        assertNotNull(productsResponse.getBody());
    }

    @Test
    @DisplayName("Integration Test 2: Product inventory updates when user adds to cart")
    void testProductInventoryUpdateOnCartAdd() {
        // Step 1: Get initial product inventory
        ResponseEntity<ProductDto[]> productsResponse = restTemplate.getForEntity(
            "/api/products", ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, productsResponse.getStatusCode());
        ProductDto product = productsResponse.getBody()[0];
        Integer initialStock = product.getQuantity();
        
        // Step 2: Add product to cart
        String cartItemJson = String.format("""
            {
                "userId": 1,
                "productId": %d,
                "quantity": 2
            }
            """, product.getProductId());
        
        ResponseEntity<String> cartResponse = restTemplate.postForEntity(
            "/api/cart/items", cartItemJson, String.class
        );
        
        assertEquals(HttpStatus.CREATED, cartResponse.getStatusCode());
        
        // Step 3: Verify product inventory decreased
        ResponseEntity<ProductDto> updatedProductResponse = restTemplate.getForEntity(
            "/api/products/" + product.getProductId(), ProductDto.class
        );
        
        assertEquals(HttpStatus.OK, updatedProductResponse.getStatusCode());
        ProductDto updatedProduct = updatedProductResponse.getBody();
        assertEquals(initialStock - 2, updatedProduct.getQuantity());
    }

    @Test
    @DisplayName("Integration Test 3: User profile updates affect product recommendations")
    void testUserProfileAffectsProductRecommendations() {
        // Step 1: Update user profile with preferences
        UserDto userUpdate = new UserDto();
        userUpdate.setUserId(1);
        userUpdate.setPreferences("electronics");
        
        ResponseEntity<UserDto> updateResponse = restTemplate.putForEntity(
            "/api/users/1", userUpdate, UserDto.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        
        // Step 2: Get personalized product recommendations
        ResponseEntity<ProductDto[]> recommendationsResponse = restTemplate.getForEntity(
            "/api/products/recommendations/1", ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, recommendationsResponse.getStatusCode());
        assertNotNull(recommendationsResponse.getBody());
        
        // Step 3: Verify recommendations are relevant to user preferences
        ProductDto[] recommendations = recommendationsResponse.getBody();
        assertTrue(recommendations.length > 0);
    }

    @Test
    @DisplayName("Integration Test 4: Product search with user context")
    void testProductSearchWithUserContext() {
        // Step 1: User performs search
        ResponseEntity<ProductDto[]> searchResponse = restTemplate.getForEntity(
            "/api/products/search?query=laptop&userId=1", ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        assertNotNull(searchResponse.getBody());
        
        // Step 2: Verify search results are personalized
        ProductDto[] results = searchResponse.getBody();
        assertTrue(results.length > 0);
        
        // Step 3: Verify search history is recorded
        ResponseEntity<String> historyResponse = restTemplate.getForEntity(
            "/api/users/1/search-history", String.class
        );
        
        assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
        assertTrue(historyResponse.getBody().contains("laptop"));
    }

    @Test
    @DisplayName("Integration Test 5: User favorites sync with product service")
    void testUserFavoritesSyncWithProductService() {
        // Step 1: Add product to user favorites
        String favoriteJson = """
            {
                "userId": 1,
                "productId": 1
            }
            """;
        
        ResponseEntity<String> favoriteResponse = restTemplate.postForEntity(
            "/api/favorites", favoriteJson, String.class
        );
        
        assertEquals(HttpStatus.CREATED, favoriteResponse.getStatusCode());
        
        // Step 2: Verify favorite is recorded in product service
        ResponseEntity<ProductDto> productResponse = restTemplate.getForEntity(
            "/api/products/1", ProductDto.class
        );
        
        assertEquals(HttpStatus.OK, productResponse.getStatusCode());
        ProductDto product = productResponse.getBody();
        assertTrue(product.getFavoriteCount() > 0);
        
        // Step 3: Get user's favorite products
        ResponseEntity<ProductDto[]> favoritesResponse = restTemplate.getForEntity(
            "/api/users/1/favorites", ProductDto[].class
        );
        
        assertEquals(HttpStatus.OK, favoritesResponse.getStatusCode());
        assertNotNull(favoritesResponse.getBody());
    }
}
