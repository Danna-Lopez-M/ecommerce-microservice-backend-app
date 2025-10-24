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
class EcommerceE2ETest {

    @LocalServerPort
    private int port;
    
    private static String authToken;
    private static Integer userId;
    private static Integer productId;
    private static Integer orderId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete user registration flow")
    void testUserRegistration() {
        String userJson = """
            {
                "username": "e2euser",
                "email": "e2e@test.com",
                "password": "Test123!",
                "firstName": "E2E",
                "lastName": "Test"
            }
            """;
        
        userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
        .when()
            .post("/users/register")
        .then()
            .statusCode(201)
            .body("username", equalTo("e2euser"))
            .body("email", equalTo("e2e@test.com"))
            .body("id", notNullValue())
        .extract()
            .path("id");
            
        System.out.println("User registered with ID: " + userId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: User login and authentication flow")
    void testUserLogin() {
        String loginJson = """
            {
                "username": "e2euser",
                "password": "Test123!"
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
            .body("username", equalTo("e2euser"))
        .extract()
            .path("token");
            
        System.out.println("User logged in, token: " + authToken);
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: Browse products and add to cart")
    void testBrowseAndAddToCart() {
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
        
        // Add to cart
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
            
        System.out.println("Product " + productId + " added to cart");
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Complete checkout and payment flow")
    void testCheckoutAndPayment() {
        // Create order from cart
        orderId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
        .when()
            .post("/orders/checkout/" + userId)
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("id", notNullValue())
        .extract()
            .path("id");
        
        // Process payment
        String paymentJson = String.format("""
            {
                "orderId": %d,
                "paymentMethod": "CREDIT_CARD",
                "cardNumber": "4111111111111111",
                "cardHolder": "E2E Test",
                "expiryDate": "12/25",
                "cvv": "123"
            }
            """, orderId);
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(paymentJson)
        .when()
            .post("/payments/process")
        .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("orderId", equalTo(orderId));
            
        System.out.println("Order " + orderId + " paid successfully");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Track order and shipping status")
    void testOrderTracking() {
        // Verify order status
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("PAID"))
            .body("id", equalTo(orderId));
        
        // Check shipping status
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/shipping/order/" + orderId)
        .then()
            .statusCode(200)
            .body("status", oneOf("PENDING", "PROCESSING", "SHIPPED"))
            .body("orderId", equalTo(orderId))
            .body("trackingNumber", notNullValue());
            
        System.out.println("Order tracking verified for order: " + orderId);
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 6: User views order history")
    void testOrderHistory() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].id", equalTo(orderId))
            .body("[0].userId", equalTo(userId));
    }
}