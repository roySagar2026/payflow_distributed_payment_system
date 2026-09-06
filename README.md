# PayFlow — Distributed Payment & Transaction Processing System

A backend-only simulation of a modern payment platform (inspired by systems like PhonePe, PayPal, and Stripe), built to practice and demonstrate core distributed systems and financial-backend engineering concepts. This project focuses exclusively on backend architecture — there is no real money movement, banking integration, or production payment processing involved.

> **Note:** This is a learning/practice project, not a production payment system. See [What This Project Is / Isn't](#what-this-project-is--isnt) below.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Key Features](#key-features)
- [Design Decisions & Highlights](#design-decisions--highlights)
- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Running with Docker](#running-with-docker)
- [Monitoring & Observability](#monitoring--observability)
- [Project Structure](#project-structure)
- [What This Project Is / Isn't](#what-this-project-is--isnt)
- [Possible Future Work](#possible-future-work)
- [License](#license)

---

## Overview

PayFlow implements the internal mechanics of a payment platform end-to-end:

- User accounts with JWT-based authentication and authorization
- Digital wallets with deposits, withdrawals, and P2P transfers
- A payment order lifecycle modeled as an explicit state machine
- Idempotent APIs to safely handle client retries
- Concurrency-safe money movement (pessimistic locking, deadlock-safe lock ordering)
- A pluggable fraud/risk rule engine
- Event-driven processing using the transactional outbox pattern with Kafka
- Asynchronous notifications via a Kafka consumer
- Refunds, built on top of the payment order state machine
- Redis-backed rate limiting
- Full audit logging via Spring AOP
- Metrics (Prometheus/Grafana) and distributed tracing (Zipkin)
- Fully containerized with Docker

---

## Architecture

```
                         ┌──────────────┐
                         │   Client     │
                         │ (Swagger UI) │
                         └──────┬───────┘
                                │ JWT Bearer Auth
                                ▼
                     ┌─────────────────────┐
                     │   Spring Boot API    │
                     │  (Controllers/AOP)   │
                     └─────────┬───────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
 ┌──────────────┐      ┌────────────────┐      ┌────────────────┐
 │  PostgreSQL   │      │      Redis      │      │     Kafka       │
 │ (source of    │      │ (idempotency,   │      │ (event-driven   │
 │  truth ledger)│      │  rate limiting) │      │  notifications) │
 └──────────────┘      └────────────────┘      └────────────────┘
        │
        ▼
 ┌───────────────────────────────┐
 │  Outbox Poller (@Scheduled)    │
 │  → publishes to Kafka reliably │
 └───────────────────────────────┘

 Observability: Prometheus (metrics) + Zipkin (distributed tracing)
```

**Core data flow for a payment:**

`CREATED → PENDING → (fraud check) → PROCESSING → COMPLETED/FAILED → (optional) REFUNDED`

Every state transition is validated by the `PaymentOrder` entity itself, preventing invalid jumps (e.g., you cannot refund a `PENDING` order).

---

## Tech Stack

| Layer               | Technology                          |
|---------------------|--------------------------------------|
| Language            | Java 21+                             |
| Framework           | Spring Boot 4.x                      |
| Web                 | Spring Web (REST)                    |
| Security            | Spring Security + JWT (jjwt)         |
| Validation          | Jakarta Validation                   |
| ORM                 | Spring Data JPA + Hibernate          |
| Database            | PostgreSQL                           |
| Migrations          | Flyway                               |
| Cache / Rate Limit  | Redis                                |
| Messaging           | Apache Kafka                         |
| API Docs            | springdoc-openapi (Swagger UI)       |
| Metrics             | Micrometer + Prometheus + Grafana    |
| Tracing             | Micrometer Tracing (Brave) + Zipkin  |
| Build               | Maven                                |
| Containerization    | Docker + Docker Compose              |

---

## Key Features

### Authentication & Authorization
- JWT access/refresh token issuance on register/login
- Stateless session management
- BCrypt password hashing

### Wallets & Money Movement
- One wallet auto-provisioned per user on registration
- Deposits and withdrawals with `BigDecimal` precision (no floating-point currency bugs)
- P2P transfers implemented as **double-entry ledger** writes — every transfer creates a linked debit and credit row, never a single mutable balance update

### Concurrency Safety
- Pessimistic row-level locking (`PESSIMISTIC_WRITE`) on wallet balance mutations
- **Deadlock-safe lock ordering**: when locking two wallets for a transfer, they are always locked in a consistent order (sorted by wallet ID) to prevent circular-wait deadlocks between simultaneous opposite-direction transfers

### Idempotency
- Clients supply an `Idempotency-Key` header on money-moving requests
- Redis stores a hash of the original request body + the response
- Duplicate requests (same key, same body) return the cached result instead of reprocessing
- Reused keys with a *different* body are rejected, preventing key-reuse bugs/attacks

### Payment Order State Machine
- Explicit, enforced states: `CREATED → PENDING → PROCESSING → COMPLETED / FAILED → REFUNDED`
- Invalid transitions throw immediately — the state machine lives on the domain entity itself, not scattered across services

### Fraud/Risk Engine
- Independent, pluggable `FraudRule` implementations (large-amount detection, transaction velocity, suspicious test-amount detection)
- Spring auto-collects all `FraudRule` beans — adding a new rule requires zero changes to orchestration code
- Every fraud check is logged with its score breakdown for auditability

### Event-Driven Architecture
- **Transactional outbox pattern**: business state changes and their corresponding events are written in the *same* database transaction, guaranteeing no event is ever lost even if Kafka is temporarily unreachable
- A scheduled poller publishes outbox events to Kafka with automatic retry
- Kafka consumer (`NotificationConsumer`) reacts to `PaymentCompleted`/`PaymentFailed` events completely decoupled from the core payment logic

### Refunds
- Modeled as a new, separate compensating transaction — never a rewrite of history
- Reuses the existing state machine (`COMPLETED → REFUNDED`) and transfer infrastructure

### Rate Limiting
- Redis-backed fixed-window counter
- Applied declaratively via a custom `@RateLimit` annotation + Spring AOP aspect

### Audit Logging
- Custom `@Audited` annotation + AOP aspect logs every sensitive action (who, what, when, outcome) — independent of and complementary to transaction history

### Observability
- Custom business metrics (payments completed/failed, fraud rejections) alongside standard JVM/HTTP metrics, exposed via `/actuator/prometheus`
- Distributed tracing via Zipkin, showing full request timelines across HTTP → service → database layers

---

## Design Decisions & Highlights

A few decisions worth calling out in an interview or code review:

- **Double-entry ledger over mutable balance updates** — every transfer produces two immutable, linked ledger rows rather than only updating a balance field, matching how real financial systems maintain auditability.
- **Pessimistic over optimistic locking for wallet mutations** — an early optimistic-locking implementation (via JPA `@Version` + retry) was load-tested with concurrent withdrawals and found to fail a significant fraction of legitimate requests under high contention on a single row. Switched to `PESSIMISTIC_WRITE` locking with deadlock-safe ordering for correctness and predictability on this specific access pattern.
- **Outbox pattern over direct Kafka publish** — avoids the dual-write problem where a database commit succeeds but the corresponding Kafka publish fails, which would silently desynchronize state and events.
- **State machine logic lives on the domain entity**, not in service methods — makes invalid state transitions structurally difficult to introduce, regardless of which code path touches the entity.
- **Database-enforced constraints as a safety net** — foreign keys and check constraints (e.g., preventing a wallet from transferring to itself) are enforced at the Postgres level, not only in application code.

---

## Getting Started

### Prerequisites
- Java 21+ (JDK)
- Maven
- PostgreSQL
- Docker (for Redis, Kafka, Zipkin, Prometheus, Grafana)

### Local Setup

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/payflow.git
cd payflow

# 2. Create the database
psql -U postgres -c "CREATE DATABASE payflow;"

# 3. Configure application.properties
# Update spring.datasource.* with your local Postgres credentials/port

# 4. Start supporting infrastructure
docker compose up -d redis kafka zipkin prometheus grafana

# 5. Run the application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

### Explore the API
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Health check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/prometheus`

---

## API Overview

| Method | Endpoint                              | Description                          |
|--------|----------------------------------------|---------------------------------------|
| POST   | `/api/auth/register`                  | Register a user + auto-provision wallet |
| POST   | `/api/auth/login`                     | Authenticate, receive JWT tokens      |
| GET    | `/api/users/me`                       | Get current authenticated user        |
| POST   | `/api/wallets/deposit`                | Deposit funds (idempotent)            |
| POST   | `/api/wallets/withdraw`               | Withdraw funds (idempotent, rate-limited) |
| POST   | `/api/transfers`                      | P2P transfer (idempotent, rate-limited) |
| POST   | `/api/payment-orders`                 | Create & process a payment order      |
| GET    | `/api/payment-orders/{id}`            | Get payment order status              |
| POST   | `/api/payment-orders/{id}/refund`     | Refund a completed order              |
| GET    | `/api/notifications/me`               | View your notification history        |
| GET    | `/api/audit-logs/me`                  | View your audit trail                 |

All endpoints except `/api/auth/**` and actuator/docs endpoints require a `Bearer` JWT token.

---

## Running with Docker

The entire system — application, PostgreSQL/local, Redis, Kafka, Zipkin, Prometheus, and Grafana — can run via Docker Compose:

```bash
docker compose up -d --build
```

This builds the application image from the multi-stage `Dockerfile` and starts all supporting infrastructure. See `docker-compose.yml` for full service configuration.

---

## Monitoring & Observability

- **Metrics**: Prometheus scrapes `/actuator/prometheus` every 5 seconds. Visualize in Grafana (`http://localhost:3000`) by adding Prometheus as a data source.
- **Tracing**: Every request is traced end-to-end and visible in Zipkin (`http://localhost:9411`), showing HTTP handling time, service calls, and database query spans.
- **Custom business metrics**: `payflow_payments_completed_total`, `payflow_payments_failed_total{reason=...}`, `payflow_fraud_rejections_total`

---

## Project Structure

```
src/main/java/com/payflow/payflow/
├── audit/            # Audit logging annotation + AOP aspect
├── config/           # Security, OpenAPI configuration
├── controller/        # REST controllers
├── dto/               # Request/response DTOs
├── event/             # Kafka event payload records
├── fraud/             # Fraud rule interface + implementations
├── model/             # JPA entities
├── ratelimit/         # Rate limiting annotation + AOP aspect
├── repository/        # Spring Data JPA repositories
├── security/          # JWT service + filter
└── service/           # Business logic
```

---

## What This Project Is / Isn't

**This project demonstrates:** correct financial data modeling, concurrency-safe transaction processing, idempotent API design, event-driven architecture, and observability — the internal engineering patterns real payment systems rely on.

**This project does not:** move real money, integrate with banking rails (UPI/NPCI/ACH), perform KYC/identity verification, hold any regulatory compliance (RBI/PCI-DSS), or include a frontend. It is a backend architecture simulation, not a production payment processor.

---

## Possible Future Work

- Reconciliation job comparing internal ledger sums for consistency
- Role-based access control (admin vs. regular user)
- Event-type Kafka headers instead of envelope-based routing
- Sandbox integration with a real payment gateway (e.g., Razorpay test mode)
- Frontend client

---

## License

This project is for educational/portfolio purposes.
