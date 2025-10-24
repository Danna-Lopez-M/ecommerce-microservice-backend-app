from locust import HttpUser, task, between, SequentialTaskSet
import json
import random

class UserBehavior(SequentialTaskSet):
    """Simulates realistic user behavior in the e-commerce system"""
    
    def on_start(self):
        """Execute on user start - Register and login"""
        self.username = f"user_{random.randint(1000, 9999)}"
        self.email = f"{self.username}@test.com"
        self.token = None
        self.product_id = None
        self.order_id = None
        
        # Register user
        self.register_user()
        # Login
        self.login_user()
    
    def register_user(self):
        """Register a new user"""
        payload = {
            "username": self.username,
            "email": self.email,
            "password": "Test123!",
            "firstName": "Load",
            "lastName": "Test"
        }
        
        with self.client.post("/api/users/register", 
                             json=payload,
                             catch_response=True) as response:
            if response.status_code == 201:
                self.user_id = response.json().get("id")
                response.success()
            else:
                response.failure(f"Registration failed: {response.status_code}")
    
    def login_user(self):
        """Login and get authentication token"""
        payload = {
            "username": self.username,
            "password": "Test123!"
        }
        
        with self.client.post("/api/auth/login",
                             json=payload,
                             catch_response=True) as response:
            if response.status_code == 200:
                self.token = response.json().get("token")
                response.success()
            else:
                response.failure(f"Login failed: {response.status_code}")
    
    @task(5)
    def browse_products(self):
        """Browse product catalog - High frequency"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        with self.client.get("/api/products",
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                products = response.json()
                if products:
                    self.product_id = products[0].get("id")
                response.success()
            else:
                response.failure(f"Browse failed: {response.status_code}")
    
    @task(3)
    def view_product_details(self):
        """View individual product details"""
        if not self.product_id:
            return
        
        headers = {"Authorization": f"Bearer {self.token}"}
        
        with self.client.get(f"/api/products/{self.product_id}",
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Product view failed: {response.status_code}")
    
    @task(2)
    def add_to_cart(self):
        """Add product to shopping cart"""
        if not self.product_id:
            return
        
        headers = {"Authorization": f"Bearer {self.token}"}
        payload = {
            "userId": self.user_id,
            "productId": self.product_id,
            "quantity": random.randint(1, 3)
        }
        
        with self.client.post("/api/cart/items",
                             json=payload,
                             headers=headers,
                             catch_response=True) as response:
            if response.status_code == 201:
                response.success()
            else:
                response.failure(f"Add to cart failed: {response.status_code}")
    
    @task(1)
    def checkout_and_pay(self):
        """Complete checkout and payment - Lower frequency"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        # Create order
        with self.client.post(f"/api/orders/checkout/{self.user_id}",
                             headers=headers,
                             catch_response=True) as response:
            if response.status_code == 201:
                self.order_id = response.json().get("id")
                response.success()
            else:
                response.failure(f"Checkout failed: {response.status_code}")
                return
        
        # Process payment
        payment_payload = {
            "orderId": self.order_id,
            "paymentMethod": "CREDIT_CARD",
            "cardNumber": "4111111111111111",
            "cardHolder": "Load Test",
            "expiryDate": "12/25",
            "cvv": "123"
        }
        
        with self.client.post("/api/payments/process",
                             json=payment_payload,
                             headers=headers,
                             catch_response=True,
                             name="/api/payments/process") as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Payment failed: {response.status_code}")
    
    @task(1)
    def view_order_history(self):
        """View user's order history"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        with self.client.get(f"/api/orders/user/{self.user_id}",
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Order history failed: {response.status_code}")


class EcommerceUser(HttpUser):
    """Main Locust user class"""
    tasks = [UserBehavior]
    wait_time = between(1, 3)  # Wait 1-3 seconds between tasks
    host = "http://localhost:8080"  # Base URL


class StressTestUser(HttpUser):
    """Stress test with higher load"""
    tasks = [UserBehavior]
    wait_time = between(0.5, 1.5)  # Shorter wait for stress testing
    host = "http://localhost:8080"


# Performance thresholds for CI/CD
class PerformanceThresholds:
    MAX_RESPONSE_TIME_MS = 2000  # 2 seconds
    MAX_ERROR_RATE = 0.05  # 5%