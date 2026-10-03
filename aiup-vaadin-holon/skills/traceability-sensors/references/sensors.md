# Sensor Source

Six files, written to `src/test/java/<base-package>/traceability/`.

**Change exactly two things:** the `package` declaration in every file, and the
`BASE_PACKAGE` constant in `UseCaseTraceabilityTest` and
`TestCaseTraceabilityTest`. Everything else is a parser — adjusting a regex to
make a build pass removes the guardrail rather than fixing the drift.

Paths are resolved relative to the working directory, which Maven sets to the
project root. Each sensor has a guard test that fails when it finds no input,
because a sensor that reports green because it is blind is the worst outcome
available.

---

## `UseCase.java`

```java
package com.example.app.traceability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the part of a use case specification that one test method exercises.
 *
 * <p>Sits on the method, because a use case has one coverage unit per scenario
 * and per business rule. {@code UseCaseTraceabilityTest} resolves every value
 * against {@code docs/use_cases/}, so the annotation cannot drift away from the
 * specification it claims to cover.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseCase {

    /** The {@code UC-NNN} id from the specification's Overview. */
    String id();

    /**
     * The main scenario, or an alternative flow's heading id <em>and</em> name
     * exactly as written in the document: {@code "A1: Duplicate Pet Name"}.
     */
    String scenario() default "Main Success Scenario";

    /**
     * The use case's own {@code BR-NNN} ids this test exercises. Never a
     * {@code GR-NNN}: {@code BusinessRuleTraceabilityTest} follows the
     * {@code BR -> GR} link itself.
     */
    String[] businessRules() default {};
}
```

## `TestCase.java`

```java
package com.example.app.traceability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the journey test that automates a {@code docs/test_cases/TC-NNN-*.md}
 * document, and names the use cases the journey walks through.
 *
 * <p>A test case has one coverage unit — the journey — so this sits on the
 * class, where {@link UseCase} sits on a method. {@code useCases} is what the
 * class name cannot express: {@code TestCaseTraceabilityTest} compares it with
 * the {@code [UC-NNN](...)} links in the document's Flow table, in both
 * directions.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestCase {

    /** The {@code TC-NNN} id from the document's Overview. */
    String id();

    /** Every {@code UC-NNN} the Flow table links to, in any order. */
    String[] useCases();
}
```

## `SpecDocuments.java`

```java
package com.example.app.traceability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The markdown handling the sensors share.
 *
 * <p>Use case and test case documents have the same Overview shape and report
 * their findings as one work list. Keeping the {@code **Status:**} pattern and
 * the reporting idiom here means a change to either is made once, rather than
 * made twice and eventually only once.
 */
final class SpecDocuments {

    /** Every AI Unified Process document carries this line in its Overview. */
    static final Pattern STATUS =
            Pattern.compile("^\\*\\*Status:\\*\\*[ \t]*(.*)$", Pattern.MULTILINE);

    private SpecDocuments() {
    }

    /**
     * The specification files in {@code directory}, sorted by file name. Only
     * {@code <prefix>*.md} counts, so a README living beside them is skipped
     * rather than failing the parser. A missing directory yields an empty list;
     * the callers' guard tests turn that into a failure.
     */
    static List<Path> filesIn(Path directory, String prefix) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> {
                        String name = file.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(".md");
                    })
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        }
    }

    /** The first capture of {@code pattern}, or a failure naming the missing line. */
    static String require(Pattern pattern, String markdown, Path file, String expected) {
        Matcher matcher = pattern.matcher(markdown);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    file + " has no '" + expected + "' line in its Overview section");
        }
        return normalize(matcher.group(1));
    }

    /** Collapses the whitespace that markdown hard breaks leave behind. */
    static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * The GitHub heading anchor for {@code "GR-006: Unique Pet Name per Owner"}:
     * lowercase, punctuation dropped, spaces turned into hyphens.
     */
    static String slug(String heading) {
        return normalize(heading)
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9 -]", "")
                .replace(' ', '-');
    }

    /**
     * Fails with every violation at once rather than stopping at the first, so
     * the report reads as a work list.
     */
    static void assertNoViolations(String headline, Stream<String> violations) {
        List<String> found = violations.sorted().toList();
        if (!found.isEmpty()) {
            fail(headline + " (" + found.size() + "):" + System.lineSeparator()
                    + found.stream().collect(
                            joining(System.lineSeparator() + "  ", "  ", "")));
        }
    }
}
```

## `UseCaseTraceabilityTest.java`

