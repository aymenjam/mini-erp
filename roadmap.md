# Roadmap — Mini-ERP

Status: [ ] todo, [~] in progress, [x] done.
Rule: after each phase we review this file (replanning) before the next phase starts.
Versions and conventions are in `constitution.md`. Important decisions are in `docs/adr/`.

## Phase 0 — Foundations

Goal: set the rules and prove the full chain works before any business code.

- [x] 0.1 Repo and build: Maven multi-module (Java 21, Spring Boot 3.5), .gitignore,
  basic CI on GitLab (`.gitlab-ci.yml`, with Docker access for Testcontainers)
- [x] 0.2 Dev infrastructure (docker-compose.dev.yaml: PostgreSQL, Kafka KRaft,
  Schema Registry, Keycloak)
- [ ] 0.3 Service template (hexagonal layout, ArchUnit, Flyway, Testcontainers,
  resource server security). ADR: baseline libraries. Check Kafka client 3.9 with broker 4.3.
- [ ] 0.4 Walking skeleton (Angular login with Keycloak -> gateway -> customers).
  ADR: API gateway (Spring Cloud Gateway, JWT validation, CORS).

Exit criterion: a user logs in with Keycloak in Angular and sees a customer list
(even empty) served by `customers` through the gateway. The backend chain is covered by
automated tests (gateway + JWT + CORS + customers) and the frontend by tests with mocks.
The login screen itself is checked by hand.

## Phase 1 — Reference data and first event

Goal: base data and reliable event publishing.

- [ ] 1.1 customers: CRUD, contacts, addresses, validation, pagination, search
- [ ] 1.2 inventory: item catalog (prices as BigDecimal + currency), one warehouse,
  stock levels, manual stock movements
- [ ] 1.3 Angular screens with role-based access
- [ ] 1.4 Transactional outbox with a polling relay: CustomerCreated and ItemCreated
  on Kafka. ADR: event format (Avro, JSON Schema or Protobuf) and schema versioning.
- [ ] 1.5 Dependency vulnerability scan in CI (needed because Spring Boot 3.5 has no
  more open-source patches). Small ADR.

Exit criterion: creating a customer publishes an event, and a test consumer receives it.
No event is lost when Kafka is stopped and restarted.

## Phase 2 — Sales, stock reservation and delivery

Goal: the first real cross-service flow, up to a delivered order.

- [ ] 2.1 sales: quotes, orders, statuses (draft, confirmed, rejected, cancelled,
  shipped, delivered)
- [ ] 2.2 ADR first, before any saga code: orchestration or choreography, and the stock
  rule (when stock is reserved, when it is decremented)
- [ ] 2.3 Stock reservation: StockReserved / StockReservationFailed
- [ ] 2.4 Saga with compensation (cancel, out of stock)
- [ ] 2.5 Idempotent consumers, duplicate handling, local customer copy in sales
- [ ] 2.6 Shipment and delivery: OrderShipped, OrderDelivered, stock decremented
  according to the ADR
- [ ] 2.7 Light contract tests for the events between sales and inventory

Exit criterion: end-to-end scenarios pass (success, out of stock, cancellation,
duplicate event, delivery), and two concurrent orders for the last item never oversell it.

## Phase 3 — Purchasing

Goal: close the stock cycle.

- [ ] 3.1 purchasing: suppliers, purchase orders
- [ ] 3.2 Partial goods receipts, GoodsReceived feeds inventory
- [ ] 3.3 Minimum stock alert and purchase suggestion

Exit criterion: a partial receipt increases stock by the right amount.
A purchase order closes only when everything is received.

## Phase 4 — Invoicing

Goal: the most regulated part.

- [ ] 4.1 invoicing: invoice from a delivered order, lines, totals.
  VAT rates are owned by invoicing.
- [ ] 4.2 Gap-free sequential numbering, immutable after issue
- [ ] 4.3 Credit notes, PDF generation, payment status
- [ ] 4.4 Events: InvoiceIssued, CreditNoteIssued

Exit criterion: an issued invoice cannot be changed, numbering stays continuous
under concurrency, and a credit note cancels the invoice.

## Phase 5 — Accounting

Goal: a purely reactive service.

- [ ] 5.1 accounting: chart of accounts (ADR: what we keep from the French PCG)
- [ ] 5.2 Double-entry journal entries built from events
- [ ] 5.3 Sales and purchase journals, trial balance
- [ ] 5.4 ACCOUNTANT role and screens

Exit criterion: the balance is always balanced, and replaying the whole topic
rebuilds exactly the same entries (business topics keep events forever).

## Phase 6 — Hardening and operations

Goal: what makes it professional.

- [ ] 6.1 Observability: OpenTelemetry traces across services, metrics, dashboards
- [ ] 6.2 Resilience: retries, dead-letter topics, timeouts, circuit breakers
- [ ] 6.3 Contract tests for all services, schema evolution rules
- [ ] 6.4 Full CI/CD and container deployment (Kubernetes/Helm optional)
- [ ] 6.5 Load tests on the order flow
- [ ] 6.6 Migrate to Spring Boot 4.x (with the matching Spring Cloud train).
- [ ] 6.7 Browser end-to-end tests on the main user flows (with an ADR for the tool)
  Can be moved earlier if a security issue requires it.
  Exit criterion: one order can be traced through all services, the system survives the
  failure of one service without data loss, and all tests pass on Spring Boot 4.x.

## Replanning log

- 2026-09-20: v2 after the Phase 0 plan review. Added ADRs (baseline libraries, gateway,
  event format), moved the saga ADR before saga code, added shipment and delivery,
  contract tests in Phase 2, vulnerability scan, VAT ownership, Boot 4.x migration.
- 2026-09-20: Phase 0 exit criterion clarified (backend chain automated, login screen
  checked by hand). Added item 6.7 (browser end-to-end tests).
