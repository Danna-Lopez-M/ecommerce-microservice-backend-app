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
class CompleteShoppingE2ETest {

    @LocalServerPort
    private int port;
    
    private static String authToken;
    private static Integer userId;
    private static Integer productId;
    private static Integer orderId;
    private static Integer paymentId;
    private static Integer shippingId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @Order(1)
    @DisplayName("E2E Test 1: Complete shopping journey from registration to delivery")
    void testCompleteShoppingJourney() {
        // Step 1: User registration
        String userJson = """
            {
                "username": "shopper",
                "email": "shopper@test.com",
                "password": "SecurePass123!",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;
        
        userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
        .when()
            .post("/users/register")
        .then()
            .statusCode(201)
            .body("username", equalTo("shopper"))
            .body("email", equalTo("shopper@test.com"))
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
                "username": "shopper",
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
            .body("username", equalTo("shopper"))
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
    @DisplayName("E2E Test 4: Complete checkout and payment")
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
                "cardHolder": "John Doe",
                "expiryDate": "12/25",
                "cvv": "123"
            }
            """, orderId);
        
        paymentId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(paymentJson)
        .when()
            .post("/payments/process")
        .then()
            .statusCode(200)
            .body("status", equalTo("COMPLETED"))
            .body("orderId", equalTo(orderId))
        .extract()
            .path("id");
            
        System.out.println("Order " + orderId + " paid successfully, payment ID: " + paymentId);
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Create shipping and track delivery")
    void testCreateShippingAndTrackDelivery() {
        // Create shipping
        String shippingJson = String.format("""
            {
                "orderId": %d,
                "address": "123 Main Street, Anytown, ST 12345",
                "recipientName": "John Doe",
                "phone": "555-1234"
            }
            """, orderId);
        
        shippingId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(shippingJson)
        .when()
            .post("/shipping")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("trackingNumber", notNullValue())
            .body("orderId", equalTo(orderId))
        .extract()
            .path("id");
            
        System.out.println("Shipping created with ID: " + shippingId);
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 6: Track order and shipping status")
    void testTrackOrderAndShippingStatus() {
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
    @Order(7)
    @DisplayName("E2E Test 7: User views order history and receives notifications")
    void testUserViewsOrderHistoryAndReceivesNotifications() {
        // View order history
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].id", equalTo(orderId))
            .body("[0].userId", equalTo(userId));
        
        // Check notifications
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/notifications/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
            
        System.out.println("Order history and notifications verified");
    }

    @Test
    @Order(8)
    @DisplayName("E2E Test 8: Update shipping status to delivered")
    void testUpdateShippingStatusToDelivered() {
        // Update shipping status to SHIPPED
        String updateShippingJson = """
            {
                "status": "SHIPPED",
                "trackingNumber": "TRK123456789"
            }
            """;
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(updateShippingJson)
        .when()
            .put("/shipping/" + shippingId)
        .then()
            .statusCode(200)
            .body("status", equalTo("SHIPPED"));
        
        // Update to DELIVERED
        String deliveredJson = """
            {
                "status": "DELIVERED"
            }
            """;
        
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(deliveredJson)
        .when()
            .put("/shipping/" + shippingId)
        .then()
            .statusCode(200)
            .body("status", equalTo("DELIVERED"));
            
        System.out.println("Shipping status updated to DELIVERED");
    }

    @Test
    @Order(9)
    @DisplayName("E2E Test 9: Verify final order status")
    void testVerifyFinalOrderStatus() {
        // Verify order status is DELIVERED
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("DELIVERED"))
            .body("id", equalTo(orderId));
        
        // Verify shipping status
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/shipping/" + shippingId)
        .then()
            .statusCode(200)
            .body("status", equalTo("DELIVERED"))
            .body("orderId", equalTo(orderId));
            
        System.out.println("Complete shopping journey verified successfully");
    }
}