```java
package com.example.app.traceability;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.example.app.traceability.SpecDocuments.assertNoViolations;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for {@code docs/use_cases/}.
 *
 * <p><strong>Referential integrity.</strong> Every {@code @UseCase} annotation
 * must point at a use case, a scenario and business rules that exist. This
 * applies to every specification whatever its status, because a dangling
 * reference is always a defect.
 *
 * <p><strong>Coverage.</strong> A specification whose status is in
 * {@link #CLAIMS_COMPLETE} must additionally have a test behind its main
 * success scenario, behind every alternative flow, and behind every business
 * rule. Any other status is exempt: this process specifies before it builds, so
 * an unimplemented use case is a normal intermediate state. The status line is
 * what turns the coverage requirement on — which is why it must not be set by
 * hand without evidence.
 */
@Tag("sensor")
class UseCaseTraceabilityTest {

    /** CHANGE ME — the root package of the project under test. */
    private static final String BASE_PACKAGE = "com.example.app";

    private static final Path USE_CASE_DIR = Path.of("docs", "use_cases");

    /** The statuses that assert the use case is finished. */
    private static final Set<String> CLAIMS_COMPLETE = Set.of("Done", "Tested");

    /** Read from the annotation itself so the two cannot drift apart. */
    private static final String MAIN_SCENARIO = mainScenarioDefault();

    private static final Pattern USE_CASE_ID = Pattern.compile(
            "^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    private static final Pattern ALTERNATIVE_FLOW = Pattern.compile(
            "^### (A\\d+):[ \t]*(.+)$", Pattern.MULTILINE);
    private static final Pattern BUSINESS_RULE = Pattern.compile(
            "^### (BR-\\d{3}):[ \t]*.+$", Pattern.MULTILINE);

    /**
     * {@code UC004FindOwnersByLastNameTest}, or {@code ...IT} for a use case
     * that needs a browser. The suffix picks the Maven phase, not the kind of
     * coverage.
     */
    private static final Pattern USE_CASE_CLASS = Pattern.compile("^UC(\\d{3})\\w*(Test|IT)$");

    private static List<UseCaseSpec> specifications;
    private static Map<String, UseCaseSpec> byId;
    private static List<TestReference> references;
    private static List<String> testClassNames;

    /** One {@code docs/use_cases/UC-NNN-*.md} file. */
    private record UseCaseSpec(String id, String status, Set<String> alternativeFlows,
                               Set<String> businessRules, Path file) {

        boolean claimsComplete() {
            return CLAIMS_COMPLETE.contains(status);
        }
    }

    /** One {@code @UseCase} annotation on one test method. */
    private record TestReference(String useCaseId, String scenario, List<String> businessRules,
                                 String location) {
    }

    @BeforeAll
    static void loadSpecificationsAndTests() throws IOException {
        specifications = readSpecifications();
        byId = specifications.stream()
                .collect(java.util.stream.Collectors.toMap(
                        UseCaseSpec::id, spec -> spec, (first, second) -> first));
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
        references = readTestReferences(classes);
        testClassNames = readTestClassNames(classes);
    }

    // --- Guard ---------------------------------------------------------------

    /**
     * Without this, a wrong working directory would make every check below pass
     * over an empty input set — a sensor that is green because it is blind.
     */
    @Test
    void specificationsAndAnnotationsAreDiscovered() {
        assertFalse(specifications.isEmpty(),
                "No use case specifications found in " + USE_CASE_DIR.toAbsolutePath()
                        + ". Tests must run with the project root as working directory.");
        assertFalse(references.isEmpty(),
                "No @UseCase annotations found on the test classpath under " + BASE_PACKAGE);
    }

    @Test
    void useCaseIdsAreUnique() {
        assertNoViolations("use case ids claimed by more than one file",
                specifications.stream()
                        .collect(groupingBy(UseCaseSpec::id,
                                mapping(spec -> spec.file().toString(),
                                        toCollection(TreeSet::new))))
                        .entrySet().stream()
                        .filter(entry -> entry.getValue().size() > 1)
                        .map(entry -> entry.getKey() + " in " + String.join(", ", entry.getValue())));
    }

    /**
     * Ids are unique across the project's use cases, so an annotation naming a
     * {@code BR} that belongs to a different use case is a defect this catches
     * at its source.
     */
    @Test
    void businessRuleIdsAreUniqueAcrossUseCases() {
        assertNoViolations("business rule ids declared in more than one use case",
                specifications.stream()
                        .flatMap(spec -> spec.businessRules().stream()
                                .map(rule -> Map.entry(rule, spec.id())))
                        .collect(groupingBy(Map.Entry::getKey,
                                mapping(Map.Entry::getValue, toCollection(TreeSet::new))))
                        .entrySet().stream()
                        .filter(entry -> entry.getValue().size() > 1)
                        .map(entry -> entry.getKey() + " in " + String.join(", ", entry.getValue())));
    }

    // --- Referential integrity: annotations must point at something real ------

    @Test
    void everyAnnotationPointsAtAKnownUseCase() {
        assertNoViolations("@UseCase annotations naming an unknown use case",
                references.stream()
                        .filter(reference -> !byId.containsKey(reference.useCaseId()))
                        .map(reference -> reference.location() + " names " + reference.useCaseId()));
    }

    @Test
    void everyAnnotationPointsAtAKnownScenario() {
        assertNoViolations("@UseCase annotations naming an unknown scenario",
                references.stream()
                        .filter(reference -> byId.containsKey(reference.useCaseId()))
                        .filter(reference -> !MAIN_SCENARIO.equals(reference.scenario()))
                        .filter(reference -> !byId.get(reference.useCaseId())
                                .alternativeFlows().contains(reference.scenario()))
                        .map(reference -> reference.location() + " names \"" + reference.scenario()
                                + "\", which is not a flow of " + reference.useCaseId()
                                + " (known: " + String.join("; ",
                                        byId.get(reference.useCaseId()).alternativeFlows()) + ")"));
    }

    @Test
    void everyAnnotationPointsAtAKnownBusinessRule() {
        assertNoViolations("@UseCase annotations naming an unknown business rule",
                references.stream()
                        .filter(reference -> byId.containsKey(reference.useCaseId()))
                        .flatMap(reference -> reference.businessRules().stream()
                                .filter(rule -> !byId.get(reference.useCaseId())
                                        .businessRules().contains(rule))
                                .map(rule -> reference.location() + " names " + rule
                                        + ", which is not a rule of " + reference.useCaseId()))); 
    }

    @Test
    void everyUseCaseTestClassNamesAKnownUseCase() {
        assertNoViolations("test classes named after a use case that does not exist",
                testClassNames.stream()
                        .map(USE_CASE_CLASS::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> Map.entry(matcher.group(0), "UC-" + matcher.group(1)))
                        .filter(entry -> !byId.containsKey(entry.getValue()))
                        .map(entry -> entry.getKey() + " implies " + entry.getValue()));
    }

    // --- Coverage: a completed specification must be covered ------------------

    @Test
    void everyCompletedUseCaseCoversItsMainScenario() {
        assertNoViolations("use cases claiming completion with no test for the main scenario",
                completed()
                        .filter(spec -> coveredScenarios(spec.id()).noneMatch(MAIN_SCENARIO::equals))
                        .map(spec -> spec.id() + " (" + spec.status() + ") has no @UseCase(id = \""
                                + spec.id() + "\") on any test method"));
    }

    @Test
    void everyCompletedUseCaseCoversEveryAlternativeFlow() {
        assertNoViolations("alternative flows of a completed use case with no test",
                completed().flatMap(spec -> {
                    Set<String> covered = coveredScenarios(spec.id())
                            .collect(toCollection(LinkedHashSet::new));
                    return spec.alternativeFlows().stream()
                            .filter(flow -> !covered.contains(flow))
                            .map(flow -> spec.id() + " (" + spec.status() + ") \"" + flow + "\"");
                }));
    }

    @Test
    void everyCompletedUseCaseCoversEveryBusinessRule() {
        assertNoViolations("business rules of a completed use case with no test",
                completed().flatMap(spec -> {
                    Set<String> covered = references.stream()
                            .filter(reference -> reference.useCaseId().equals(spec.id()))
                            .flatMap(reference -> reference.businessRules().stream())
                            .collect(toCollection(LinkedHashSet::new));
                    return spec.businessRules().stream()
                            .filter(rule -> !covered.contains(rule))
                            .map(rule -> spec.id() + " (" + spec.status() + ") " + rule
                                    + " is named by no @UseCase(businessRules = ...)");
                }));
    }

    @Test
    void everyCompletedUseCaseHasATestClassNamedAfterIt() {
        assertNoViolations("use cases claiming completion with no UC<NNN><Name>Test/IT class",
                completed().filter(spec -> {
                    String prefix = "UC" + spec.id().substring(3);
                    return testClassNames.stream().noneMatch(name ->
                            USE_CASE_CLASS.matcher(name).matches() && name.startsWith(prefix));
                }).map(spec -> spec.id() + " (" + spec.status() + ") expects a class named UC"
                        + spec.id().substring(3) + "<Name>Test or UC" + spec.id().substring(3)
                        + "<Name>IT"));
    }

    // --- Parsing ---------------------------------------------------------------

    private static Stream<UseCaseSpec> completed() {
        return specifications.stream().filter(UseCaseSpec::claimsComplete);
    }

    private static Stream<String> coveredScenarios(String useCaseId) {
        return references.stream()
                .filter(reference -> reference.useCaseId().equals(useCaseId))
                .map(TestReference::scenario);
    }

    private static List<UseCaseSpec> readSpecifications() throws IOException {
        List<UseCaseSpec> specs = new ArrayList<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            Set<String> flows = new LinkedHashSet<>();
            Matcher flow = ALTERNATIVE_FLOW.matcher(markdown);
            while (flow.find()) {
                flows.add(SpecDocuments.normalize(flow.group(1) + ": " + flow.group(2)));
            }
            Set<String> rules = new LinkedHashSet<>();
            Matcher rule = BUSINESS_RULE.matcher(markdown);
            while (rule.find()) {
                rules.add(rule.group(1));
            }
            specs.add(new UseCaseSpec(
                    SpecDocuments.require(USE_CASE_ID, markdown, file, "**Use Case ID:**"),
                    SpecDocuments.require(SpecDocuments.STATUS, markdown, file, "**Status:**"),
                    flows, rules, file));
        }
        return specs;
    }

    private static List<TestReference> readTestReferences(JavaClasses classes) {
        List<TestReference> found = new ArrayList<>();
        for (JavaClass type : classes) {
            for (JavaMethod method : type.getMethods()) {
                if (!method.isAnnotatedWith(UseCase.class)) {
                    continue;
                }
                UseCase annotation = method.getAnnotationOfType(UseCase.class);
                found.add(new TestReference(
                        annotation.id(),
                        SpecDocuments.normalize(annotation.scenario()),
                        List.of(annotation.businessRules()),
                        type.getSimpleName() + "#" + method.getName()));
            }
        }
        return found;
    }

    private static List<String> readTestClassNames(JavaClasses classes) {
        List<String> names = new ArrayList<>();
        for (JavaClass type : classes) {
            names.add(type.getSimpleName());
        }
        return names;
    }

    private static String mainScenarioDefault() {
        try {
            return (String) UseCase.class.getMethod("scenario").getDefaultValue();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("UseCase.scenario() is gone", e);
        }
    }
}
```

