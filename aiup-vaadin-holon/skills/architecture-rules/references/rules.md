# ArchUnit Rule Source

Three test classes under `src/test/java/<base-package>/architecture/`, and one
annotation under `src/main/java/<base-package>/shared/` (production classes have
to be able to carry it).

**Change exactly two things:** the `package` declaration, and `BASE_PACKAGE`.

Every rule cites the section of
[`rules/holon-stack.md`](../../../rules/holon-stack.md) it enforces. A rule
nobody can trace to a decision gets deleted the first time it is inconvenient.

---

## `Fallback.java` (main source set)

```java
package com.example.app.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class that deliberately steps outside the Holon stack because Holon
 * has no equivalent for what it needs.
 *
 * <p>{@code holon-stack.md} allows this, justified inline with a
 * {@code // FALLBACK:} comment. ArchUnit cannot read comments, so this says the
 * same thing in a form a rule can check: the rules with a legitimate exception
 * skip an annotated class, and every exception in the codebase is therefore
 * greppable, reviewable and countable.
 *
 * <p>{@link #reason()} must name <em>what Holon could not do</em> — not that
 * the alternative was easier. No rule can judge that sentence; it is the one
 * place where a human still has to look.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Fallback {

    /** What Holon could not express, e.g. "Datastore cannot express a recursive CTE". */
    String reason();
}
```

## `ArchitectureTest.java`

```java
package com.example.app.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * The conventions of {@code rules/holon-stack.md}, made executable.
 *
 * <p>The test tree is deliberately excluded: conventions governing tests live in
 * {@link TestLayerConventionsTest}, because a rule like "a browserless test is
 * not named {@code *IT}" would otherwise have nothing to match here.
 *
 * <p>Rules with a legitimate exception skip classes annotated
 * {@code @Fallback}, which is the executable form of the {@code // FALLBACK:}
 * comment the stack rules ask for.
 */
@ArchTag("sensor")
@AnalyzeClasses(
        packages = ArchitectureTest.BASE_PACKAGE,
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    /** CHANGE ME — the root package of the project. */
    static final String BASE_PACKAGE = "com.example.app";

    private static final String FALLBACK = BASE_PACKAGE + ".shared.Fallback";

    // --- Banned dependencies (holon-stack.md "Banned Imports") ----------------

    @ArchTest
    static final ArchRule noSpringSecurity =
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.security..")
                    .because("holon-stack.md: security is Holon Auth — Realm, Authenticator, "
                            + "AuthContext, Permission, @Authenticate and @RolesAllowed. "
                            + "Two half-configured security models is worse than either.");

    @ArchTest
    static final ArchRule noSpringMvcRest =
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.web.bind.annotation..")
                    .because("holon-stack.md: Vaadin is the UI layer — there is no REST layer "
                            + "to add a controller to.");

    @ArchTest
    static final ArchRule noHibernateValidatorConstraints =
            noClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.hibernate.validator.constraints..")
                    .because("holon-stack.md: use jakarta.validation.constraints.* — "
                            + "@NotBlank, @Size, @Email — so the constraints stay portable.");

    @ArchTest
    static final ArchRule noVaadinI18nProvider =
            noClasses()
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.vaadin.flow.i18n.I18NProvider")
                    .because("holon-stack.md: localization is Holon's LocalizationContext — "
                            + "a second mechanism means two places to add a translation to.");

    @ArchTest
    static final ArchRule noDeprecatedThemeAnnotation =
            noClasses()
                    .should().beAnnotatedWith("com.vaadin.flow.theme.Theme")
                    .because("holon-stack.md: @Theme is removed in Vaadin 25.3 — use "
                            + "@StyleSheet(Lumo.STYLESHEET) then @StyleSheet(\"styles.css\") "
                            + "on the AppShellConfigurator.");

    /**
     * The Holon persistence container is not the domain model: the domain is a
     * plain JavaBean with {@code @DataPath} / {@code @Identifier}, described by a
     * {@code BeanPropertySet}.
     */
    @ArchTest
    static final ArchRule noDynamicPropertyContainerOutsideFallback =
            noClasses()
                    .that().areNotAnnotatedWith(FALLBACK)
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.holonplatform.core.property.PropertyBox")
                    .because("holon-stack.md: the domain is a plain JavaBean described by a "
                            + "BeanPropertySet. Annotate @Fallback if a Holon API genuinely "
                            + "hands one back and there is no typed alternative.");

    @ArchTest
    static final ArchRule noJpaOutsideFallback =
            noClasses()
                    .that().areNotAnnotatedWith(FALLBACK)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "javax.persistence..",
                            "org.springframework.data.jpa..",
                            "org.springframework.data.repository..")
                    .because("holon-stack.md: persistence is the Holon Datastore. JPA and "
                            + "Spring Data are a fallback for a query the Datastore cannot "
                            + "express — mark the class @Fallback(reason = ...) and say which "
                            + "query that is.");

    // --- Injection (holon-stack.md "Banned Imports", @Autowired row) ----------

    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
            .because("holon-stack.md: inject through the constructor. Field and setter "
                    + "injection hide a dependency from everyone reading the constructor, "
                    + "and from every test trying to build the object.");

    @ArchTest
    static final ArchRule noSetterInjection =
            noMethods()
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("holon-stack.md: inject through the constructor.");

    // --- Boundaries (holon-stack.md "Transaction Boundaries") ----------------

    /**
     * The unit of work is a service method. A view that queries the Datastore
     * directly has put the transaction boundary in the UI, where a user can hold
     * it open by not clicking anything.
     */
    @ArchTest
    static final ArchRule viewsDoNotTouchTheDatastore =
            noClasses()
                    .that().haveSimpleNameEndingWith("View")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.holonplatform.core.datastore..",
                            "com.holonplatform.jdbc..")
                    .because("holon-stack.md: the transaction boundary lives in the service. "
                            + "A view calls its <Entity>Service and nothing below it.");

    @ArchTest
    static final ArchRule onlyServicesAreTransactional =
            noMethods()
                    .that().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Service")
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .because("holon-stack.md: a single service method is the unit of work. "
                            + "@Transactional anywhere else either does nothing or moves the "
                            + "boundary somewhere nobody expects it.");

    @ArchTest
    static final ArchRule beansDoNotDependOnViews =
            noClasses()
                    .that().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("View")
                    .because("holon-stack.md: the dependency runs view -> service -> datastore. "
                            + "A service that knows a view cannot be reused by another one.");

    // --- Structure (holon-stack.md "Naming & Package Conventions") -----------

    /**
     * Package by feature: a bean, its model interface, its service and its views
     * share one package. {@code shared} is the only cross-cutting package.
     */
    @ArchTest
    static final ArchRule noLayerPackages =
            noClasses()
                    .should().resideInAnyPackage("..domain..", "..service..", "..repository..",
                            "..ui..", "..dto..", "..impl..")
                    .because("holon-stack.md: organise by feature, not by layer. Everything "
                            + "about customers lives in the customer package; only genuinely "
                            + "cross-cutting classes go in shared.");

    @ArchTest
    static final ArchRule routesAreViews =
            classes()
                    .that().areAnnotatedWith("com.vaadin.flow.router.Route")
                    .should().haveSimpleNameEndingWith("View")
                    .allowEmptyShould(true)
                    .because("holon-stack.md: views end in View — BillListView, "
                            + "BillDetailView. The other rules here rely on that suffix.");

    @ArchTest
    static final ArchRule servicesAreNotViews =
            noClasses()
                    .that().haveSimpleNameEndingWith("Service")
                    .should().beAnnotatedWith("com.vaadin.flow.router.Route")
                    .because("holon-stack.md: a service is not routable.");

    // --- Logging (holon-stack.md "Naming & Package Conventions") -------------

    @ArchTest
    static final ArchRule noConsoleOutput = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .because("holon-stack.md: log through SLF4J. System.out is invisible to every "
                    + "log aggregator the application will ever run behind.");
}
```

