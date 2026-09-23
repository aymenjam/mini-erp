---
globs: services/*/src/main/java/**
paths: services/*/src/main/java/**
---
# Lombok (limited use)

- Allowed only in `infrastructure`, and only these annotations: `@Getter`, `@RequiredArgsConstructor`, `@Slf4j`,
  and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- `@NoArgsConstructor` only in exactly this form and only in `infrastructure.out.persistence`. Why: JPA needs a protected no-arg
  constructor, and constructors are not inherited, so every concrete JPA entity needs its own. No other argument (no `force`, no other access level).
- Not in `domain` (use records and explicit methods) and not in `application`.
- Never `@Data`, `@EqualsAndHashCode` or `@ToString` on JPA entities. In fact they are banned everywhere, like `@Setter`,
  `@Builder`, `@Value`, `@AllArgsConstructor`, `val`, `@SneakyThrows` and everything in `lombok.experimental`.
  Why: they hide equals/hashCode/toString problems with JPA (lazy loading, proxies), and they make the domain depend on a tool.
  `@AllArgsConstructor` also adds a constructor that skips the explicit ones: a new field would silently change it.
- Setters on JPA entities: see `jpa-entities.md`.
- Checked twice: `lombok.config` makes the banned features a compile error, and `LombokUsageTest` plus an ArchUnit rule
  fail the build when Lombok is used in the wrong package or with another annotation.
  `@NoArgsConstructor` is not in `lombok.config` (it cannot check the access level): only `LombokUsageTest` checks its form and package.
- A new Lombok feature needs an ADR first (baseline libraries, ADR-008). Enable annotation processing in your IDE.