## `TestCaseTraceabilityTest.java`

```java
package com.example.app.traceability;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.example.app.traceability.SpecDocuments.assertNoViolations;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for {@code docs/test_cases/}.
 *
 * <p>A journey spans several use cases, and the class name can only carry its
 * own id. The {@code useCases} of {@link TestCase} carries the rest, and this
 * sensor compares it with the {@code [UC-NNN](...)} links in the document's
 * Flow table <em>as a set, in both directions</em> — so a step added to the
 * journey without being automated, or a use case dropped from the document
 * while the annotation still claims it, fails the build.
 *
 * <p>As with use cases, referential integrity is required of every document and
 * coverage only of one whose status asserts it: {@code Automated}.
 */
@Tag("sensor")
class TestCaseTraceabilityTest {

    /** CHANGE ME — the root package of the project under test. */
    private static final String BASE_PACKAGE = "com.example.app";

    private static final Path TEST_CASE_DIR = Path.of("docs", "test_cases");
    private static final Path USE_CASE_DIR = Path.of("docs", "use_cases");

    /** The status that asserts a journey test exists. */
    private static final Set<String> CLAIMS_AUTOMATED = Set.of("Automated");

    private static final Pattern TEST_CASE_ID = Pattern.compile(
            "^\\*\\*ID:\\*\\*[ \t]*(TC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    /** A Flow-table cell: {@code [UC-004](../use_cases/UC-004-find-owners.md)}. */
    private static final Pattern USE_CASE_LINK = Pattern.compile(
            "\\[(UC-\\d{3})]\\((\\.\\./use_cases/[^)]+)\\)");
    /** {@code TC001VisitBookedForKnownPetIT} — a journey always needs a browser. */
    private static final Pattern TEST_CASE_CLASS = Pattern.compile("^TC(\\d{3})\\w*IT$");

    private static List<TestCaseSpec> specifications;
    private static Map<String, TestCaseSpec> byId;
    private static List<JourneyTest> journeys;
    private static Set<String> knownUseCaseIds;

    /** One {@code docs/test_cases/TC-NNN-*.md} file. */
    private record TestCaseSpec(String id, String status, Set<String> useCases,
                                Set<String> linkedPaths, Path file) {

        boolean claimsAutomated() {
            return CLAIMS_AUTOMATED.contains(status);
        }
    }

    /** One {@code @TestCase} annotation on one test class. */
    private record JourneyTest(String testCaseId, Set<String> useCases, String className) {
    }

    @BeforeAll
    static void loadSpecificationsAndJourneys() throws IOException {
        specifications = readSpecifications();
        byId = specifications.stream()
                .collect(toMap(TestCaseSpec::id, spec -> spec, (first, second) -> first));
        journeys = readJourneyTests();
        knownUseCaseIds = readKnownUseCaseIds();
    }

    // --- Guard ---------------------------------------------------------------

    @Test
    void specificationsAreDiscovered() {
        assertFalse(specifications.isEmpty(),
                "No test case specifications found in " + TEST_CASE_DIR.toAbsolutePath()
                        + ". Tests must run with the project root as working directory."
                        + " Delete this sensor if the project deliberately has no test cases.");
    }

    @Test
    void anAutomatedTestCaseImpliesAnAnnotatedJourney() {
        if (specifications.stream().noneMatch(TestCaseSpec::claimsAutomated)) {
            return; // nothing claims automation yet; the coverage checks are vacuous
        }
        assertFalse(journeys.isEmpty(),
                "A test case claims **Status:** Automated but no @TestCase annotation was found"
                        + " on the test classpath under " + BASE_PACKAGE);
    }

    @Test
    void testCaseIdsAreUnique() {
        Set<String> seen = new LinkedHashSet<>();
        assertNoViolations("test case ids claimed by more than one file",
                specifications.stream()
                        .filter(spec -> !seen.add(spec.id()))
                        .map(spec -> spec.id() + " in " + spec.file()));
    }

    // --- Referential integrity -----------------------------------------------

    @Test
    void everyAnnotationPointsAtAKnownTestCase() {
        assertNoViolations("@TestCase annotations naming an unknown test case",
                journeys.stream()
                        .filter(journey -> !byId.containsKey(journey.testCaseId()))
                        .map(journey -> journey.className() + " names " + journey.testCaseId()));
    }

    @Test
    void everyJourneyClassNameEncodesItsId() {
        assertNoViolations("@TestCase classes whose name does not encode their id",
                journeys.stream()
                        .filter(journey -> {
                            Matcher matcher = TEST_CASE_CLASS.matcher(journey.className());
                            return !matcher.matches()
                                    || !journey.testCaseId().equals("TC-" + matcher.group(1));
                        })
                        .map(journey -> journey.className() + " automates "
                                + journey.testCaseId() + ", so it must be named TC"
                                + journey.testCaseId().substring(3) + "<JourneyName>IT"));
    }

    @Test
    void everyLinkedUseCaseExists() {
        assertNoViolations("Flow tables linking a use case that does not exist",
                specifications.stream().flatMap(spec -> spec.useCases().stream()
                        .filter(useCase -> !knownUseCaseIds.contains(useCase))
                        .map(useCase -> spec.id() + " links " + useCase)));
    }

    @Test
    void everyLinkedPathResolves() {
        assertNoViolations("Flow table links pointing at a missing file",
                specifications.stream().flatMap(spec -> spec.linkedPaths().stream()
                        .filter(relative -> !Files.isRegularFile(
                                spec.file().getParent().resolve(relative).normalize()))
                        .map(relative -> spec.id() + " links " + relative)));
    }

    @Test
    void everyAnnotationListsOnlyKnownUseCases() {
        assertNoViolations("@TestCase annotations naming a use case that does not exist",
                journeys.stream().flatMap(journey -> journey.useCases().stream()
                        .filter(useCase -> !knownUseCaseIds.contains(useCase))
                        .map(useCase -> journey.className() + " names " + useCase)));
    }

    /** The one check the class name cannot make: the journey and the document agree. */
    @Test
    void annotationAndFlowTableAgree() {
        assertNoViolations("@TestCase annotations disagreeing with their Flow table",
                journeys.stream()
                        .filter(journey -> byId.containsKey(journey.testCaseId()))
                        .flatMap(journey -> {
                            Set<String> documented = byId.get(journey.testCaseId()).useCases();
                            Stream<String> missing = documented.stream()
                                    .filter(useCase -> !journey.useCases().contains(useCase))
                                    .map(useCase -> journey.className() + " does not name "
                                            + useCase + ", which its Flow table links");
                            Stream<String> extra = journey.useCases().stream()
                                    .filter(useCase -> !documented.contains(useCase))
                                    .map(useCase -> journey.className() + " names " + useCase
                                            + ", which its Flow table does not link");
                            return Stream.concat(missing, extra);
                        }));
    }

    // --- Coverage -------------------------------------------------------------

    @Test
    void everyAutomatedTestCaseHasAJourneyTest() {
        assertNoViolations("test cases claiming automation with no journey test",
                specifications.stream()
                        .filter(TestCaseSpec::claimsAutomated)
                        .filter(spec -> journeys.stream()
                                .noneMatch(journey -> journey.testCaseId().equals(spec.id())))
                        .map(spec -> spec.id() + " (" + spec.status()
                                + ") expects a class annotated @TestCase(id = \"" + spec.id()
                                + "\") named TC" + spec.id().substring(3) + "<JourneyName>IT"));
    }

    // --- Parsing ---------------------------------------------------------------

    private static List<TestCaseSpec> readSpecifications() throws IOException {
        List<TestCaseSpec> specs = new ArrayList<>();
        for (Path file : SpecDocuments.filesIn(TEST_CASE_DIR, "TC-")) {
            String markdown = Files.readString(file);
            Set<String> useCases = new LinkedHashSet<>();
            Set<String> paths = new TreeSet<>();
            Matcher link = USE_CASE_LINK.matcher(markdown);
            while (link.find()) {
                useCases.add(link.group(1));
                paths.add(link.group(2));
            }
            specs.add(new TestCaseSpec(
                    SpecDocuments.require(TEST_CASE_ID, markdown, file, "**ID:**"),
                    SpecDocuments.require(SpecDocuments.STATUS, markdown, file, "**Status:**"),
                    useCases, paths, file));
        }
        return specs;
    }

    private static List<JourneyTest> readJourneyTests() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
        List<JourneyTest> found = new ArrayList<>();
        for (JavaClass type : classes) {
            if (!type.isAnnotatedWith(TestCase.class)) {
                continue;
            }
            TestCase annotation = type.getAnnotationOfType(TestCase.class);
            found.add(new JourneyTest(annotation.id(),
                    Stream.of(annotation.useCases()).collect(toCollection(LinkedHashSet::new)),
                    type.getSimpleName()));
        }
        return found;
    }

    private static Set<String> readKnownUseCaseIds() throws IOException {
        Set<String> ids = new TreeSet<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            Matcher matcher = Pattern
                    .compile("^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE)
                    .matcher(Files.readString(file));
            if (matcher.find()) {
                ids.add(matcher.group(1));
            }
        }
        return ids;
    }
}
```

