# ADR-002: Microservices instead of a modular monolith

- Status: Accepted
- Date: 2026-09-20

## Context
The goal is to learn and to show professional practice, not to ship the simplest ERP.
We want to practise events, sagas, eventual consistency, idempotency and per-service data.
A modular monolith would be faster and simpler for a real small ERP.

## Decision
We build six services (customers, inventory, sales, purchasing, invoicing, accounting), plus a gateway and one frontend.
Each service owns its data and its API. Services talk through REST (queries) and Kafka (events).
We do not use distributed transactions. We use sagas and compensation.

## Consequences
- We get real distributed-system problems to solve: consistency, duplicates, failure of one service.
- Much more work: infrastructure, build, tests, observability, deployment.
- Local development is heavier. Dev mode (ADR-007) keeps it manageable.
- Service boundaries are guessed early. Wrong boundaries are expensive to fix, so we keep services small and review at each phase.

## Alternatives considered
- Modular monolith: simpler and faster, but it hides the problems we want to learn.
- Modular monolith first, split later: honest for real products, but the learning goal comes late.
