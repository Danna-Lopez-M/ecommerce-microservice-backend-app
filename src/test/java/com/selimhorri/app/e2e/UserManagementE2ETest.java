package com.selimhorri.app.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserManagementE2ETest {

    @LocalServerPort
    private int port;
    
    private static String authToken;
    private static Integer userId;
    private static Integer productId;
    private static Integer favoriteId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: User registration and profile setup")
    void testUserRegistrationAndProfileSetup() {
        // Register user
        String userJson = """
            {
                "username": "usermanager",
                "email": "usermanager@test.com",
                "password": "SecurePass123!",
                "firstName": "Jane",
                "lastName": "Smith"
            }
            """;
        
        userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
        .when()
            .post("/users/register")
        .then()
            .statusCode(201)
            .body("username", equalTo("usermanager"))
            .body("email", equalTo("usermanager@test.com"))
            .body("id", notNullValue())
        .extract()
            .path("id");
            
        System.out.println("User registered with ID: " + userId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: User login and authentication")
    void testUserLogin() {
        String loginJson = """
            {
                "username": "usermanager",
                "password": "SecurePass123!"
            }
            """;
        
        authToken = given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo("usermanager"))
        .extract()
            .path("token");
            
        System.out.println("User logged in, token: " + authToken);
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Update user profile information")
    void testUpdateUserProfile() {
        String updateJson = """
            {
                "firstName": "Jane Updated",
                "lastName": "Smith Updated",
                "phone": "555-9876",
                "preferences": "electronics,books"
            }
            """;
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(updateJson)
        .when()
            .put("/users/" + userId)
        .then()
            .statusCode(200)
            .body("firstName", equalTo("Jane Updated"))
            .body("lastName", equalTo("Smith Updated"))
            .body("phone", equalTo("555-9876"));
            
        System.out.println("User profile updated successfully");
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Browse products and add to favorites")
    void testBrowseProductsAndAddToFavorites() {
        // Browse products
        productId = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
        .extract()
            .path("[0].id");
        
        // Add to favorites
        String favoriteJson = String.format("""
            {
                "userId": %d,
                "productId": %d
            }
            """, userId, productId);
        
        favoriteId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(favoriteJson)
        .when()
            .post("/favorites")
        .then()
            .statusCode(201)
            .body("userId", equalTo(userId))
            .body("productId", equalTo(productId))
        .extract()
            .path("id");
            
        System.out.println("Product " + productId + " added to favorites");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: View user favorites and recommendations")
    void testViewUserFavoritesAndRecommendations() {
        // View favorites
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/favorites/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].userId", equalTo(userId))
            .body("[0].productId", equalTo(productId));
        
        // Get recommendations based on favorites
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/recommendations/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Favorites and recommendations viewed successfully");
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 6: Update user notification preferences")
    void testUpdateUserNotificationPreferences() {
        String preferencesJson = """
            {
                "emailNotifications": true,
                "smsNotifications": false,
                "pushNotifications": true,
                "marketingEmails": false
            }
            """;
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(preferencesJson)
        .when()
            .put("/users/" + userId + "/notification-preferences")
        .then()
            .statusCode(200)
            .body("emailNotifications", equalTo(true))
            .body("smsNotifications", equalTo(false))
            .body("pushNotifications", equalTo(true))
            .body("marketingEmails", equalTo(false));
            
        System.out.println("Notification preferences updated successfully");
    }

    @Test
    @Order(7)
    @DisplayName("E2E Test 7: View user order history")
    void testViewUserOrderHistory() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Order history viewed successfully");
    }

    @Test
    @Order(8)
    @DisplayName("E2E Test 8: Update user password")
    void testUpdateUserPassword() {
        String passwordJson = """
            {
                "currentPassword": "SecurePass123!",
                "newPassword": "NewSecurePass456!"
            }
            """;
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(passwordJson)
        .when()
            .put("/users/" + userId + "/password")
        .then()
            .statusCode(200)
            .body("message", equalTo("Password updated successfully"));
            
        System.out.println("Password updated successfully");
    }

    @Test
    @Order(9)
    @DisplayName("E2E Test 9: Login with new password")
    void testLoginWithNewPassword() {
        String loginJson = """
            {
                "username": "usermanager",
                "password": "NewSecurePass456!"
            }
            """;
        
        String newToken = given()
            .contentType(ContentType.JSON)
            .body(loginJson)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo("usermanager"))
        .extract()
            .path("token");
            
        System.out.println("Login with new password successful, new token: " + newToken);
    }

    @Test
    @Order(10)
    @DisplayName("E2E Test 10: Remove product from favorites")
    void testRemoveProductFromFavorites() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .delete("/favorites/" + favoriteId)
        .then()
            .statusCode(200)
            .body("message", equalTo("Favorite removed successfully"));
        
        // Verify favorite was removed
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/favorites/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
            
        System.out.println("Product removed from favorites successfully");
    }
}
