
# Buy-02

Buy-02 is a full-featured microservices e-commerce marketplace web application. The platform provides a complete end-to-end purchasing experience, from product browsing, searching, and faceted filtering to shopping cart management, Pay on Delivery checkout, order lifecycle tracking, and user/seller analytics dashboards.

The frontend is built with Angular, and the backend is composed of Spring Boot microservices registered with Netflix Eureka, routed through Spring Cloud Gateway, persisted across dedicated MongoDB databases, and integrated asynchronously with Apache Kafka.

---

## Table of Contents

- [Features](#features)
- [System Architecture](#system-architecture)
- [API Endpoints](#api-endpoints)
- [Repository Layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Quick Start with Docker](#quick-start-with-docker)
- [Backend Services Configuration](#backend-services-configuration)
- [Running Backend Services Locally](#running-backend-services-locally)
- [Frontend Development](#frontend-development)
- [Testing](#testing)
- [CI/CD & Code Quality](#cicd--code-quality)
- [Error Handling & Security](#error-handling--security)
- [Troubleshooting](#troubleshooting)

---

## Features

### 🛒 Shopping Cart & Checkout

- **Cart Management:** Add products, adjust quantities, remove items, and view subtotals, tax, and order totals.
- **Pay on Delivery Checkout:** Multi-step checkout wizard covering shipping address, order review, and cash-on-delivery confirmation.

### 📦 Orders Management

- **Order Lifecycle & Tracking:** Follow order progression with detailed status timelines:
  `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
- **Buyer & Seller Order Portals:**
  - Buyers can inspect purchase history, search and filter by status and date, cancel orders, or reorder.
  - Sellers can monitor customer orders containing their products, search by order ID or status, and update fulfillment states.

### 👤 Profiles & Analytics Dashboards

- **User Dashboard:** Overview of personal spend, order count, most-purchased products, and top category breakdown with interactive charts.
- **Seller Dashboard:** Revenue analytics, best-selling products, unit sales metrics, and product performance.

### 🔎 Search & Faceted Filtering

- **Keyword Search:** Real-time product search across titles and descriptions.
- **Faceted Filters:** Refine search by category, price ranges, sorting (price, date, popularity), and pagination.

### 🛡️ Robust Microservices Architecture

- **JWT Authentication & RBAC:** Token-based security and role checks enforced at gateway and service layers.
- **Asynchronous Communication:** Kafka-driven events between orders, products, and media services.
- **Service Discovery:** Netflix Eureka registry for dynamic microservice discovery and load balancing.

---

## System Architecture

```text
               Angular Frontend (:4200)
                          |
                          v
                   API Gateway (:8080)
          [SSL / Reverse Proxy / JWT Authentication]
             |          |          |          |
             v          v          v          v
         User Service  Product    Media      Order
           (:8081)    Service    Service    Service
                      (:8082)    (:8083)    (:8084)
             \          |          |          /
              +---------+----------+---------+
                          |
                    Eureka Server
                       (:8761)

--------------------------------------------------
Infrastructure:
  - MongoDB (:27017) -> userDB, productDB, mediaDB, orderDB
  - Apache Kafka Broker (:9092)
--------------------------------------------------
```

---

## API Endpoints

The API Gateway (`http://localhost:8080`) serves as the single entry point.

| Prefix | Service | Key Responsibilities |
|---|---|---|
| `/api/v1/auth` | User Service | Registration, login, JWT issuance |
| `/api/v1/users` | User Service | User profile management, buyer analytics |
| `/api/v1/products` | Product Service | Product catalog, search, filtering, seller management |
| `/api/v1/media` | Media Service | Product image upload and Cloudinary asset management |
| `/api/v1/cart` | Order Service | Add, update, remove cart items, cart calculations |
| `/api/v1/orders` | Order Service | Order placement, tracking, seller fulfillment, status updates, cancellation and reordering |

**Note:** Public endpoints, such as catalog browsing and product search, are read-only. All mutations and cart, order, and profile operations require JWT authentication.

---

## Repository Layout

```text
├── frontend/
│   └── src/app/
│       ├── components/         # Shared UI components
│       ├── guards/             # Angular route guards (Auth, Role)
│       ├── interceptors/       # JWT injection, 401/403 handlers
│       ├── pages/              # Cart, Checkout, Orders, Profiles, Search, Dashboards
│       └── services/           # Angular services connecting to Gateway APIs
│
├── backend/
│   ├── docker-compose.yml      # Microservices orchestration
│   ├── sonarscanner.sh         # SonarQube scanner automation
│   ├── gateway/                # Spring Cloud Gateway (:8080)
│   ├── server/                 # Netflix Eureka Server (:8761)
│   ├── userService/            # Authentication & User Profile (:8081)
│   ├── productService/         # Product Catalog & Search (:8082)
│   ├── mediaService/           # Cloudinary Media Management (:8083)
│   └── orderService/           # Cart & Order Processing (:8084)
│
└── jenkins/
    ├── docker-compose.yaml     # Jenkins & SonarQube orchestration
    └── Jenkinsfile             # Build -> Test -> Quality Gate -> Package
```

---

## Prerequisites

- **Docker & Docker Compose:** v2+
- **Node.js:** Compatible with Angular 21
- **npm:** 11.x+
- **Java:** JDK 17 or 21
- **Maven:** For manual local builds

---

## Quick Start with Docker

### 1. Start Backend Services

```bash
cd backend
docker compose up --build
```

### 2. Start the Angular Frontend

Open a separate terminal:

```bash
cd frontend
npm install
npm start
```

### 3. Access the Application

| Component | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Eureka Service Registry | http://localhost:8761 |
| MongoDB | localhost:27017 |
| Kafka Broker | localhost:9092 |

### Stop the Backend Environment

```bash
cd backend
docker compose down
```

**Note:** Database data is preserved in `backend/data/db` between runs.

---

## Backend Services Configuration

Backend services can be configured using environment variables in `backend/.env`.

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | Required | Secret key used for signing and verifying JWT tokens |
| `MONGODB_URI` | `mongodb://admin:password@mongodb:27017/?authSource=admin` | MongoDB connection string |
| `SERVER_SSL_KEY_STORE_PASSWORD` | Required for SSL | Gateway SSL keystore password |
| `CLOUDINARY_API_KEY` | Required | Cloudinary API key for image uploads |
| `CLOUDINARY_API_SECRET` | Required | Cloudinary API secret for image uploads |

---

## Running Backend Services Locally

Ensure MongoDB, Kafka, and Eureka are running locally or through Docker.

Each service includes a Maven wrapper (`./mvnw`).

### Run an Individual Service

```bash
cd backend/<service-directory>
./mvnw spring-boot:run
```

### Service Ports & Databases

| Service | Directory | Port | Database |
|---|---|---:|---|
| Eureka Registry | `backend/server` | 8761 | None |
| API Gateway | `backend/gateway` | 8080 | None |
| User Service | `backend/userService` | 8081 | `userDB` |
| Product Service | `backend/productService` | 8082 | `productDB` |
| Media Service | `backend/mediaService` | 8083 | `mediaDB` |
| Order Service | `backend/orderService` | 8084 | `orderDB` |

---

## Frontend Development

From the `frontend/` directory:

```bash
npm install        # Install project dependencies
npm start          # Start development server
npm run build      # Compile production build into dist/
npm run watch      # Compile and rebuild on file changes
npm test           # Run unit tests
```

---

## Testing

### Backend Unit & Integration Tests

Run tests for an individual microservice:

```bash
cd backend/<service-directory>
./mvnw test
```

### Generate JaCoCo Coverage Reports

Generate coverage reports and enforce the **>=80% line coverage** requirement:

```bash
cd backend/<service-directory>
./mvnw clean verify
```

Reports are generated at:

```text
target/site/jacoco/index.html
```

### Frontend Tests

Run Angular unit tests with coverage:

```bash
cd frontend
npm test -- --coverage
```

---

## CI/CD & Code Quality (Jenkins & SonarQube)

The project follows CI/CD and code quality practices.

### Jenkins Pipeline Stages

1. **Checkout:** Source code retrieval and verification.
2. **Build:** Parallel builds of all microservices and frontend.
3. **Test:** Unit and integration tests with JaCoCo XML reports.
4. **Quality Gate:** SonarQube analysis for security vulnerabilities, code smells, duplication, and test coverage.
5. **Package & Deploy:** Containerization and deployment preparation.

### Running Jenkins & SonarQube Locally

```bash
cp jenkins/.env.example jenkins/.env
```

Update `jenkins/.env` with your credentials and `SONAR_TOKEN`.

Then start the environment:

```bash
docker compose -f jenkins/docker-compose.yaml up --build
```

### Access CI/CD Tools

- **Jenkins:** http://localhost:8080 (or configured Jenkins port)
- **SonarQube:** http://localhost:9090

### Run a Manual SonarQube Scan

```bash
cd backend
./sonarscanner.sh
```

---

## Error Handling & Security

### Global Exception Handling

All microservices return consistent error response schemas.

Example:

```json
{
  "code": "BAD_REQUEST",
  "message": "Detailed error message",
  "details": []
}
```

### HTTP Status Codes

| Status Code | Meaning |
|---|---|
| `400 Bad Request` | Validation errors |
| `401 Unauthorized` | Authentication failures |
| `403 Forbidden` | Unauthorized role access |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Business rule conflicts, such as duplicate states or inventory locks |

### Security Measures

- **Ownership Enforcement:** Buyers can only modify their own orders, while sellers can only fulfill orders containing their products.
- **Frontend Interceptors & Guards:** Automatic JWT injection, unified toast/snackbar notifications, and protected routes.
- **Credential Protection:** Never commit secrets or credentials. Always inject them through environment variables or secret stores.

---

## Troubleshooting

If a service fails to start:

1. Verify Docker containers are running.
2. Check that MongoDB and Kafka are accessible.
3. Ensure Eureka is running before dependent services.
4. Verify environment variables in `backend/.env`.
5. Check service logs:

```bash
docker compose logs -f <service-name>
```
