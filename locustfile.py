"""
Locust performance tests that exercise the API Gateway against the microservices.

Default host: http://localhost:8080 (override with --host or LOCUST_HOST env var)
Endpoints used:
- /user-service/api/users
- /order-service/api/carts
- /product-service/api/products
- /order-service/api/orders
- /payment-service/api/payments
- /favourite-service/api/favourites
- /shipping-service/api/shippings
"""

import os
import random
import string
from datetime import datetime, timezone

from locust import HttpUser, LoadTestShape, between, task


def _rand(prefix: str, length: int = 6) -> str:
    """Short helper to build deterministic-but-unique strings."""
    suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=length))
    return f"{prefix}-{suffix}"


def _format_like_date() -> str:
    """Format timestamp as expected by favourite-service (dd-MM-yyyy__HH:mm:ss:SSSSSS)."""
    return datetime.now(timezone.utc).strftime("%d-%m-%Y__%H:%M:%S:%f")


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

    @task(1)
    def add_favourite(self):
        if not (self.user_id and self.last_product_id):
            return

        payload = {
            "userId": self.user_id,
            "productId": self.last_product_id,
            "likeDate": _format_like_date(),
        }

        with self.client.post(
            "/favourite-service/api/favourites",
            json=payload,
            name="favourites:create",
            catch_response=True,
        ) as response:
            if response.status_code in (200, 201):
                response.success()
            else:
                response.failure(f"Favourite creation failed ({response.status_code})")

    @task(1)
    def create_shipping(self):
        if not (self.last_order_id and self.last_product_id):
            return

        payload = {
            "orderId": self.last_order_id,
            "productId": self.last_product_id,
            "orderedQuantity": random.randint(1, 3),
        }

        with self.client.post(
            "/shipping-service/api/shippings",
            json=payload,
            name="shippings:create",
            catch_response=True,
        ) as response:
            if response.status_code in (200, 201):
                response.success()
            else:
                response.failure(f"Shipping creation failed ({response.status_code})")


# --------- Load shape ---------
class StepLoadShape(LoadTestShape):
    """
    Escenario escalonado:
    - 1 min: 10 usuarios (warm-up)
    - 1 min: 30 usuarios (carga media)
    - 1 min: 60 usuarios (stress)
    - 1 min: 30 usuarios (descenso)
    - 1 min: 10 usuarios (enfriamiento)
    """

    stages = [
        {"duration": 60, "users": 10, "spawn_rate": 5},
        {"duration": 120, "users": 30, "spawn_rate": 10},
        {"duration": 180, "users": 60, "spawn_rate": 20},
        {"duration": 240, "users": 30, "spawn_rate": 10},
        {"duration": 300, "users": 10, "spawn_rate": 5},
    ]

    def tick(self):
        run_time = self.get_run_time()

        for stage in self.stages:
            if run_time < stage["duration"]:
                return stage["users"], stage["spawn_rate"]

        return None
