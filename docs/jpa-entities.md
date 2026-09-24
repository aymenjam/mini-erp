# JPA entities: rules and reasons

JPA entities live in `infrastructure.out.persistence`. They are a storage detail. The business rules are in the domain.
This page explains why we do not use setters on entities, and what to do instead.

## 1. Why no setters
A setter lets anyone change any field, at any time, and skip the business rules. Nothing says *why* the field changes.
A named method says what happens: `changeEmail(...)`, `applyFrom(...)`. It is also the one place to check a rule.

## 2. `id` and `version` belong to Hibernate
- `setVersion` breaks optimistic locking. Hibernate uses `version` to detect two writers on the same row.
- `setId` changes the identity of a row that is already saved.

Our code never sets them after the entity is created. `AbstractJpaEntity` has only getters for both.

## 3. A class-level `@Setter` is invisible
`@Setter` on a class creates a setter for **every** field, including `id` and `version`. Nobody sees these methods in the code,
so nobody reviews them. This is why `@Setter` is a compile error (`lombok.setter.flagUsage = error`).

## 4. How to create and change an entity
- **Create:** use a constructor or a static factory.
- **Update:** load the entity, then call **one** method of the entity that copies the fields. It never copies `id` or `version`.
- The persistence mapper is the only place that does this.

```java
// example, not in the template
@Entity
@Table(name = "customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerJpaEntity extends AbstractJpaEntity {

    @Getter
    private String name;
    @Getter
    private String email;

    public CustomerJpaEntity(UUID id, String name, String email) {
        super(id);
        this.name = name;
        this.email = email;
    }

    // Copies the business fields. Never id or version: Hibernate owns them.
    public void applyFrom(Customer customer) {
        this.name = customer.name();
        this.email = customer.email();
    }
}

// in the mapper or the adapter (example, not in the template)
repository.findById(customer.id())
        .map(entity -> { entity.applyFrom(customer); return entity; })
        .orElseGet(() -> new CustomerJpaEntity(customer.id(), customer.name(), customer.email()));
```

## 4b. Children of an aggregate (contacts and addresses of a customer)
A child table is mapped as `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)` on the parent, and `@ManyToOne(LAZY)` on the child.
Both are entities of `infrastructure.out.persistence`, and the children extend `AbstractJpaEntity` too (own id and `version`).
- **Change the list in place.** `applyFrom` of the parent matches the children by id: it updates the existing ones, adds the new ones and removes the
  missing ones from the same list. Never assign a new list: `orphanRemoval` would fail.
- **The parent version goes up** when a child changes, because `applyFrom` also copies `updatedAt` (the parent row is dirty). Two people who change
  the children of one customer at the same time get 409 for the second one.
- **A flag that must be unique** (primary contact, default address) has a partial unique index, and PostgreSQL cannot defer it. Hibernate writes new rows
  before it updates old rows, so "set the new flag" would run before "clear the old flag" and break the index. Solution: the adapter calls
  `releaseFlagsLostIn` first, flushes, and then calls `applyFrom` and flushes again (`CustomerPersistenceAdapter.save`).
- **Lazy children and lists.** Do not build the full domain object for a list: it would load the children of every row (N+1 queries). Lists use
  a small domain record (`CustomerSummary`) and `CustomerPersistenceMapper.toSummary`.

## 5. Why no `@Data`, `@EqualsAndHashCode`, `@ToString`
- **Identity:** a generated `equals` and `hashCode` use all fields. The hash changes when a field changes, so an entity in a `Set` or `Map` gets lost.
  Hibernate also uses proxies, and a proxy is not the same class as the entity.
- **Lazy loading:** a generated `toString` or `equals` reads every field. It can load related data by accident, or fail with `LazyInitializationException`.

**Policy (decided with the first real entity, `CustomerJpaEntity`):** we do not write `equals` and `hashCode` on entities.
An entity never leaves `infrastructure.out.persistence`: it is not compared and not put in a `Set` or `Map`. The mapper turns it into a
domain object, and the domain object is compared by its values (records) or by its `id`. If a case ever needs it, add an ADR first.

## 6. What is allowed
- `@Getter`
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, only in `infrastructure.out.persistence`. JPA needs it, and constructors are not inherited.

## 7. Where the rules are enforced
- `lombok.config`: banned features are a compile error.
- `LombokUsageTest` (rules L1–L5): fails the build when Lombok is in the wrong package or has another form.
- More: `.claude/rules/lombok.md` and [ADR-008](adr/008-baseline-libraries.md).
