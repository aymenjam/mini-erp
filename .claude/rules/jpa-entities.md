---
globs: services/*/src/main/java/**/persistence/**
paths: services/*/src/main/java/**/persistence/**
---
# JPA entities

- Never `@Data`, `@EqualsAndHashCode` or `@ToString` on JPA entities (banned everywhere, reasons in `lombok.md`).
- No `@Setter` and no setters on JPA entities. Why: anyone could change any field and skip the business rules, and `id` and `version`
  belong to Hibernate. Create an entity with a constructor or a static factory. To update, load it and call one method of the entity that
  copies the fields (never `id` or `version`). Only the persistence mapper does this.
- Details: `docs/jpa-entities.md`.
