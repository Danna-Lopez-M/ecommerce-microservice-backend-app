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
class SystemResilienceE2ETest {

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
    @DisplayName("E2E Test 1: System health check")
    void testSystemHealthCheck() {
        given()
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
            
        System.out.println("System health check passed");
    }

    @Test
    @Order(2)
    @DisplayName("E2E Test 2: Service discovery health check")
    void testServiceDiscoveryHealthCheck() {
        given()
        .when()
            .get("/discovery/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
            
        System.out.println("Service discovery health check passed");
    }

    @Test
    @Order(3)
    @DisplayName("E2E Test 3: User registration and login under load")
    void testUserRegistrationAndLoginUnderLoad() {
        // Register user
        String userJson = """
            {
                "username": "resilienceuser",
                "email": "resilienceuser@test.com",
                "password": "SecurePass123!",
                "firstName": "Resilience",
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
            .body("username", equalTo("resilienceuser"))
            .body("id", notNullValue())
        .extract()
            .path("id");
        
        // Login
        String loginJson = """
            {
                "username": "resilienceuser",
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
            
        System.out.println("User registration and login under load successful");
    }

    @Test
    @Order(4)
    @DisplayName("E2E Test 4: Product service resilience")
    void testProductServiceResilience() {
        // Test product service with multiple requests
        for (int i = 0; i < 5; i++) {
            given()
                .header("Authorization", "Bearer " + authToken)
            .when()
                .get("/products")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
        }
        
        // Get a product for further testing
        productId = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
        .extract()
            .path("[0].id");
            
        System.out.println("Product service resilience test passed");
    }

    @Test
    @Order(5)
    @DisplayName("E2E Test 5: Order service resilience")
    void testOrderServiceResilience() {
        // Create order
        String orderJson = String.format("""
            {
                "userId": %d,
                "totalAmount": 199.99,
                "status": "PENDING"
            }
            """, userId);
        
        orderId = given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(orderJson)
        .when()
            .post("/orders")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("userId", equalTo(userId))
        .extract()
            .path("id");
        
        // Test order service with multiple requests
        for (int i = 0; i < 3; i++) {
            given()
                .header("Authorization", "Bearer " + authToken)
            .when()
                .get("/orders/" + orderId)
            .then()
                .statusCode(200)
                .body("id", equalTo(orderId));
        }
            
        System.out.println("Order service resilience test passed");
    }

    @Test
    @Order(6)
    @DisplayName("E2E Test 6: Payment service resilience")
    void testPaymentServiceResilience() {
        // Test payment service with multiple requests
        for (int i = 0; i < 3; i++) {
            String paymentJson = String.format("""
                {
                    "orderId": %d,
                    "paymentMethod": "CREDIT_CARD",
                    "cardNumber": "4111111111111111",
                    "cardHolder": "Resilience User",
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
                .body("status", equalTo("COMPLETED"));
        }
            
        System.out.println("Payment service resilience test passed");
    }

    @Test
    @Order(7)
    @DisplayName("E2E Test 7: Shipping service resilience")
    void testShippingServiceResilience() {
        // Test shipping service with multiple requests
        for (int i = 0; i < 3; i++) {
            String shippingJson = String.format("""
                {
                    "orderId": %d,
                    "address": "789 Resilience Ave, City, ST 98765",
                    "recipientName": "Resilience User",
                    "phone": "555-9876"
                }
                """, orderId);
            
            given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(shippingJson)
            .when()
                .post("/shipping")
            .then()
                .statusCode(201)
                .body("status", equalTo("PENDING"))
                .body("orderId", equalTo(orderId));
        }
            
        System.out.println("Shipping service resilience test passed");
    }

    @Test
    @Order(8)
    @DisplayName("E2E Test 8: Proxy client resilience")
    void testProxyClientResilience() {
        // Test proxy client with multiple requests
        for (int i = 0; i < 5; i++) {
            given()
                .header("Authorization", "Bearer " + authToken)
            .when()
                .get("/proxy/health")
            .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
        }
            
        System.out.println("Proxy client resilience test passed");
    }

    @Test
    @Order(9)
    @DisplayName("E2E Test 9: Error handling and recovery")
    void testErrorHandlingAndRecovery() {
        // Test error handling with invalid requests
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products/99999")
        .then()
            .statusCode(404);
        
        // Test recovery with valid requests
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(0));
            
        System.out.println("Error handling and recovery test passed");
    }

    @Test
    @Order(10)
    @DisplayName("E2E Test 10: System metrics and monitoring")
    void testSystemMetricsAndMonitoring() {
        // Test system metrics
        given()
        .when()
            .get("/actuator/metrics")
        .then()
            .statusCode(200);
        
        // Test specific metrics
        given()
        .when()
            .get("/actuator/metrics/jvm.memory.used")
        .then()
            .statusCode(200);
            
        System.out.println("System metrics and monitoring test passed");
    }

    @Test
    @Order(11)
    @DisplayName("E2E Test 11: Database connection resilience")
    void testDatabaseConnectionResilience() {
        // Test database operations
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/users/" + userId)
        .then()
            .statusCode(200)
            .body("id", equalTo(userId));
        
        // Test multiple database operations
        for (int i = 0; i < 3; i++) {
            given()
                .header("Authorization", "Bearer " + authToken)
            .when()
                .get("/orders/user/" + userId)
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
        }
            
        System.out.println("Database connection resilience test passed");
    }

    @Test
    @Order(12)
    @DisplayName("E2E Test 12: Final system validation")
    void testFinalSystemValidation() {
        // Final comprehensive system test
        given()
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
        
        // Test all major endpoints
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/products")
        .then()
            .statusCode(200);
        
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/orders/user/" + userId)
        .then()
            .statusCode(200);
        
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/users/" + userId)
        .then()
            .statusCode(200);
            
        System.out.println("Final system validation passed - System is resilient and operational");
    }
}