## `TestLayerConventionsTest.java`

```java
package com.example.app.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The conventions that govern the test tree, which {@link ArchitectureTest}
 * deliberately excludes.
 *
 * <p>A misnamed test is the quietest failure this project has: nothing errors,
 * the wrong Maven plugin simply never picks the class up, and it reports as
 * passing <em>by never running at all</em>. That is worth a rule rather than a
 * paragraph.
 *
 * <p>Only the two dangerous directions are asserted. Plenty of {@code *Test}
 * classes are neither view tests nor journeys — the traceability sensors, this
 * class — so "every {@code *Test} extends something" would be false.
 *
 * <p><strong>Adjust the two base types</strong> to whatever the project's test
 * skills produce: {@code SpringBrowserlessTest} for browserless view tests, and
 * the Playwright base class for {@code *IT} journeys. If the project has no
 * Playwright layer yet, delete the second rule rather than pointing it at a
 * class that does not exist.
 */
@ArchTag("sensor")
@AnalyzeClasses(packages = ArchitectureTest.BASE_PACKAGE)
public class TestLayerConventionsTest {

    @ArchTest
    static final ArchRule browserlessTestsAreNotNamedIT =
            noClasses()
                    .that().areAssignableTo("com.vaadin.browserless.SpringBrowserlessTest")
                    .should().haveSimpleNameEndingWith("IT")
                    .allowEmptyShould(true)
                    .because("Surefire runs *Test and Failsafe runs *IT — a browserless test "
                            + "named *IT is skipped by `mvn test` and runs in the wrong phase.");

    @ArchTest
    static final ArchRule playwrightTestsAreNotNamedTest =
            noClasses()
                    .that().haveSimpleNameEndingWith("Test")
                    .should().dependOnClassesThat().resideInAPackage("com.microsoft.playwright..")
                    .allowEmptyShould(true)
                    .because("Failsafe runs *IT — a Playwright test named *Test starts a "
                            + "browser inside the unit-test phase, where nothing is deployed.");

    /** Setup and cleanup talk to the database too; the ban holds there as well. */
    @ArchTest
    static final ArchRule testsDoNotUseSpringSecurity = ArchitectureTest.noSpringSecurity;
}
```

