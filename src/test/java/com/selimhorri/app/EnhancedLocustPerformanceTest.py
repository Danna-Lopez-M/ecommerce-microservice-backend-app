#!/usr/bin/env python3
"""
Enhanced Locust Performance Tests for E-commerce Microservices
Comprehensive performance testing with realistic user scenarios
"""

from locust import HttpUser, task, between, SequentialTaskSet
import json
import random
import time
from datetime import datetime, timedelta

class EcommerceUserBehavior(SequentialTaskSet):
    """Enhanced user behavior simulation for e-commerce system"""
    
    def on_start(self):
        """Execute on user start - Enhanced registration and login"""
        self.username = f"perfuser_{random.randint(10000, 99999)}"
        self.email = f"{self.username}@performance.com"
        self.token = None
        self.user_id = None
        self.product_id = None
        self.order_id = None
        self.cart_items = []
        
        # Register user
        self.register_user()
        # Login
        self.login_user()
    
    def register_user(self):
        """Register a new user with enhanced data"""
        payload = {
            "username": self.username,
            "email": self.email,
            "password": "PerfTest123!",
            "firstName": "Performance",
            "lastName": "Tester",
            "phone": f"555-{random.randint(1000, 9999)}",
            "preferences": random.choice(["electronics", "books", "clothing", "home"])
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
            "password": "PerfTest123!"
        }
        
        with self.client.post("/api/auth/login",
                             json=payload,
                             catch_response=True) as response:
            if response.status_code == 200:
                self.token = response.json().get("token")
                response.success()
            else:
                response.failure(f"Login failed: {response.status_code}")
    
    @task(8)
    def browse_products(self):
        """Browse product catalog - High frequency with filtering"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        # Random filtering options
        filters = []
        if random.random() < 0.3:
            filters.append(f"category={random.choice(['electronics', 'books', 'clothing'])}")
        if random.random() < 0.2:
            filters.append(f"minPrice={random.randint(10, 100)}")
        if random.random() < 0.2:
            filters.append(f"maxPrice={random.randint(100, 500)}")
        
        filter_str = "&".join(filters)
        url = f"/api/products?{filter_str}" if filter_str else "/api/products"
        
        with self.client.get(url,
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                products = response.json()
                if products:
                    self.product_id = products[0].get("id")
                response.success()
            else:
                response.failure(f"Browse failed: {response.status_code}")
    
    @task(6)
    def search_products(self):
        """Search products with various queries"""
        headers = {"Authorization": f"Bearer {self.token}"}
        search_queries = ["laptop", "book", "shirt", "phone", "tablet", "shoes"]
        query = random.choice(search_queries)
        
        with self.client.get(f"/api/products/search?query={query}",
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Search failed: {response.status_code}")
    
    @task(5)
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
    
    @task(4)
    def add_to_cart(self):
        """Add product to shopping cart with random quantities"""
        if not self.product_id:
            return
        
        headers = {"Authorization": f"Bearer {self.token}"}
        quantity = random.randint(1, 5)
        
        payload = {
            "userId": self.user_id,
            "productId": self.product_id,
            "quantity": quantity
        }
        
        with self.client.post("/api/cart/items",
                             json=payload,
                             headers=headers,
                             catch_response=True) as response:
            if response.status_code == 201:
                self.cart_items.append({
                    "productId": self.product_id,
                    "quantity": quantity
                })
                response.success()
            else:
                response.failure(f"Add to cart failed: {response.status_code}")
    
    @task(3)
    def view_cart(self):
        """View shopping cart contents"""
        headers = {"Authorization": f"Bearer {self.token}"}
        
        with self.client.get(f"/api/cart/user/{self.user_id}",
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"View cart failed: {response.status_code}")
    
    @task(2)
    def update_cart_item(self):
        """Update cart item quantity"""
        if not self.cart_items:
            return
        
        headers = {"Authorization": f"Bearer {self.token}"}
        item = random.choice(self.cart_items)
        new_quantity = random.randint(1, 10)
        
        payload = {
            "quantity": new_quantity
        }
        
        with self.client.put(f"/api/cart/items/{item['productId']}",
                            json=payload,
                            headers=headers,
                            catch_response=True) as response:
            if response.status_code == 200:
                item["quantity"] = new_quantity
                response.success()
            else:
                response.failure(f"Update cart failed: {response.status_code}")
    
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
            "cardHolder": "Performance Tester",
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
    
    @task(1)
    def add_to_favorites(self):
        """Add product to favorites"""
        if not self.product_id:
            return
        
        headers = {"Authorization": f"Bearer {self.token}"}
        payload = {
            "userId": self.user_id,
            "productId": self.product_id
        }
        
        with self.client.post("/api/favorites",
                             json=payload,
                             headers=headers,
                             catch_response=True) as response:
            if response.status_code == 201:
                response.success()
            else:
                response.failure(f"Add to favorites failed: {response.status_code}")


class EcommerceUser(HttpUser):
    """Main Locust user class for normal load"""
    tasks = [EcommerceUserBehavior]
    wait_time = between(1, 3)  # Wait 1-3 seconds between tasks
    host = "http://localhost:8080"  # Base URL


class StressTestUser(HttpUser):
    """Stress test with higher load and shorter wait times"""
    tasks = [EcommerceUserBehavior]
    wait_time = between(0.5, 1.5)  # Shorter wait for stress testing
    host = "http://localhost:8080"


class SpikeTestUser(HttpUser):
    """Spike test with very high load"""
    tasks = [EcommerceUserBehavior]
    wait_time = between(0.1, 0.5)  # Very short wait for spike testing
    host = "http://localhost:8080"


class EnduranceTestUser(HttpUser):
    """Endurance test with sustained load"""
    tasks = [EcommerceUserBehavior]
    wait_time = between(2, 5)  # Longer wait for endurance testing
    host = "http://localhost:8080"


# Performance thresholds for CI/CD
class PerformanceThresholds:
    MAX_RESPONSE_TIME_MS = 2000  # 2 seconds
    MAX_ERROR_RATE = 0.05  # 5%
    MIN_RPS = 10  # Minimum requests per second
    MAX_95_PERCENTILE = 3000  # 3 seconds


# Test scenarios
class TestScenarios:
    NORMAL_LOAD = {
        "users": 50,
        "spawn_rate": 5,
        "run_time": "5m"
    }
    
    STRESS_LOAD = {
        "users": 200,
        "spawn_rate": 10,
        "run_time": "10m"
    }
    
    SPIKE_LOAD = {
        "users": 500,
        "spawn_rate": 50,
        "run_time": "2m"
    }
    
    ENDURANCE_LOAD = {
        "users": 100,
        "spawn_rate": 2,
        "run_time": "30m"
    }


# Utility functions for test execution
def run_performance_test(scenario_name, scenario_config):
    """Run performance test with specific scenario"""
    print(f"Running {scenario_name} test with config: {scenario_config}")
    # This would be called from CI/CD pipeline
    pass


def validate_performance_metrics(stats_file):
    """Validate performance metrics against thresholds"""
    import csv
    
    with open(stats_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row['Type'] == 'Aggregated':
                avg_response = float(row['Average Response Time'])
                failure_rate = float(row['Failure Rate'])
                rps = float(row['Requests/s'])
                
                print(f"Performance Metrics:")
                print(f"  Average Response Time: {avg_response}ms")
                print(f"  Failure Rate: {failure_rate}%")
                print(f"  Requests per Second: {rps}")
                
                # Validate thresholds
                if avg_response > PerformanceThresholds.MAX_RESPONSE_TIME_MS:
                    print(f"❌ Average response time exceeds threshold")
                    return False
                if failure_rate > PerformanceThresholds.MAX_ERROR_RATE * 100:
                    print(f"❌ Failure rate exceeds threshold")
                    return False
                if rps < PerformanceThresholds.MIN_RPS:
                    print(f"❌ RPS below threshold")
                    return False
                
                print("✅ All performance thresholds met")
                return True
    
    return False


if __name__ == "__main__":
    print("Enhanced Locust Performance Tests for E-commerce Microservices")
    print("Available test scenarios:")
    for scenario, config in TestScenarios.__dict__.items():
        if not scenario.startswith('_'):
            print(f"  {scenario}: {config}")
