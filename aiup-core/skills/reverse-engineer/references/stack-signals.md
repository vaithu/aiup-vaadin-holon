# Stack signals — where to find actors, use cases, and entities

This is a lookup, not a script. Use it after you've identified the project's
stack from build files. Sections are independent — read only the ones that
match the project in front of you.

## Java / Spring Boot (with Holon Platform)

- **Build files**: `pom.xml`, `build.gradle(.kts)`. Look for `com.holon-platform`
  BOM + starters to confirm Holon modules in use (e.g. `holon-datastore-jdbc`,
  `holon-vaadin-flow-spring-boot`, `holon-auth`, `holon-spring-boot`).
- **Entry points**:
  - Holon Vaadin Flow views: classes annotated `@Route(...)`.
  - Scheduled jobs: `@Scheduled`.
  - Message listeners: `@KafkaListener`, `@JmsListener`, `@EventListener`.
- **Actors**:
  - Holon Auth: role/permission definitions in a `Realm` bootstrap (`@Bean` returning `Realm`).
  - `AuthContext.require().isPermitted("role:action")` calls — these expose the permission model.
  - `@Permitted` annotations on `@Route` classes or action buttons.
- **Entities**:
  - Plain JavaBeans with `@com.holonplatform.core.beans.DataPath` and
    `@com.holonplatform.core.beans.Identifier` — these are the domain objects.
  - `BeanPropertySet<T>` constants reveal all queryable properties.
  - Flyway migrations in `src/main/resources/db/migration/V*.sql` — column names match
    the snake_case of bean field names.
- **Tests**: JUnit 5, Testcontainers, Vaadin Browserless (`vaadin-testbench-unit-junit`),
  Playwright. Test class names often encode use case IDs.

## Java / Spring Boot (plain — JPA / jOOQ)

- **Build files**: `pom.xml`, `build.gradle(.kts)`. Look for `spring-boot-starter-*`
  dependencies to confirm modules (`-web`, `-security`, `-data-jpa`, `-jooq`,
  `-thymeleaf`, etc.).
- **Entry points**:
  - `@RestController`, `@Controller` classes.
  - Vaadin views: classes annotated `@Route(...)`.
  - Scheduled jobs: `@Scheduled`.
  - Message listeners: `@KafkaListener`, `@RabbitListener`, `@JmsListener`,
    `@EventListener`.
- **Actors**:
  - `SecurityFilterChain` configuration — `requestMatchers(...).hasRole("X")`,
    `.authenticated()`, `.permitAll()`.
  - Method-level `@RolesAllowed`, `@PreAuthorize`, `@Secured`.
  - Custom `UserDetailsService` and any role/authority enum.
- **Entities**:
  - JPA: `@Entity` classes (relationships from `@OneToMany`, `@ManyToOne`,
    `@OneToOne`, `@ManyToMany`).
  - jOOQ: schema is in Flyway migrations (`src/main/resources/db/migration/V*.sql`)
    rather than annotated classes; the generated classes mirror the DDL.
  - Validation: Bean Validation annotations (`@NotNull`, `@Size`, `@Email`,
    `@Min`, `@Max`, `@Pattern`).
- **Tests**: `@SpringBootTest`, `@WebMvcTest`, Vaadin Browserless / Karibu
  view tests, Playwright tests under `src/test/`.

## Python / Django

- **Build files**: `requirements.txt`, `pyproject.toml`, `manage.py`.
- **Entry points**: `urls.py`, view functions, class-based views, DRF `ViewSet`s.
- **Actors**: Django `Group`/`Permission`, `LoginRequiredMixin`, `PermissionRequiredMixin`.
- **Entities**: `models.py` — `ForeignKey`, `OneToOneField`, `ManyToManyField`.
- **Tests**: `tests.py` or `tests/` directory; `TestCase` subclasses.

## Node.js / TypeScript / Express

- **Build files**: `package.json`. Look for `express`, `nestjs`, `next`.
- **Entry points**: Express routes, NestJS `@Controller`, Next.js `pages/api/`.
- **Actors**: JWT middleware, NestJS `@UseGuards(RolesGuard)`, NextAuth sessions.
- **Entities**: Prisma `schema.prisma`, TypeORM `@Entity`, Drizzle `pgTable`.

### Prisma → AIUP type mapping

| Prisma type                         | AIUP Data Type | Length/Precision | Validation Rules                  |
|-------------------------------------|----------------|------------------|-----------------------------------|
| `Int @id @default(autoincrement())` | Long           | 19               | Primary Key, Sequence             |
| `Int`                               | Integer        | 10               | Not Null                          |
| `String`                            | String         | 255              | Not Null                          |
| `String @unique`                    | String         | 255              | Not Null, Unique                  |
| `String?`                           | String         | 255              | Optional                          |
| `Decimal @db.Decimal(10, 2)`        | Decimal        | 10,2             | Not Null, Min: 0, Max: 9999.99    |
| `Boolean`                           | Boolean        | —                | Not Null                          |
| `DateTime @default(now())`          | DateTime       | —                | Not Null                          |
| relation field `userId Int`         | Long           | 19               | Not Null, Foreign Key (USER.id)   |
