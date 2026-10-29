# Buy 02 — Technical Two-Day MVP Plan

This is an implementation checklist, not a feature-status report. The repository currently has Spring Boot user, product, media, gateway, and Eureka services; Angular auth/product/profile pages; MongoDB; Jenkins; and SonarQube. It does not yet have the Buy 02 cart, orders, analytics, or search/filter flows.

**Scope:** Implement the required MVP only. Pay on Delivery is the only payment method. Skip optional wishlist and other payment methods. Do not skip server-side authorization, price calculation, stock validation, or critical tests.

## Architecture and implementation choices

- Keep product search in the existing Product Service.
- Add an Orders Service for buyer carts, orders, and order-derived analytics.
- Give Orders Service its own MongoDB database/collection; do not write directly to another service's MongoDB.
- Route browser requests through API Gateway and use the existing JWT identity. Controllers must get buyer/seller IDs from the authenticated principal, never from request bodies.
- Treat the UI and client-supplied prices/totals as untrusted. Resolve product information and calculate totals on the backend.
- Keep order item snapshots (`productId`, product name, seller ID, unit price, quantity) so historical orders don't change when a product is edited or deleted.
- For a two-day MVP, use simple Angular pages/cards/tables rather than charting libraries or elaborate UI.

## Day 1 — backend, API contracts, and backend tests

### 1. Prepare branches and service skeleton

- [ ] Create feature branches such as `feature/product-search`, `feature/orders-cart`, and `feature/analytics`.
- [ ] For each branch, agree on the endpoint/request/response contract below before implementing the Angular client.
- [ ] Create `backend/orderservice` following the existing Spring Boot service conventions. Start by copying the dependency/configuration pattern from `backend/productservice`, then change package names, artifact name, Mongo database, and service name.
- [ ] Add an Orders Service application class, `application.yml`, Mongo configuration, Eureka client configuration, Dockerfile, and test configuration.
- [ ] Add the service's folders following existing patterns:

```text
backend/orderservice/src/main/java/.../
  controller/       CartController, OrderController, AnalyticsController
  dto/              request and response DTOs
  model/            Cart, CartItem, Order, OrderItem, OrderStatus
  repository/       CartRepository, OrderRepository
  service/          CartService, OrderService, AnalyticsService
  security/         JWT filter/service and SecurityConfig
  exception/        API error and GlobalExceptionHandler
```

- [ ] Ensure JWT parsing/role mapping matches existing services. Confirm whether the authenticated principal's name is the user ID or email; consistently use the identifier already used for product `sellerId`.
- [ ] Do not expose Mongo entities directly from controllers. Use DTOs with only the fields the client needs.

**Done when:** Orders Service starts locally, connects to its own MongoDB, registers with Eureka, and has a passing health endpoint.

### 2. Implement product keyword search, filters, and pagination

- [ ] Add `category` to `Product`, `ProductRequest`, and `ProductResponse`; update the Angular `Product`/`ProductInput` models and seller product create/edit form.
- [ ] Add a `ProductSearchRequest` or controller query parameters for:

```text
GET /products?q=&category=&minPrice=&maxPrice=&sort=price,asc&page=0&size=20
```

- [ ] Use Spring Data MongoDB `MongoTemplate` with `Criteria` and `Query` (or repository queries if they support every required combination) to apply optional filters and paging.
- [ ] Allowlist sort fields such as `name`, `price`, and `createdAt`; reject unsupported sort fields and invalid ranges/page sizes.
- [ ] Return a stable paginated response, for example `{ content, page, size, totalElements, totalPages }`. Update the existing Angular product service/component to read this response; avoid silently changing the existing list shape without updating every caller.
- [ ] Keep `GET /products` and `GET /products/{id}` public/read-only. Search must not make create/update/delete public.
- [ ] Add Mongo indexes for commonly filtered fields (at minimum category and price if used); use a text index only if its behavior fits the desired keyword search.
- [ ] Add Product Service tests for keyword-only, each filter, combined filters, sorting, page boundaries, empty results, and invalid input.

**Files to change/add:** `Product.java`, `ProductRequest.java`, `ProductResponse.java`, `ProductRepository.java` or a custom search repository, `ProductService.java`, `ProductController.java`, mapper, product tests; Angular product model/service/listing and seller form.

### 3. Implement buyer cart

- [ ] Store one cart per buyer. Suggested Mongo shape:

```text
Cart { id, buyerId, items: [{ productId, quantity }], updatedAt }
```

- [ ] Add a unique index on `buyerId` and a repository method to find the cart by buyer.
- [ ] Add DTOs: `AddCartItemRequest { productId, quantity }`, `UpdateCartItemRequest { quantity }`, and a `CartResponse` containing current product details and calculated line/subtotals.
- [ ] Implement endpoints:

```text
GET    /cart
POST   /cart/items
PATCH  /cart/items/{productId}
DELETE /cart/items/{productId}
```

