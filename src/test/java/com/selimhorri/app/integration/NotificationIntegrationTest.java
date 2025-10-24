package com.selimhorri.app.integration;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.NotificationDto;
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
class NotificationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Integration Test 1: Order creation triggers notification")
    void testOrderCreationTriggersNotification() {
        // Step 1: Create order
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(199.99);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Step 2: Verify notification was sent
        ResponseEntity<NotificationDto[]> notificationsResponse = restTemplate.getForEntity(
            "/api/notifications/user/1", NotificationDto[].class
        );
        
        assertEquals(HttpStatus.OK, notificationsResponse.getStatusCode());
        assertNotNull(notificationsResponse.getBody());
        
        // Step 3: Verify notification content
        NotificationDto[] notifications = notificationsResponse.getBody();
        assertTrue(notifications.length > 0);
        
        boolean orderNotificationFound = false;
        for (NotificationDto notification : notifications) {
            if (notification.getType().equals("ORDER_CREATED") && 
                notification.getOrderId().equals(orderId)) {
                orderNotificationFound = true;
                break;
            }
        }
        assertTrue(orderNotificationFound, "Order creation notification should be sent");
    }

    @Test
    @DisplayName("Integration Test 2: Payment processing triggers notification")
    void testPaymentProcessingTriggersNotification() {
        // Step 1: Create order and process payment
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(299.99);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Process payment
        String paymentJson = """
            {
                "orderId": %d,
                "amount": 299.99,
                "paymentMethod": "CREDIT_CARD"
            }
            """.formatted(orderId);
        
        ResponseEntity<String> paymentResponse = restTemplate.postForEntity(
            "/api/payments/process", paymentJson, String.class
        );
        
        assertEquals(HttpStatus.OK, paymentResponse.getStatusCode());
        
        // Step 2: Verify payment notification was sent
        ResponseEntity<NotificationDto[]> notificationsResponse = restTemplate.getForEntity(
            "/api/notifications/user/1", NotificationDto[].class
        );
        
        assertEquals(HttpStatus.OK, notificationsResponse.getStatusCode());
        assertNotNull(notificationsResponse.getBody());
        
        // Step 3: Verify payment notification content
        NotificationDto[] notifications = notificationsResponse.getBody();
        boolean paymentNotificationFound = false;
        for (NotificationDto notification : notifications) {
            if (notification.getType().equals("PAYMENT_SUCCESS") && 
                notification.getOrderId().equals(orderId)) {
                paymentNotificationFound = true;
                break;
            }
        }
        assertTrue(paymentNotificationFound, "Payment success notification should be sent");
    }

    @Test
    @DisplayName("Integration Test 3: Shipping status updates trigger notifications")
    void testShippingStatusUpdatesTriggerNotifications() {
        // Step 1: Create order, payment, and shipping
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(150.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        Integer orderId = orderResponse.getBody().getOrderId();
        
        // Process payment
        String paymentJson = """
            {
                "orderId": %d,
                "amount": 150.0,
                "paymentMethod": "CREDIT_CARD"
            }
            """.formatted(orderId);
        
        restTemplate.postForEntity("/api/payments/process", paymentJson, String.class);
        
        // Create shipping
        String shippingJson = """
            {
                "orderId": %d,
                "address": "123 Main St, City, State 12345"
            }
            """.formatted(orderId);
        
        ResponseEntity<String> shippingResponse = restTemplate.postForEntity(
            "/api/shipping", shippingJson, String.class
        );
        Integer shippingId = Integer.parseInt(shippingResponse.getBody());
        
        // Step 2: Update shipping status
        String updateShippingJson = """
            {
                "status": "SHIPPED",
                "trackingNumber": "TRK123456789"
            }
            """;
        
        ResponseEntity<String> updateResponse = restTemplate.putForEntity(
            "/api/shipping/" + shippingId, updateShippingJson, String.class
        );
        
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        
        // Step 3: Verify shipping notification was sent
        ResponseEntity<NotificationDto[]> notificationsResponse = restTemplate.getForEntity(
            "/api/notifications/user/1", NotificationDto[].class
        );
        
        assertEquals(HttpStatus.OK, notificationsResponse.getStatusCode());
        assertNotNull(notificationsResponse.getBody());
        
        // Step 4: Verify shipping notification content
        NotificationDto[] notifications = notificationsResponse.getBody();
        boolean shippingNotificationFound = false;
        for (NotificationDto notification : notifications) {
            if (notification.getType().equals("SHIPPING_UPDATE") && 
                notification.getOrderId().equals(orderId)) {
                shippingNotificationFound = true;
                break;
            }
        }
        assertTrue(shippingNotificationFound, "Shipping update notification should be sent");
    }

    @Test
    @DisplayName("Integration Test 4: User preferences affect notification delivery")
    void testUserPreferencesAffectNotificationDelivery() {
        // Step 1: Update user notification preferences
        String preferencesJson = """
            {
                "userId": 1,
                "emailNotifications": true,
                "smsNotifications": false,
                "pushNotifications": true
            }
            """;
        
        ResponseEntity<String> preferencesResponse = restTemplate.putForEntity(
            "/api/users/1/notification-preferences", preferencesJson, String.class
        );
        
        assertEquals(HttpStatus.OK, preferencesResponse.getStatusCode());
        
        // Step 2: Create order to trigger notification
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(99.99);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        
        // Step 3: Verify notification was sent via preferred channels
        ResponseEntity<NotificationDto[]> notificationsResponse = restTemplate.getForEntity(
            "/api/notifications/user/1", NotificationDto[].class
        );
        
        assertEquals(HttpStatus.OK, notificationsResponse.getStatusCode());
        assertNotNull(notificationsResponse.getBody());
        
        // Step 4: Verify notification channels
        NotificationDto[] notifications = notificationsResponse.getBody();
        assertTrue(notifications.length > 0);
        
        for (NotificationDto notification : notifications) {
            assertTrue(notification.getChannels().contains("EMAIL"));
            assertTrue(notification.getChannels().contains("PUSH"));
            assertFalse(notification.getChannels().contains("SMS"));
        }
    }

    @Test
    @DisplayName("Integration Test 5: Notification delivery failure handling")
    void testNotificationDeliveryFailureHandling() {
        // Step 1: Simulate notification service failure
        ResponseEntity<String> failureResponse = restTemplate.getForEntity(
            "/api/notifications/simulate-failure", String.class
        );
        
        assertEquals(HttpStatus.OK, failureResponse.getStatusCode());
        
        // Step 2: Create order to trigger notification
        OrderDto orderDto = new OrderDto();
        orderDto.setUserId(1);
        orderDto.setTotalAmount(75.0);
        
        ResponseEntity<OrderDto> orderResponse = restTemplate.postForEntity(
            "/api/orders", orderDto, OrderDto.class
        );
        
        assertEquals(HttpStatus.CREATED, orderResponse.getStatusCode());
        
        // Step 3: Verify notification retry mechanism
        ResponseEntity<String> retryResponse = restTemplate.getForEntity(
            "/api/notifications/retry-failed", String.class
        );
        
        assertEquals(HttpStatus.OK, retryResponse.getStatusCode());
        assertTrue(retryResponse.getBody().contains("retry"));
    }
}
