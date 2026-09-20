# ADR-005: Kafka in KRaft mode + transactional outbox

- Status: Accepted
- Date: 2026-09-20

## Context
Services must exchange events reliably. Writing to the database and sending to Kafka are two systems,
so a crash between them loses or invents an event (dual write problem).
Kafka 4.x removed ZooKeeper.

## Decision
- Kafka runs in KRaft mode with the official `apache/kafka` image (single node in dev). No ZooKeeper.
- Schema Registry (Confluent `cp-schema-registry`) holds event schemas, so they can be versioned and checked for compatibility.
- We publish events with the transactional outbox: the use case saves the aggregate and an outbox row in the same database transaction.
  A relay reads the outbox and publishes to Kafka. Delivery is at least once.
- The relay is a polling publisher inside the service. It is simple and needs no extra infrastructure.
- Consumers are idempotent: they store processed `eventId`s.
- Topic and event naming: see `constitution.md`.

## Consequences
- No event is lost when Kafka is down: rows wait in the outbox. Duplicates are possible, so idempotent consumers are required.
- Extra table and relay code in each publishing service. Outbox rows must be cleaned up.
- Polling adds a small delay. If it becomes a problem, we can switch to change data capture (Debezium, needs Kafka Connect) with a new ADR.
- Confluent Schema Registry uses the Confluent Community License, which is not an OSI open-source licence. It is fine for this project.
  Apicurio Registry is the open alternative if this becomes a problem.
- The payload format (Avro, JSON Schema, Protobuf) is not decided here. A separate ADR comes before roadmap item 1.4.

## Alternatives considered
- Kafka with ZooKeeper: not possible with Kafka 4.x.
- Send to Kafka directly inside the use case: dual write, can lose events.
- Debezium CDC on the outbox table: no polling delay, but more infrastructure now.
- Kafka transactions across DB and Kafka: not possible. Kafka cannot join a database transaction.