- [ ] In each controller, get `buyerId` from `Authentication`. Ignore/reject buyer IDs in request payloads.
- [ ] Validate nonblank product ID and `quantity >= 1`. When adding an existing item, define whether quantities are added or replaced and use that behavior consistently.
- [ ] Resolve current product data through a typed HTTP client (`WebClient`/`RestClient`) to Product Service. Do not accept product name, seller ID, or price from the browser as authoritative.
- [ ] Reject missing or unavailable products and quantities above current stock. Return product details with cart reads so the client can show current product information.
- [ ] Add `CartService` unit tests and controller tests for empty cart, add/update/remove, invalid quantity, missing product, and cart ownership.

**Files to add:** Orders Service cart model/DTO/repository/service/controller/tests; Product Service client; Angular `cart.model.ts`, `cart.service.ts`, and cart UI later on Day 2.

### 4. Implement checkout and order management

- [ ] Define a constrained status enum, for example `PLACED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`. Document which statuses can be cancelled; a simple MVP rule is cancellation only while `PLACED`.
- [ ] Suggested persisted order:

```text
Order {
  id, buyerId,
  items: [{ productId, productName, sellerId, unitPrice, quantity, lineTotal }],
  deliveryAddress,
  subtotal, total,
  paymentMethod: "PAY_ON_DELIVERY",
  status, createdAt, updatedAt, archived
}
```

- [ ] Keep the checkout request minimal: delivery address and any required confirmation. Do not accept buyer ID, item price, seller ID, subtotal, or total as trusted input.
- [ ] Implement:

```text
POST /orders/checkout
GET  /orders
GET  /orders/{orderId}
POST /orders/{orderId}/cancel
POST /orders/{orderId}/reorder
GET  /seller/orders
```

- [ ] During checkout, load the buyer's cart, fetch authoritative product data, validate every item and stock level, calculate totals with `BigDecimal`, and create an order snapshot using `PAY_ON_DELIVERY`.
- [ ] Do not reduce inventory by an unsafe read-then-write sequence. Add a Product Service stock reservation/decrement operation that performs a conditional atomic update (`quantity >= requested`) and returns the product snapshot. If multiple items are reserved and a later reservation/order save fails, define and test compensating releases; do not claim cross-service atomicity.
- [ ] Make checkout safe against duplicate submissions. For a minimal solution, accept an idempotency key and persist it uniquely per buyer, or otherwise ensure a retry cannot create duplicate orders.
- [ ] Clear the cart only after order creation succeeds. Define compensation if inventory was reserved but order persistence failed.
- [ ] Filter buyer order queries by authenticated buyer ID. Filter seller queries by the seller ID in order items. For `GET /orders/{id}` and cancellation, verify ownership in the service before returning or changing the order.
- [ ] Implement reorder by copying still-available product IDs/quantities into the cart. Revalidate product availability and price at the next checkout.
- [ ] Prefer archive/hide over permanently deleting order history. If the evaluator expects “remove,” document exactly which orders are removable and whether this only archives them.
- [ ] Return appropriate status codes: `201` order created, `400` invalid input, `401` unauthenticated, `403` forbidden, `404` missing, and `409` stock/status/idempotency conflict.
- [ ] Add tests for price recalculation, stock conflicts, duplicate checkout, cart clearing, buyer/seller access rules, cancellation rules, and failed reservation compensation.

**Cross-service note:** Mongo transactions cannot make writes across Product Service and Orders Service atomic. Use a reservation/compensation flow (a small saga) and test its failure paths. If time is too short to implement reservations correctly, explicitly limit the MVP and do not silently create orders while overselling stock.

### 5. Wire Orders Service into the platform

- [ ] Add gateway routes for `/cart`, `/cart/**`, `/orders`, `/orders/**`, `/seller/orders`, `/analytics/**` to the Orders Service in `GatewayRoutesConfig`.
- [ ] Add an Orders MongoDB container, Orders Service container, environment variables, healthcheck, and `depends_on` entries to `docker-compose.yml`.
- [ ] Configure the Orders Service with its Mongo URI, Eureka URL, and JWT secret using the same environment-variable conventions as existing services.
- [ ] Do not add a dependency on another service's database. Product data must be fetched through Product Service endpoints.
- [ ] Build and run the affected Maven modules and Compose configuration before beginning the frontend work.

### 6. Backend test checkpoint

- [ ] Run `./mvnw -B test` in Product Service, Orders Service, and any other changed backend module.
- [ ] Add integration/controller tests for authentication and authorization, not only service unit tests.
- [ ] Verify public product reads work without a token and all cart/order mutations require authentication.
- [ ] Verify a seller cannot act as a buyer and one buyer cannot access another buyer's order.
- [ ] Do not move to UI integration until the API contract and core backend flow pass.

## Day 2 — Angular integration, analytics, CI, and release check

### 7. Add Angular models and API services

