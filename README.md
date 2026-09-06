# PayFlow — Distributed Payment & Transaction Processing System

> **A backend simulation of a modern payment platform's internal architecture — wallets, ledgers, idempotency, fraud detection, and event-driven processing, built the way real fintech systems are engineered.**

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-Event--Driven-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-Idempotency%20%26%20Rate%20Limiting-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-22C55E.svg)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Design Decisions & Highlights](#design-decisions--highlights)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Running with Docker](#running-with-docker)
- [Monitoring & Observability](#monitoring--observability)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [What This Project Is / Isn't](#what-this-project-is--isnt)
- [Roadmap](#roadmap)
- [License](#license)
- [Author](#author)

---

## Overview

**PayFlow** implements the internal engineering of a payment platform (conceptually similar to PhonePe, PayPal, or Stripe) end-to-end — focused entirely on backend correctness, not on real money movement or banking integration.

### Problem Statement

Payment systems have to get a specific set of hard problems right simultaneously: money must never be created or destroyed, concurrent operations on the same account must never corrupt balances, network retries must never double-charge a user, and every action must be traceable after the fact. Most backend tutorials skip these problems entirely. **PayFlow implements them properly**, using the same patterns production fintech systems rely on.

### What It Covers

| Concern | Implementation |
|---|---|
| **Identity** | JWT-based authentication & authorization |
| **Ledger** | Double-entry wallet transactions, not mutable balance fields |
| **Concurrency** | Pessimistic locking with deadlock-safe lock ordering |
| **Reliability** | Idempotent APIs backed by Redis |
| **Risk** | Pluggable fraud/risk rule engine |
| **Async Processing** | Transactional outbox pattern + Kafka |
| **Compliance-adjacent** | Full audit logging via AOP |
| **Operability** | Prometheus metrics + Zipkin distributed tracing |
| **Delivery** | Fully Dockerized, multi-stage builds |

---

## System Architecture

### Request & Data Flow

```
┌──────────────┐
│   Client      │
│ (Swagger UI)  │
└──────┬───────┘
       │ JWT Bearer Auth
       ▼
┌─────────────────────────────────────┐
│        Spring Boot API Layer         │
│   Controllers → AOP (Audit/RateLimit)│
└───────────────────┬───────────────────┘
                    │
   ┌─────────────────┼─────────────────┐
   ▼                 ▼                 ▼
┌──────────┐   ┌────────────┐   ┌─────────────┐
│PostgreSQL │   │   Redis     │   │   Kafka      │
│ ledger,   │   │ idempotency,│   │ events for   │
│ orders,   │   │ rate limits │   │ notifications│
│ audit log │   └────────────┘   └─────────────┘
└─────┬────┘
      │
      ▼
┌───────────────────────────────┐
│  Outbox Poller (@Scheduled)   │
│  → publishes events reliably  │
│    even if Kafka is down      │
└───────────────────────────────┘

Observability: Prometheus (metrics) + Zipkin (distributed tracing)
```

### Payment Order State Machine

```
CREATED → PENDING → [fraud check] → PROCESSING → COMPLETED ──► REFUNDED
                          │                │
                          ▼                ▼
                        FAILED           FAILED
```

Every transition is validated on the `PaymentOrder` entity itself — invalid jumps (e.g. refunding a `PENDING` order) are structurally impossible, not just discouraged by convention.

---

## Key Features

**Authentication & Authorization**
- JWT access/refresh token issuance on register/login
- Stateless session management, BCrypt password hashing

**Wallets & Money Movement**
- One wallet auto-provisioned per user at registration
- `BigDecimal` precision throughout — no floating-point currency bugs
- P2P transfers implemented as **double-entry ledger** writes: every transfer produces a linked debit and credit row, never a single overwritten balance

**Concurrency Safety**
- `PESSIMISTIC_WRITE` row-level locking on wallet mutations
- **Deadlock-safe lock ordering**: wallets are always locked in a consistent order (sorted by ID) during transfers, preventing circular-wait deadlocks between simultaneous opposite-direction transfers

**Idempotency**
- Clients supply an `Idempotency-Key` header on money-moving requests
- Redis stores a hash of the original request + its response
- Duplicate requests return the cached result; reused keys with a *different* body are rejected

**Payment Order State Machine**
- Explicit, enforced states with transition logic living on the domain entity itself

**Fraud/Risk Engine**
- Independent, pluggable `FraudRule` implementations (large-amount detection, transaction velocity, suspicious test-amount detection)
- Spring auto-collects every `FraudRule` bean — adding a new rule requires zero orchestration changes
- Every check logged with its full score breakdown

**Event-Driven Architecture**
- **Transactional outbox pattern**: business state and its corresponding event are written in the same DB transaction, guaranteeing no event is lost even if Kafka is temporarily down
- Scheduled poller publishes to Kafka with automatic retry
- Kafka consumer reacts to `PaymentCompleted`/`PaymentFailed` events, fully decoupled from core payment logic

**Refunds**
- Modeled as a new compensating transaction, never a rewrite of history
- Reuses the existing state machine and transfer infrastructure

**Rate Limiting**
- Redis-backed fixed-window counter, applied declaratively via a custom `@RateLimit` annotation + AOP aspect

**Audit Logging**
- Custom `@Audited` annotation + AOP aspect logs every sensitive action — who, what, when, outcome — independent of transaction history

**Observability**
- Custom business metrics (`payments_completed`, `payments_failed{reason}`, `fraud_rejections`) alongside standard JVM/HTTP metrics
- Full distributed tracing across HTTP → service → database layers via Zipkin

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21+ |
| Framework | Spring Boot 4.x |
| Web | Spring Web (REST) |
| Security | Spring Security + JWT (jjwt) |
| Validation | Jakarta Validation |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Cache / Rate Limiting | Redis |
| Messaging | Apache Kafka |
| API Docs | springdoc-openapi (Swagger UI) |
| Metrics | Micrometer + Prometheus + Grafana |
| Tracing | Micrometer Tracing (Brave) + Zipkin |
| Build | Maven |
| Containerization | Docker + Docker Compose |

---

## Design Decisions & Highlights

Worth calling out in review or interview:

- **Double-entry ledger over mutable balance updates** — every transfer produces two immutable, linked ledger rows, matching how real financial systems maintain auditability.
- **Pessimistic over optimistic locking for wallet mutations** — an initial optimistic-locking implementation (JPA `@Version` + retry) was load-tested with concurrent withdrawals and found to fail a meaningful fraction of legitimate requests under high contention on a single row. Switched to `PESSIMISTIC_WRITE` with deadlock-safe ordering for correctness and predictability on this access pattern.
- **Outbox pattern over direct Kafka publish** — avoids the dual-write problem where a DB commit succeeds but the Kafka publish fails, silently desynchronizing state and events.
- **State machine logic lives on the domain entity**, not scattered across services — makes invalid transitions structurally difficult to introduce.
- **Database-enforced constraints as a safety net** — foreign keys and check constraints (e.g. blocking a wallet from transferring to itself) are enforced at the Postgres level, not only in application code.

---

## Quick Start

**Prerequisites:** JDK 21+, Maven, PostgreSQL, Docker

```bash
# 1. Clone
git clone https://github.com/roySagar2026/payflow_distributed_payment_system.git
cd payflow_distributed_payment_system

# 2. Create the database
psql -U postgres -c "CREATE DATABASE payflow;"

# 3. Configure application.properties (see Configuration below)

# 4. Start supporting infrastructure
docker compose up -d redis kafka zipkin prometheus grafana

# 5. Run the app
./mvnw spring-boot:run
```

App starts on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Installation

### Step 1 — Clone the repository
```bash
git clone https://github.com/roySagar2026/payflow_distributed_payment_system.git
cd payflow_distributed_payment_system
```

### Step 2 — Set up PostgreSQL
```sql
CREATE DATABASE payflow;
```

### Step 3 — Start infrastructure dependencies
```bash
docker compose up -d redis kafka zipkin prometheus grafana
```

### Step 4 — Build and run
```bash
./mvnw clean install
./mvnw spring-boot:run
```

---

## Configuration

`src/main/resources/application.properties`:

```properties
# Datasource
spring.datasource.url=jdbc:postgresql://localhost:5432/payflow
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

# JPA / Flyway
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

# JWT
payflow.jwt.secret=${JWT_SECRET}
payflow.jwt.access-token-expiration-ms=900000
payflow.jwt.refresh-token-expiration-ms=604800000

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# Tracing
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
management.tracing.sampling.probability=1.0
```

> Secrets (`DB_PASSWORD`, `JWT_SECRET`) are read from environment variables — set these locally or via your IDE run configuration rather than committing real values.

---

## API Reference

**Base URL:** `http://localhost:8080`

All endpoints except `/api/auth/**`, `/swagger-ui/**`, and actuator endpoints require a `Bearer <JWT>` header.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a user + auto-provision a wallet |
| `POST` | `/api/auth/login` | Authenticate, receive JWT tokens |
| `GET` | `/api/users/me` | Get current authenticated user |
| `POST` | `/api/wallets/deposit` | Deposit funds *(idempotent)* |
| `POST` | `/api/wallets/withdraw` | Withdraw funds *(idempotent, rate-limited)* |
| `POST` | `/api/transfers` | P2P transfer *(idempotent, rate-limited)* |
| `POST` | `/api/payment-orders` | Create & process a payment order |
| `GET` | `/api/payment-orders/{id}` | Get payment order status |
| `POST` | `/api/payment-orders/{id}/refund` | Refund a completed order |
| `GET` | `/api/notifications/me` | View your notification history |
| `GET` | `/api/audit-logs/me` | View your audit trail |

### Example — Register + Deposit

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@payflow.com","password":"password123","fullName":"Test User"}'

curl -X POST http://localhost:8080/api/wallets/deposit \
  -H "Authorization: Bearer <accessToken>" \
  -H "Idempotency-Key: <uuid>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 500}'
```

Full interactive documentation is available via Swagger UI at `/swagger-ui/index.html`, including a Bearer-token Authorize flow.

---

## Running with Docker

The entire system — application + Redis, Kafka, Zipkin, Prometheus, and Grafana — runs via Docker Compose:

```bash
docker compose up -d --build
```

This builds the app image from the multi-stage `Dockerfile` and starts every supporting service. See `docker-compose.yml` for full configuration.

---

## Monitoring & Observability

| Tool | URL | Purpose |
|---|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` | Interactive API docs |
| Actuator Health | `http://localhost:8080/actuator/health` | Liveness/readiness |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` | Raw metrics |
| Prometheus UI | `http://localhost:9090` | Query/inspect metrics |
| Grafana | `http://localhost:3000` | Dashboards (add Prometheus as data source) |
| Zipkin | `http://localhost:9411` | Distributed trace visualization |

**Custom business metrics:** `payflow_payments_completed_total`, `payflow_payments_failed_total{reason=...}`, `payflow_fraud_rejections_total`

---

## Project Structure

```
payflow/
├── src/main/java/com/payflow/payflow/
│   ├── audit/          # @Audited annotation + AOP aspect
│   ├── config/         # Security, OpenAPI configuration
│   ├── controller/     # REST controllers
│   ├── dto/            # Request/response records
│   ├── event/          # Kafka event payload records
│   ├── fraud/          # FraudRule interface + implementations
│   ├── model/          # JPA entities
│   ├── ratelimit/      # @RateLimit annotation + AOP aspect
│   ├── repository/     # Spring Data JPA repositories
│   ├── security/       # JWT service + auth filter
│   └── service/        # Business logic
├── src/main/resources/
│   └── db/migration/   # Flyway SQL migrations (V1 → V9)
├── Dockerfile           # Multi-stage build
├── docker-compose.yml   # Full infra: Redis, Kafka, Zipkin, Prometheus, Grafana
├── prometheus.yml
└── pom.xml
```

---

## Troubleshooting

**`NoSuchMethodError` on startup (springdoc / Jackson / Kafka related)**
Spring Boot 4 modularized several auto-configurations into dedicated starters. Ensure you're using `springdoc-openapi-starter-webmvc-ui:3.x` (not `2.x`), Jackson 3's `JsonMapper` (not the legacy `com.fasterxml.jackson.databind.ObjectMapper`), and `spring-boot-starter-kafka` (not plain `spring-kafka`).

**`spring-boot-starter-aop` not found**
This starter was removed in Spring Boot 4. Use `org.aspectj:aspectjweaver` directly instead — Spring AOP itself is included in `spring-boot-starter` by default.

**Actuator endpoints return 403/401**
Add `/actuator/**` to the permitted paths in `SecurityConfig`. For production, scope this down to specific endpoints or a separate management port.

**Containerized app can't reach a locally-installed Postgres**
Use `host.docker.internal` instead of `localhost` in the datasource URL when the app runs in Docker but Postgres runs on the host machine directly.

**Prometheus target shows "DOWN"**
Confirm the scrape target in `prometheus.yml` uses `host.docker.internal:8080` (host-run app) or the correct service name (containerized app), and that `/actuator/prometheus` is reachable without authentication.

---

## What This Project Is / Isn't

**Demonstrates:** correct financial data modeling, concurrency-safe transaction processing, idempotent API design, event-driven architecture, and observability — the internal engineering patterns real payment systems rely on.

**Does not include:** real money movement, banking rail integration (UPI/NPCI/ACH), KYC/identity verification, regulatory compliance (RBI/PCI-DSS), or a frontend. This is a backend architecture simulation, not a production payment processor.

---

## Roadmap

**Current**
- Full backend: auth, wallets, transfers, payment orders, fraud engine, refunds
- Event-driven notifications via Kafka outbox pattern
- Rate limiting, audit logging, metrics, and distributed tracing
- Fully Dockerized deployment

**Planned**
- Reconciliation job comparing internal ledger sums for consistency
- Role-based access control (admin vs. regular user)
- Event-type Kafka headers instead of envelope-based routing
- Sandbox integration with a real payment gateway (e.g. Razorpay test mode)
- Frontend client

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Author

**Sagar Roy**
GitHub: [@roySagar2026](https://github.com/roySagar2026)