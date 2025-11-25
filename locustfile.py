"""
Locust performance tests that exercise the API Gateway against the microservices.

Default host: http://localhost:8080 (override with --host or LOCUST_HOST env var)
Endpoints used:
- /user-service/api/users
- /order-service/api/carts
- /product-service/api/products
- /order-service/api/orders
- /payment-service/api/payments
"""

import os
import random
import string
from datetime import datetime, timezone

from locust import HttpUser, between, task


def _rand(prefix: str, length: int = 6) -> str:
    """Short helper to build deterministic-but-unique strings."""
    suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=length))
    return f"{prefix}-{suffix}"


class GatewayUser(HttpUser):
    wait_time = between(0.5, 2.0)
    host = os.getenv("LOCUST_HOST", "http://localhost:8080")

    def on_start(self) -> None:
        self.user_id = None
        self.cart_id = None
        self.last_product_id = None
        self.last_order_id = None

        self._create_user()
        self._create_cart()
        self._prefetch_products()

    # --------- Setup helpers ---------
    def _create_user(self) -> None:
        payload = {
            "firstName": _rand("Locust"),
            "lastName": "User",
            "email": f"{_rand('locust')}@test.com",
            "phone": f"+1{random.randint(2000000000, 9999999999)}",
            # Field name must be "credential" (not credentialDto) to match @JsonProperty
            "credential": {
                "username": _rand("locust"),
                "password": "Password123!",
                "roleBasedAuthority": "ROLE_USER",
                "isEnabled": True,
                "isAccountNonExpired": True,
                "isAccountNonLocked": True,
                "isCredentialsNonExpired": True,
            },
        }

        with self.client.post(
            "/user-service/api/users",
            json=payload,
            name="users:create",
            catch_response=True,
        ) as response:
            if response.status_code in (200, 201):
                body = response.json()
                self.user_id = body.get("userId")
                response.success()
            else:
                response.failure(f"User creation failed ({response.status_code})")

    def _create_cart(self) -> None:
        if not self.user_id:
            return

        payload = {"userId": self.user_id}
        with self.client.post(
            "/order-service/api/carts",
            json=payload,
            name="carts:create",
            catch_response=True,
        ) as response:
            if response.status_code in (200, 201):
                body = response.json()
                self.cart_id = body.get("cartId")
                response.success()
            else:
                response.failure(f"Cart creation failed ({response.status_code})")

    def _prefetch_products(self) -> None:
        with self.client.get(
            "/product-service/api/products",
            name="products:list",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                data = response.json()
                collection = data.get("collection") or []
                if collection:
                    self.last_product_id = collection[0].get("productId")
                response.success()
            else:
                response.failure(f"Product list failed ({response.status_code})")

    # --------- Tasks ---------
    @task(5)
    def list_products(self):
        with self.client.get(
            "/product-service/api/products",
            name="products:list",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                data = response.json()
                collection = data.get("collection") or []
                if collection:
                    self.last_product_id = random.choice(collection).get("productId")
                response.success()
            else:
                response.failure(f"Product list failed ({response.status_code})")

    @task(3)
    def view_product(self):
        if not self.last_product_id:
            return

        with self.client.get(
            f"/product-service/api/products/{self.last_product_id}",
            name="products:detail",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Product detail failed ({response.status_code})")

    @task(2)
    def create_order(self):
        if not self.cart_id:
            return

        payload = {
            "orderDesc": f"Checkout {datetime.now(timezone.utc).isoformat()}",
            "orderFee": round(random.uniform(20, 250), 2),
            "cart": {"cartId": self.cart_id, "userId": self.user_id},
        }

        with self.client.post(
            "/order-service/api/orders",
            json=payload,
            name="orders:create",
            catch_response=True,
        ) as response:
            if response.status_code in (200, 201):
                body = response.json()
                self.last_order_id = body.get("orderId")
                response.success()
            else:
                response.failure(f"Order creation failed ({response.status_code})")

    @task(1)
    def register_payment(self):
        if not self.last_order_id:
            return

        payload = {
            "isPayed": False,
            "paymentStatus": "NOT_STARTED",
            "order": {"orderId": self.last_order_id},
        }

        with self.client.post(
            "/payment-service/api/payments",
            json=payload,
            name="payments:create",
            catch_response=True,
        ) as response:
            if response.status_code in (200, 201):
                response.success()
            else:
                response.failure(f"Payment creation failed ({response.status_code})")

    @task(1)
    def list_orders(self):
        with self.client.get(
            "/order-service/api/orders",
            name="orders:list",
            catch_response=True,
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Orders list failed ({response.status_code})")
