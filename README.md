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
| Product Service | Product creation, retrieval, updating, and deletion |
| Media Service | Image management and Cloudinary integration |
| MongoDB | Persistence for users, products, and media metadata |
| Apache Kafka | Asynchronous communication between services |
| Cloudinary | External storage for uploaded images |

### Service Communication

- The frontend communicates with the backend through the API Gateway.
- The API Gateway routes requests to registered services.
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
- Retrieve product details.
- Create, update, and delete products.
- Associate products with image URLs.
- Protect seller operations according to the application's authorization rules.

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

After those checks, Jenkins runs SonarQube analysis and waits for the quality gate. If the gate passes, Jenkins builds and deploys the marketplace containers. If a deployment fails its health checks, the pipeline attempts to restore the previous successful image version. Jenkins also sends build success or failure notifications by email.

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

