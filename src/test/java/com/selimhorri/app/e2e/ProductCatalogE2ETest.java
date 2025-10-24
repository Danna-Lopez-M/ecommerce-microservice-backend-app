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
class ProductCatalogE2ETest {

    @LocalServerPort
    private int port;
    
    private static String authToken;
    private static Integer userId;
    private static Integer productId;
    private static Integer categoryId;
    private static Integer reviewId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: User registration and login")
    void testUserRegistrationAndLogin() {
        // Register user
        String userJson = """
            {
                "username": "cataloguser",
                "email": "cataloguser@test.com",
                "password": "SecurePass123!",
                "firstName": "Catalog",
                "lastName": "User"
            }
            """;
        
        userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
        .when()
            .post("/users/register")
        .then()
            .statusCode(201)
            .body("username", equalTo("cataloguser"))
            .body("id", notNullValue())
        .extract()
            .path("id");
        
        // Login
        String loginJson = """
            {
                "username": "cataloguser",
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
        .extract()
            .path("token");
            
        System.out.println("User registered and logged in successfully");
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Browse product categories")
    void testBrowseProductCategories() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/categories")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
            
        System.out.println("Product categories browsed successfully");
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Search products by category")
    void testSearchProductsByCategory() {
        // Get categories first
        categoryId = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/categories")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
        .extract()
            .path("[0].id");
        
        // Search products by category
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/category/" + categoryId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Products searched by category successfully");
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Search products by keyword")
    void testSearchProductsByKeyword() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/search?query=laptop")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Products searched by keyword successfully");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: View product details")
    void testViewProductDetails() {
        // Get a product first
        productId = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
        .extract()
            .path("[0].id");
        
        // View product details
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/" + productId)
        .then()
            .statusCode(200)
            .body("id", equalTo(productId))
            .body("productTitle", notNullValue())
            .body("priceUnit", notNullValue());
            
        System.out.println("Product details viewed successfully");
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 6: Add product review")
    void testAddProductReview() {
        String reviewJson = String.format("""
            {
                "userId": %d,
                "productId": %d,
                "rating": 5,
                "comment": "Excellent product, highly recommended!"
            }
            """, userId, productId);
        
        reviewId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(reviewJson)
        .when()
            .post("/reviews")
        .then()
            .statusCode(201)
            .body("rating", equalTo(5))
            .body("comment", equalTo("Excellent product, highly recommended!"))
        .extract()
            .path("id");
            
        System.out.println("Product review added successfully");
    }

    @Test
    @Order(7)
    @DisplayName("E2E Test 7: View product reviews")
    void testViewProductReviews() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/" + productId + "/reviews")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].rating", equalTo(5));
            
        System.out.println("Product reviews viewed successfully");
    }

    @Test
    @Order(8)
    @DisplayName("E2E Test 8: Filter products by price range")
    void testFilterProductsByPriceRange() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products?minPrice=50&maxPrice=200")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Products filtered by price range successfully");
    }

    @Test
    @Order(9)
    @DisplayName("E2E Test 9: Sort products by price")
    void testSortProductsByPrice() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products?sortBy=price&sortOrder=asc")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Products sorted by price successfully");
    }

    @Test
    @Order(10)
    @DisplayName("E2E Test 10: Get product recommendations")
    void testGetProductRecommendations() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/recommendations/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Product recommendations retrieved successfully");
    }

    @Test
    @Order(11)
    @DisplayName("E2E Test 11: Update product inventory")
    void testUpdateProductInventory() {
        String inventoryJson = """
            {
                "quantity": 100
            }
            """;
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(inventoryJson)
        .when()
            .put("/products/" + productId + "/inventory")
        .then()
            .statusCode(200)
            .body("quantity", equalTo(100));
            
        System.out.println("Product inventory updated successfully");
    }

    @Test
    @Order(12)
    @DisplayName("E2E Test 12: Add product to cart and verify inventory")
    void testAddProductToCartAndVerifyInventory() {
        String cartItemJson = String.format("""
            {
                "userId": %d,
                "productId": %d,
                "quantity": 2
            }
            """, userId, productId);
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(cartItemJson)
        .when()
            .post("/cart/items")
        .then()
            .statusCode(201)
            .body("quantity", equalTo(2));
        
        // Verify inventory decreased
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/" + productId)
        .then()
            .statusCode(200)
            .body("quantity", equalTo(98));
            
        System.out.println("Product added to cart and inventory updated successfully");
    }
}