## `SourceConventionsTest.java`

Two conventions cannot be expressed in bytecode: a SQL string is a constant
ArchUnit does not expose, and a `@Fallback` reason is prose. This class reads
`src/main/java` as text instead — the same technique the traceability sensors
use on `docs/`.

```java
package com.example.app.architecture;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Conventions that live in the source text rather than in the bytecode.
 *
 * <p>ArchUnit reads compiled classes, where a string constant has no owner and a
 * comment does not exist. These two rules therefore read the files.
 */
@Tag("sensor")
class SourceConventionsTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    /** A SQL verb at the start of a string literal. */
    private static final Pattern PLAIN_SQL = Pattern.compile(
            "\"\\s*(SELECT|INSERT\\s+INTO|UPDATE|DELETE\\s+FROM|MERGE\\s+INTO)\\s",
            Pattern.CASE_INSENSITIVE);

    /** A fallback that says nothing. */
    private static final Pattern EMPTY_FALLBACK = Pattern.compile(
            "@Fallback\\s*\\(\\s*reason\\s*=\\s*\"\\s*(|n/?a|tbd|todo|none|because)\\s*\"",
            Pattern.CASE_INSENSITIVE);

    @Test
    void theSourceTreeIsDiscovered() {
        assertTrue(Files.isDirectory(SOURCE_ROOT),
                "No sources at " + SOURCE_ROOT.toAbsolutePath()
                        + ". Tests must run with the project root as working directory.");
    }

    @Test
    void noPlainSqlInSources() throws IOException {
        report("SQL written as a string instead of a Datastore query",
                "holon-stack.md: build queries with the Datastore's typed API. A SQL string "
                        + "is not checked by the compiler and rots silently when a column is "
                        + "renamed. Mark the class @Fallback if the Datastore cannot express it.",
                matches(PLAIN_SQL, true));
    }

    @Test
    void everyFallbackGivesAReason() throws IOException {
        report("@Fallback annotations without a usable reason",
                "The reason must name what Holon could not do — it is the only part of a "
                        + "fallback a reviewer can judge.",
                matches(EMPTY_FALLBACK, false));
    }

    private static List<String> matches(Pattern pattern, boolean skipFallbackClasses)
            throws IOException {
        List<String> found = new ArrayList<>();
        if (!Files.isDirectory(SOURCE_ROOT)) {
            return found;
        }
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                if (skipFallbackClasses && source.contains("@Fallback")) {
                    continue;
                }
                List<String> lines = source.lines().toList();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.stripLeading().startsWith("//") || line.stripLeading().startsWith("*")) {
                        continue;
                    }
                    if (pattern.matcher(line).find()) {
                        found.add(file + ":" + (i + 1) + "  " + line.trim());
                    }
                }
            }
        }
        return found;
    }

    private static void report(String headline, String advice, List<String> violations) {
        if (!violations.isEmpty()) {
            fail(headline + " (" + violations.size() + "):" + System.lineSeparator()
                    + violations.stream().collect(
                            joining(System.lineSeparator() + "  ", "  ", ""))
                    + System.lineSeparator() + System.lineSeparator() + advice);
        }
    }
}
```

---

## Notes

- **`areNotAnnotatedWith(String)`** takes the fully qualified annotation name, so
  the rules compile whether or not `@Fallback` has been created yet.
- **`areAssignableTo(String)`** does the same for the test base classes, which
  means `TestLayerConventionsTest` compiles in a project that has no
  browserless layer. Combined with `allowEmptyShould(true)`, the rule then
  passes vacuously
  rather than failing to compile — delete it if the layer is never coming.
- **`noLayerPackages` is the rule most likely to fail on adoption** of an
  existing codebase, and the most valuable to fix: a `service/` package is
  usually the first sign that package-by-feature was abandoned.
- **`NO_CLASSES_SHOULD_USE_FIELD_INJECTION`** covers `@Autowired`, `@Inject` and
  `@Resource` on fields; the separate `noSetterInjection` rule catches the
  method form, which it does not.
- **`skipFallbackClasses`** is deliberately coarse: a class carrying any
  `@Fallback` is exempt from the SQL scan as a whole, because a fallback query
  method is usually accompanied by helpers. The annotation's `reason()` is what
  review reads.
