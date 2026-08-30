# Application Configuration Reference

This file provides the canonical `application.properties` stubs for a
Holon Platform + Vaadin Flow + Spring Boot 4.x project.

Secrets (passwords, keys) must be externalised via environment variables —
never commit plaintext credentials.

---

## `application.properties` (full annotated template)

```properties
# ── Spring Boot ───────────────────────────────────────────────────────────────
spring.application.name=my-app

# ── DataSource (PostgreSQL) ───────────────────────────────────────────────────
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/mydb}
spring.datasource.username=${DB_USER:myuser}
spring.datasource.******
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# ── Flyway ────────────────────────────────────────────────────────────────────
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
# Safe for first run against an existing schema:
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true

# ── JPA / Hibernate timezone ──────────────────────────────────────────────────
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# ── Vaadin ────────────────────────────────────────────────────────────────────
# Enable dev tools in local development (disable in production)
vaadin.devtools.enabled=true
# EAGER loads the Vaadin servlet immediately (recommended)
vaadin.launch-mode=EAGER
# Heartbeat interval in seconds — keeps the session alive during long operations
vaadin.heartbeat-interval=300
vaadin.close-idle-sessions=false

# ── Holon Auth ────────────────────────────────────────────────────────────────
# The Realm bean is declared programmatically in @Configuration / @SpringBootApplication.
# This property names the realm in the Holon Spring Boot auth autoconfiguration.
holon.realm.name=my-app-realm

# ── Logging ───────────────────────────────────────────────────────────────────
logging.level.root=INFO
logging.level.com.example.myapp=DEBUG
logging.level.com.holonplatform=INFO
logging.level.org.flywaydb=INFO
logging.level.com.zaxxer.hikari=WARN
```

---

## `application-local.properties` (developer overrides — do not commit)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb_dev
spring.datasource.username=dev
spring.datasource.******

vaadin.devtools.enabled=true

logging.level.com.example.myapp=DEBUG
logging.level.com.holonplatform=DEBUG
```

Activate with `--spring.profiles.active=local` or `SPRING_PROFILES_ACTIVE=local`.

---

## `application-test.properties` (Testcontainers / CI)

```properties
# Testcontainers replaces the URL at test runtime via @DynamicPropertySource
spring.datasource.url=jdbc:tc:postgresql:15:///testdb
spring.datasource.username=test
spring.datasource.******

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

logging.level.com.example.myapp=DEBUG
```

---

## Required JVM arguments

Add the following to your run configuration or `JAVA_TOOL_OPTIONS` in CI:

```
-Duser.timezone=UTC
```

This prevents the PostgreSQL driver from silently shifting `TIMESTAMPTZ` values to
the JVM's local timezone when reading results.

For Spring Boot Maven plugin:

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <jvmArguments>-Duser.timezone=UTC</jvmArguments>
  </configuration>
</plugin>
```

---

## `pom.xml` BOM imports (reference)

```xml
<dependencyManagement>
  <dependencies>
    <!-- Holon Vaadin Flow BOM -->
    <dependency>
      <groupId>com.holon-platform.vaadin</groupId>
      <artifactId>holon-vaadin-flow-bom</artifactId>
      <version>10.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <!-- Vaadin BOM -->
    <dependency>
      <groupId>com.vaadin</groupId>
      <artifactId>vaadin-bom</artifactId>
      <version>25.2.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <!-- Spring Boot BOM -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>4.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- Spring Boot -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
  </dependency>

  <!-- Holon Vaadin Flow Spring Boot starter -->
  <dependency>
    <groupId>com.holon-platform.vaadin</groupId>
    <artifactId>holon-vaadin-flow-spring-boot</artifactId>
  </dependency>

  <!-- Holon JPA Datastore Spring Boot starter -->
  <dependency>
    <groupId>com.holon-platform.jpa</groupId>
    <artifactId>holon-datastore-jpa-spring-boot</artifactId>
    <version>10.0.0</version>
  </dependency>

  <!-- Holon Auth -->
  <dependency>
    <groupId>com.holon-platform.core</groupId>
    <artifactId>holon-auth</artifactId>
    <version>10.0.0</version>
  </dependency>

  <!-- PostgreSQL driver -->
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
  </dependency>

  <!-- Flyway PostgreSQL support -->
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
  </dependency>

  <!-- Jakarta Servlet API (Jakarta EE 11) -->
  <dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.1.0</version>
    <scope>provided</scope>
  </dependency>

  <!-- Jakarta Bean Validation -->
  <dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
  </dependency>

  <!-- Test -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## Holon auto-configuration notes

The Holon Spring Boot starters register the following beans automatically
(no manual `@Bean` declarations required):

| Starter | Auto-registered beans |
|---------|----------------------|
| `holon-datastore-jpa-spring-boot` | `Datastore`, `EntityManagerFactory` (backed by Spring Boot JPA autoconfiguration) |
| `holon-vaadin-flow-spring-boot` | Vaadin servlet, `Navigator` (request-scoped), `AuthContext` (session-scoped if a `Realm` bean is present) |

The `Realm` bean must still be declared manually — see `security-patterns.md`.