## `BusinessRuleTraceabilityTest.java`

```java
package com.example.app.traceability;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.example.app.traceability.SpecDocuments.assertNoViolations;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor between {@code docs/business_rules.md} and the use cases
 * that reference a {@code GR-NNN}.
 *
 * <p>A shared rule is stated once and referenced from each use case that
 * applies it, so there are four things that can disagree: the summary table and
 * the rules below it, a rule's {@code **Realized by:**} line and the use cases,
 * a use case's link and the heading it lands on, and the anchor in that link.
 * All four are checked, in both directions.
 *
 * <p>One judgement is built in: <strong>a rule realized by fewer than two use
 * cases is a defect.</strong> A rule only one use case needs belongs in that
 * use case; a single realizer means the rule was promoted too early, or a use
 * case forgot its link. A rule whose heading ends in {@code (Obsolete)} is
 * exempt, because retiring a rule is the one legitimate way to reach zero.
 */
@Tag("sensor")
class BusinessRuleTraceabilityTest {

    private static final Path CATALOGUE = Path.of("docs", "business_rules.md");
    private static final Path USE_CASE_DIR = Path.of("docs", "use_cases");

    private static final Pattern RULE_HEADING = Pattern.compile(
            "^## (GR-\\d{3}):[ \t]*(.+)$", Pattern.MULTILINE);
    private static final Pattern REALIZED_BY = Pattern.compile(
            "^\\*\\*Realized by:\\*\\*[ \t]*(.*)$", Pattern.MULTILINE);
    private static final Pattern REALIZER = Pattern.compile("(UC-\\d{3})[ \t]+(BR-\\d{3})");
    private static final Pattern TABLE_ROW = Pattern.compile(
            "^\\|[ \t]*(GR-\\d{3})[ \t]*\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|[ \t]*$",
            Pattern.MULTILINE);
    private static final Pattern USE_CASE_ID = Pattern.compile(
            "^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    private static final Pattern BUSINESS_RULE_SECTION = Pattern.compile(
            "^### (BR-\\d{3}):[ \t]*(.+)$", Pattern.MULTILINE);
    private static final Pattern REALIZES_LINK = Pattern.compile(
            "Realizes[ \t]+\\[(GR-\\d{3}):[ \t]*([^\\]]+)]"
                    + "\\(\\.\\./business_rules\\.md#([a-z0-9-]+)\\)");

    private static Map<String, Rule> rules;
    private static Map<String, String> tableRows;
    private static List<Realization> realizations;
    private static Map<String, Set<String>> declaredRulesByUseCase;

    /** One {@code ## GR-NNN: Name} section of the catalogue. */
    private record Rule(String id, String name, Set<String> realizedBy) {

        boolean obsolete() {
            return name.endsWith("(Obsolete)");
        }
    }

    /** One {@code Realizes [GR-NNN: Name](...)} link inside one use case's BR. */
    private record Realization(String useCaseId, String businessRuleId, String ruleId,
                               String ruleName, String anchor, Path file) {

        String pair() {
            return useCaseId + " " + businessRuleId;
        }
    }

    @BeforeAll
    static void loadCatalogueAndUseCases() throws IOException {
        rules = readRules();
        tableRows = readTableRows();
        realizations = readRealizations();
        declaredRulesByUseCase = readDeclaredRules();
    }

    // --- Guard ---------------------------------------------------------------

    /**
     * No catalogue is a valid state — until a use case links to one. The checks
     * below are then all vacuous, which is correct rather than blind.
     */
    @Test
    void theCatalogueExistsWhenAUseCaseReferencesIt() {
        if (realizations.isEmpty()) {
            return;
        }
        assertFalse(rules.isEmpty(),
                realizations.size() + " use case business rule(s) reference "
                        + CATALOGUE.toAbsolutePath() + ", which holds no GR-NNN rule."
                        + " Run /business-rules, or remove the links.");
    }

    // --- The catalogue is internally consistent -------------------------------

    @Test
    void theSummaryTableAndTheRulesAgree() {
        Stream<String> missingFromTable = rules.values().stream()
                .filter(rule -> !tableRows.containsKey(rule.id()))
                .map(rule -> rule.id() + " has a section but no row in the summary table");
        Stream<String> missingFromRules = tableRows.keySet().stream()
                .filter(id -> !rules.containsKey(id))
                .map(id -> id + " has a row in the summary table but no section");
        Stream<String> nameMismatch = rules.values().stream()
                .filter(rule -> tableRows.containsKey(rule.id()))
                .filter(rule -> !rule.name().equals(tableRows.get(rule.id())))
                .map(rule -> rule.id() + " is \"" + rule.name() + "\" in its heading but \""
                        + tableRows.get(rule.id()) + "\" in the summary table");
        assertNoViolations("the summary table and the rules below it disagree",
                Stream.of(missingFromTable, missingFromRules, nameMismatch).flatMap(s -> s));
    }

    @Test
    void everyRuleIsRealizedByAtLeastTwoUseCases() {
        assertNoViolations("rules that do not belong in the shared catalogue",
                rules.values().stream()
                        .filter(rule -> !rule.obsolete())
                        .filter(rule -> rule.realizedBy().size() < 2)
                        .map(rule -> rule.id() + " is realized by " + rule.realizedBy().size()
                                + " use case(s) " + rule.realizedBy()
                                + " — a rule only one use case needs belongs in that use case"));
    }

    // --- The catalogue and the use cases agree --------------------------------

    @Test
    void everyRealizesLinkPointsAtAKnownRule() {
        assertNoViolations("use case business rules referencing an unknown shared rule",
                realizations.stream()
                        .filter(realization -> !rules.containsKey(realization.ruleId()))
                        .map(realization -> realization.pair() + " references "
                                + realization.ruleId() + ", which the catalogue does not define"));
    }

    @Test
    void everyRealizesLinkNamesTheRuleCorrectly() {
        assertNoViolations("use case business rules naming a shared rule differently",
                realizations.stream()
                        .filter(realization -> rules.containsKey(realization.ruleId()))
                        .filter(realization -> !rules.get(realization.ruleId()).name()
                                .equals(realization.ruleName()))
                        .map(realization -> realization.pair() + " calls "
                                + realization.ruleId() + " \"" + realization.ruleName()
                                + "\", the catalogue calls it \""
                                + rules.get(realization.ruleId()).name() + "\""));
    }

    /** A renamed heading leaves every link pointing at the top of the page. */
    @Test
    void everyRealizesLinkAnchorResolves() {
        assertNoViolations("Realizes links whose anchor does not resolve",
                realizations.stream()
                        .filter(realization -> rules.containsKey(realization.ruleId()))
                        .filter(realization -> {
                            Rule rule = rules.get(realization.ruleId());
                            return !SpecDocuments.slug(rule.id() + ": " + rule.name())
                                    .equals(realization.anchor());
                        })
                        .map(realization -> realization.pair() + " links #"
                                + realization.anchor() + ", the heading resolves to #"
                                + SpecDocuments.slug(realization.ruleId() + ": "
                                        + rules.get(realization.ruleId()).name())));
    }

    @Test
    void realizedByAndTheUseCasesAgree() {
        Set<String> actual = new TreeSet<>();
        realizations.forEach(realization -> actual.add(
                realization.ruleId() + " <- " + realization.pair()));
        Set<String> declared = new TreeSet<>();
        rules.values().forEach(rule -> rule.realizedBy()
                .forEach(pair -> declared.add(rule.id() + " <- " + pair)));

        Stream<String> notInUseCases = declared.stream()
                .filter(entry -> !actual.contains(entry))
                .map(entry -> entry + " is on a Realized by: line, but that use case"
                        + " business rule has no Realizes link");
        Stream<String> notInCatalogue = actual.stream()
                .filter(entry -> !declared.contains(entry))
                .map(entry -> entry + " has a Realizes link, but is missing from that rule's"
                        + " Realized by: line");
        assertNoViolations("the catalogue and the use cases disagree",
                Stream.concat(notInUseCases, notInCatalogue));
    }

    @Test
    void everyRealizedByPairExistsAsABusinessRule() {
        assertNoViolations("Realized by: entries naming a business rule that does not exist",
                rules.values().stream().flatMap(rule -> rule.realizedBy().stream()
                        .filter(pair -> {
                            String[] parts = pair.split(" ");
                            Set<String> declaredRules =
                                    declaredRulesByUseCase.getOrDefault(parts[0], Set.of());
                            return !declaredRules.contains(parts[1]);
                        })
                        .map(pair -> rule.id() + " claims " + pair
                                + ", which is not a business rule of that use case")));
    }

    // --- Parsing ---------------------------------------------------------------

    private static Map<String, Rule> readRules() throws IOException {
        Map<String, Rule> found = new LinkedHashMap<>();
        if (!Files.isRegularFile(CATALOGUE)) {
            return found;
        }
        String markdown = Files.readString(CATALOGUE);
        Matcher heading = RULE_HEADING.matcher(markdown);
        List<int[]> bounds = new ArrayList<>();
        List<String[]> headings = new ArrayList<>();
        while (heading.find()) {
            bounds.add(new int[] { heading.end(), markdown.length() });
            headings.add(new String[] { heading.group(1), SpecDocuments.normalize(heading.group(2)) });
            if (bounds.size() > 1) {
                bounds.get(bounds.size() - 2)[1] = heading.start();
            }
        }
        for (int i = 0; i < headings.size(); i++) {
            String body = markdown.substring(bounds.get(i)[0], bounds.get(i)[1]);
            Set<String> realizedBy = new LinkedHashSet<>();
            Matcher line = REALIZED_BY.matcher(body);
            if (line.find()) {
                Matcher realizer = REALIZER.matcher(line.group(1));
                while (realizer.find()) {
                    realizedBy.add(realizer.group(1) + " " + realizer.group(2));
                }
            }
            found.put(headings.get(i)[0],
                    new Rule(headings.get(i)[0], headings.get(i)[1], realizedBy));
        }
        return found;
    }

    private static Map<String, String> readTableRows() throws IOException {
        Map<String, String> found = new LinkedHashMap<>();
        if (!Files.isRegularFile(CATALOGUE)) {
            return found;
        }
        Matcher row = TABLE_ROW.matcher(Files.readString(CATALOGUE));
        while (row.find()) {
            found.put(row.group(1), SpecDocuments.normalize(row.group(2)));
        }
        return found;
    }

    private static List<Realization> readRealizations() throws IOException {
        List<Realization> found = new ArrayList<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            Matcher id = USE_CASE_ID.matcher(markdown);
            if (!id.find()) {
                continue;
            }
            String useCaseId = id.group(1);
            Matcher section = BUSINESS_RULE_SECTION.matcher(markdown);
            List<int[]> bounds = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            while (section.find()) {
                bounds.add(new int[] { section.end(), markdown.length() });
                ids.add(section.group(1));
                if (bounds.size() > 1) {
                    bounds.get(bounds.size() - 2)[1] = section.start();
                }
            }
            for (int i = 0; i < ids.size(); i++) {
                Matcher link = REALIZES_LINK
                        .matcher(markdown.substring(bounds.get(i)[0], bounds.get(i)[1]));
                if (link.find()) {
                    found.add(new Realization(useCaseId, ids.get(i), link.group(1),
                            SpecDocuments.normalize(link.group(2)), link.group(3), file));
                }
            }
        }
        return found;
    }

    private static Map<String, Set<String>> readDeclaredRules() throws IOException {
        Map<String, Set<String>> found = new LinkedHashMap<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            Matcher id = USE_CASE_ID.matcher(markdown);
            if (!id.find()) {
                continue;
            }
            Set<String> ids = new TreeSet<>();
            Matcher section = BUSINESS_RULE_SECTION.matcher(markdown);
            while (section.find()) {
                ids.add(section.group(1));
            }
            found.put(id.group(1), ids);
        }
        return found;
    }
}
```

---

## Notes on the parsing

- **`ONLY_INCLUDE_TESTS`** restricts the ArchUnit import to `target/test-classes`,
  so the sensors never see production classes and stay fast.
- **Both document parsers find section bodies by heading bounds**, not by a
  blank-line heuristic: a heading's body runs to the next heading of the same
  level, or to the end of the file. That is why a `### BR-014:` heading may
  carry prose, a list or a link without upsetting the parser.
- **`(Obsolete)`** on a `GR` heading is the only exemption in the whole sensor
  set, and it exists so a retired rule can reach zero realizers honestly.
- **`REALIZER` requires whitespace between the two ids** (`UC-007 BR-014`), so
  prose that merely mentions both in a sentence is not mistaken for a claim.
