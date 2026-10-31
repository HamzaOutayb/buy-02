# Buy 01 — E-Commerce Microservices Platform

A full-stack e-commerce marketplace built with **Spring Boot microservices, Angular, MongoDB, Apache Kafka, and Docker Compose**. The platform allows users to register as clients or sellers, browse products, and manage product listings and images.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [CI/CD and SonarQube](#cicd-and-sonarqube)
- [Project Structure](#project-structure)

## Overview

Buy 01 is a marketplace application designed around independently deployable backend services. Each service has a specific responsibility, while the API Gateway provides a single entry point for frontend requests.

The project demonstrates microservice architecture, REST API development, authentication and authorization, event-driven communication, file uploads, and containerized deployment.

## Architecture

![Buy 01 Backend Architecture](backend-architecteur.jpeg)

The backend consists of the following components:

| Component | Responsibility |
|---|---|
| Angular Frontend | User interface, authentication forms, product listings, and seller dashboard |
| API Gateway | Entry point for client requests and routing to backend services |
| Eureka Server | Service registration and discovery |
| User Service | Registration, login, user profiles, and roles |
| Product Service | Product management, search, and stock reservation |
| Orders Service | Buyer carts, checkout, order tracking, and seller order management |
| Media Service | Image management and Cloudinary integration |
| MongoDB | Persistence for users, products, and media metadata |
| Apache Kafka | Asynchronous communication between services |
| Cloudinary | External storage for uploaded images |

### Service Communication

- The frontend communicates with the backend through the API Gateway.
- The API Gateway routes requests to registered services.
- The Orders Service obtains current product details from the Product Service and reserves stock when checkout succeeds.
- Backend services use Eureka for service discovery.
- MongoDB stores application data.
- Kafka transports asynchronous events between the Product Service and Media Service.
- Cloudinary stores image files, while the application stores their references and related metadata.

## Features

### Authentication and Authorization
- User registration and login.
- Client and seller roles.
- JWT-based authentication.
- Password hashing and protected endpoints.
- Role-based access to seller operations.

### Product Management
- Browse available products.
- Search products by name or description, filter by category, and navigate paginated results.
- Retrieve product details.
- Create, update, and delete products.
- Associate products with image URLs.
- Protect seller operations according to the application's authorization rules.

The public product search endpoint is `GET /products/search`. It accepts `q`, `category`, `page`, and `size` query parameters. Results are returned in a stable newest-first order. The existing `GET /products` endpoint remains available for the seller product view.

### Cart and Orders
- Add available products to a buyer cart, update quantities, and remove items.
- Checkout with a delivery address and Pay on Delivery. The Orders Service snapshots product prices and details and splits multi-seller checkouts into seller-scoped orders.
- Track order status, search/filter order history by text, and cancel placed orders.
- Sellers can list their related orders and advance them through `PROCESSING`, `SHIPPED`, and `DELIVERED`.
- Product stock is reserved atomically during checkout and returned when a placed order is cancelled.

The gateway exposes the buyer API under `/cart` and `/orders`, and seller order management under `/seller/orders`. Order-list endpoints accept optional `q`, `status`, `from`, and `to` query parameters; dates use `YYYY-MM-DD`.

### Media Management
- Upload product images.
- Store images using Cloudinary.
- Keep image references in the application database.
- Use Kafka events to communicate product-deletion information for image cleanup.

### Infrastructure
- Independent Spring Boot services.
- Eureka service discovery.
- Centralized API routing.
- MongoDB persistence.
- Kafka event-driven communication.
- Docker Compose orchestration.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Backend programming language |
| Spring Boot | Backend microservices |
| Spring Security | Authentication and authorization |
| Spring Cloud Gateway | API Gateway |
| Netflix Eureka | Service discovery |
| Spring Data MongoDB | Database integration |
| Apache Kafka | Asynchronous messaging |
| Angular | Frontend application |
| TypeScript | Frontend programming language |
| MongoDB | NoSQL database |
| Cloudinary | Image storage |
| Docker | Application containerization |
| Docker Compose | Multi-container orchestration |
| Maven | Java dependency management and builds |

## CI/CD and SonarQube

Jenkins provides continuous integration and deployment for the marketplace. It polls the repository's `master` branch, then checks out changes and runs the backend tests and frontend install, build, and test stages.

### Starting Jenkins and SonarQube

The Jenkins and SonarQube services are defined in `jenkins/compose.yml`. From the repository root, copy the example environment file and fill in the required credentials and host settings:

```sh
cp jenkins/.env.example jenkins/.env
```

Set at least `JENKINS_ADMIN_PASSWORD`, `GITHUB_USERNAME`, `GITHUB_TOKEN`, `SMTP_PASSWORD`, and `PODMAN_SOCKET_PATH` in `jenkins/.env`. Use a GitHub token with read access to the repository. Set `PODMAN_SOCKET_PATH` to the container engine socket on the host. For rootless Podman, this is typically `/run/user/<host-user-id>/podman/podman.sock`; find the ID with `id -u`. The Jenkins container accesses the socket as its unprivileged user through group `0`. Keep `jenkins/.env` private and do not commit it.

Start the CI services with Docker Compose:

```sh
docker compose --env-file jenkins/.env -f jenkins/compose.yml up -d --build
```

Jenkins is available at `http://localhost:8088` and SonarQube at `http://localhost:9000` by default. Change `JENKINS_PORT` or `SONAR_PORT` in `jenkins/.env` if those host ports are already in use.

In SonarQube, create a user token with permission to analyze the project, then set it as `SONAR_TOKEN` in `jenkins/.env`. Jenkins Configuration as Code registers this as the `sonarqube-token` credential and configures the SonarQube server at `http://sonarqube:9000` for communication between the containers.

To enable Jenkins to receive quality-gate results, add a SonarQube webhook with this URL:

```text
http://jenkins:8080/sonarqube-webhook/
```

Leave the webhook secret blank unless you also configure matching secret validation in Jenkins. The `buy-01` project settings and scan paths are defined in `sonar-project.properties`.

### Analysis and deployment

After the backend tests and frontend install, build, and test stages pass, Jenkins runs SonarQube analysis and waits up to 10 minutes for the quality gate. The webhook delivers the gate result to Jenkins. If the gate passes, Jenkins builds and deploys the marketplace containers. If a deployment fails its health checks, the pipeline attempts to restore the previous successful image version. Jenkins also sends build success or failure notifications by email.

SonarQube analyzes the source code and reports maintainability and reliability issues. Its quality gate is a release check: it decides whether the pipeline may proceed to deployment. SonarQube complements the automated tests; it does not replace them.

## Project Structure

The main application code and its CI configuration are organized as follows:

```text
safezone/
├── frontend/
├── backend/
│   ├── api-gateway/
│   ├── eureka-server/
│   ├── mediaservice/
│   ├── orderservice/
│   ├── productservice/
│   └── userservice/
├── jenkins/
│   ├── casc.yaml
│   ├── compose.yml
│   └── .env.example
├── docker-compose.yml
├── Jenkinsfile
├── backend-architecteur.jpeg
└── README.md
```

### Running the marketplace

Start the application stack from the repository root:

```sh
docker compose up --build
```

The Orders Service uses the `order-mongodb` database volume and requires the Product Service and Eureka. Set `JWT_SECRET` and `INTERNAL_SERVICE_TOKEN` in the environment before deploying outside local development; both values must be shared by the services that validate JWTs or perform internal stock operations. The defaults in Compose are development-only.
