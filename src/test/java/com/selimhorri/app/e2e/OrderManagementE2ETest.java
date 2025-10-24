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
class OrderManagementE2ETest {

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
    @DisplayName("E2E Test 1: User registration and login")
    void testUserRegistrationAndLogin() {
        // Register user
        String userJson = """
            {
                "username": "ordermanager",
                "email": "ordermanager@test.com",
                "password": "SecurePass123!",
                "firstName": "Order",
                "lastName": "Manager"
            }
            """;
        
        userId = given()
            .contentType(ContentType.JSON)
            .body(userJson)
        .when()
            .post("/users/register")
        .then()
            .statusCode(201)
            .body("username", equalTo("ordermanager"))
            .body("id", notNullValue())
        .extract()
            .path("id");
        
        // Login
        String loginJson = """
            {
                "username": "ordermanager",
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
    @DisplayName("E2E Test 2: Add products to cart")
    void testAddProductsToCart() {
        // Get a product
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
                "quantity": 3
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
            .body("quantity", equalTo(3));
            
        System.out.println("Product added to cart successfully");
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: View cart contents")
    void testViewCartContents() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/cart/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].userId", equalTo(userId))
            .body("[0].productId", equalTo(productId))
            .body("[0].quantity", equalTo(3));
            
        System.out.println("Cart contents viewed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Create order from cart")
    void testCreateOrderFromCart() {
        orderId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
        .when()
            .post("/orders/checkout/" + userId)
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("userId", equalTo(userId))
            .body("id", notNullValue())
        .extract()
            .path("id");
            
        System.out.println("Order created successfully with ID: " + orderId);
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Process payment for order")
    void testProcessPaymentForOrder() {
        String paymentJson = String.format("""
            {
                "orderId": %d,
                "paymentMethod": "CREDIT_CARD",
                "cardNumber": "4111111111111111",
                "cardHolder": "Order Manager",
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
            
        System.out.println("Payment processed successfully with ID: " + paymentId);
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 6: Verify order status after payment")
    void testVerifyOrderStatusAfterPayment() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("PAID"))
            .body("id", equalTo(orderId))
            .body("userId", equalTo(userId));
            
        System.out.println("Order status verified as PAID");
    }

    @Test
    @Order(7)
    @DisplayName("E2E Test 7: Create shipping for order")
    void testCreateShippingForOrder() {
        String shippingJson = String.format("""
            {
                "orderId": %d,
                "address": "456 Order Street, City, ST 54321",
                "recipientName": "Order Manager",
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
            .body("orderId", equalTo(orderId))
            .body("trackingNumber", notNullValue())
        .extract()
            .path("id");
            
        System.out.println("Shipping created successfully with ID: " + shippingId);
    }

    @Test
    @Order(8)
    @DisplayName("E2E Test 8: Update shipping status")
    void testUpdateShippingStatus() {
        String updateShippingJson = """
            {
                "status": "SHIPPED",
                "trackingNumber": "TRK987654321"
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
            .body("status", equalTo("SHIPPED"))
            .body("trackingNumber", equalTo("TRK987654321"));
            
        System.out.println("Shipping status updated to SHIPPED");
    }

    @Test
    @Order(9)
    @DisplayName("E2E Test 9: View order history")
    void testViewOrderHistory() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/user/" + userId)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].id", equalTo(orderId))
            .body("[0].userId", equalTo(userId))
            .body("[0].status", equalTo("SHIPPED"));
            
        System.out.println("Order history viewed successfully");
    }

    @Test
    @Order(10)
    @DisplayName("E2E Test 10: Track order status")
    void testTrackOrderStatus() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/" + orderId + "/track")
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("status", equalTo("SHIPPED"))
            .body("trackingNumber", equalTo("TRK987654321"));
            
        System.out.println("Order tracking information retrieved successfully");
    }

    @Test
    @Order(11)
    @DisplayName("E2E Test 11: Cancel order")
    void testCancelOrder() {
        // Create another order to cancel
        String newOrderJson = """
            {
                "userId": %d,
                "totalAmount": 99.99,
                "status": "PENDING"
            }
            """.formatted(userId);
        
        Integer newOrderId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(newOrderJson)
        .when()
            .post("/orders")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
        .extract()
            .path("id");
        
        // Cancel the order
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .post("/orders/" + newOrderId + "/cancel")
        .then()
            .statusCode(200)
            .body("message", equalTo("Order cancelled successfully"));
        
        // Verify order is cancelled
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/" + newOrderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CANCELLED"));
            
        System.out.println("Order cancelled successfully");
    }

    @Test
    @Order(12)
    @DisplayName("E2E Test 12: Update order status to delivered")
    void testUpdateOrderStatusToDelivered() {
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
            .put("/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("DELIVERED"));
        
        // Verify final order status
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("DELIVERED"));
            
        System.out.println("Order status updated to DELIVERED successfully");
    }
}