- [ ] Add typed interfaces for `Cart`, `CartItem`, `Order`, `OrderItem`, `OrderStatus`, paginated products, and analytics responses under `frontend/src/app/models/`.
- [ ] Add `CartService`, `OrderService`, and `AnalyticsService` under `frontend/src/app/services/`. Use `HttpClient`, the existing `API_BASE_URL`, and the existing auth interceptor pattern.
- [ ] Keep server-owned fields (buyer ID, seller ID, price, totals, status) read-only in frontend types and never send them as trusted checkout data.
- [ ] Add focused service specs that assert request URLs, query parameters, and error propagation.

### 8. Build cart and checkout pages

- [ ] Add standalone components under `frontend/src/app/components/cart/` and `checkout/`.
- [ ] Use `ReactiveFormsModule` for delivery address and checkout validation.
- [ ] Add cart routes in `app.routes.ts` protected by `authGuard`.
- [ ] Add “Add to cart” buttons to product list and details. Disable or explain the action for products with no stock; handle unauthenticated users by routing to login.
- [ ] Render product, quantity, per-item price, line total, subtotal, and server-calculated total. Refresh the cart after add/update/remove.
- [ ] Checkout page should show address → order review → Pay on Delivery confirmation. Submit only the address/confirmation (and idempotency key if implemented).
- [ ] On a stock conflict, display the API message and reload the cart; do not report checkout success.
- [ ] Add empty, loading, error, and success states, plus keyboard-accessible labels/buttons.

### 9. Build buyer and seller order pages

- [ ] Add buyer order list and details components under `frontend/src/app/components/orders/`.
- [ ] Add authenticated routes for `/orders` and `/orders/:id`.
- [ ] Add a seller orders component/route protected by `sellerGuard`.
- [ ] Show buyer orders with status/date filters; show order items, snapshot prices, total, delivery details, and a status timeline.
- [ ] Display cancel/reorder actions only when appropriate, but rely on backend checks as the authority.
- [ ] Ensure seller screens do not rely on client-side filtering for privacy; use the seller-scoped backend endpoint.

### 10. Add search/filter UI to product listing

- [ ] Add a reactive search form and category, min/max price, sort, and page controls in the existing products component/template.
- [ ] Call the paginated Product Service endpoint when filters change; debounce keyword input and reset page to zero when filters change.
- [ ] Preserve filters in URL query parameters if time permits so a search can be reloaded/shared.
- [ ] Show total results, current page, loading/error/empty states, and a clear-filters button.
- [ ] Add component tests for query parameter generation and empty/error states.

### 11. Add backend analytics and simple dashboard UI

- [ ] Add `GET /analytics/me` and `GET /analytics/seller` to Orders Service (or separate clearly scoped endpoints).
- [ ] Aggregate from completed/non-cancelled order snapshots using Mongo aggregation or Java aggregation over a bounded query.
- [ ] Buyer response: total spent, most-bought products, and top categories if category snapshots are stored.
- [ ] Seller response: revenue, best-selling products, and units sold, computed only from items matching the authenticated seller ID.
- [ ] Decide whether `DELIVERED` orders alone count toward revenue/spend; apply the same rule to both dashboards.
- [ ] If top categories are required, add category to order item snapshots at checkout; old orders without category should be handled explicitly.
- [ ] Add simple dashboard cards/tables to the existing profile area or a seller dashboard component. Charts are optional.
- [ ] Test aggregation with multiple buyers/sellers, cancelled orders, and empty order history.

### 12. CI, final verification, and PR delivery

- [ ] Add Orders Service test/build and JaCoCo output to `Jenkinsfile`.
- [ ] Add Orders Service source/test/binary/coverage paths to `sonar-project.properties`.
- [ ] Verify the gateway and Orders Service compile in CI, test reports are published, and SonarQube quality gate is not bypassed.
- [ ] Verify `docker-compose.yml` can build/start the new service and health checks pass.
- [ ] Run `npm ci`, `npm test`, and `npm run build` in `frontend/`.
- [ ] Run `./mvnw -B test` in every affected backend service.
- [ ] Manually test: public search → login → add/update/remove cart item → checkout Pay on Delivery → buyer order list/details → seller's related order list → cancel/reorder where allowed → buyer/seller analytics.
- [ ] Check 401/403/404/409 and invalid input behavior. Confirm no secrets or password/token data appear in responses/logs.
- [ ] Update the main `README.md` to reflect features that are actually implemented, list services and API routes, and include local run/test instructions.
- [ ] Update project branding (`buy-01` vs `buy-02`) only if intended; if renaming, update Compose project/container/image names, Jenkins deployment references, SonarQube project key/name, and documentation together.
- [ ] Open/review/merge PRs only after the relevant tests and Jenkins quality gate pass.

## Two-day priority if schedule slips

1. Search/filter backend and UI.
2. Cart and server-validated Pay on Delivery checkout, including stock and ownership checks.
3. Buyer order history/details and seller-scoped order list.
4. Critical backend authorization, checkout, stock, and status tests.
5. Basic analytics cards and aggregation.
6. Styling, charts, URL-persisted filters, and nonessential polish.

If time is insufficient for a safe stock reservation/compensation flow, be explicit about that limitation rather than presenting checkout as concurrency-safe.
