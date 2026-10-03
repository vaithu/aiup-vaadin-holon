# Build Wiring

What the sensors need from `pom.xml`. Three independent pieces — apply only the
ones the project is missing.

## 1. ArchUnit (test scope)

The sensors read `@UseCase` / `@TestCase` off the compiled test classes with
ArchUnit's `ClassFileImporter`. `/architecture-rules` needs the same dependency
for its `@ArchTest` rules, so declaring it once covers both.

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.4.1</version>
    <scope>test</scope>
</dependency>
```

Check the current version with the `maven-central` or `context7` MCP server
before pinning; do not copy a version into a project without confirming it.

`archunit-junit5` brings `archunit` itself transitively, so one dependency is
enough. **Keep it `test`-scoped** — ArchUnit on the runtime classpath is a
finding in its own right.

## 2. The `sensor` tag

All six sensor classes carry `@Tag("sensor")` (or `@ArchTag("sensor")` for the
ArchUnit ones), which is what makes the group runnable on its own:

```sh
mvn -q test -Dgroups=sensor
```

Surefire supports `-Dgroups` out of the box; **nothing has to be configured**
for that command to work. What does have to be true is that the sensors are not
excluded by an existing `<includes>`/`<excludes>` block. If the project narrows
Surefire, make sure `**/*Test.java` still matches the sensor classes.

Seconds, no Docker, no Spring context — that is the point. Do not let a sensor
acquire a `@SpringBootTest`, a Testcontainer, or a database fixture; the moment
it needs infrastructure, the guard that asks for it before every turn becomes
too expensive to obey, and will be skipped.

To make the group's cost visible in CI, run it as its own step before the
expensive one:

```yaml
- name: Sensors
  run: mvn -q -B test -Dgroups=sensor
- name: Build and verify
  run: mvn -B -Pquality verify
```

## 3. Failsafe for `*IT`

The naming contract (`*Test` → Surefire → `test`; `*IT` → Failsafe → `verify`)
only holds if Failsafe is actually bound. Without it, every `*IT` class is dead
code that reports as passing by never running — the quietest failure a test
suite has.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Both goals are required: `integration-test` runs the classes, `verify` is what
fails the build on their results. Declaring only the first produces a green
build over failing tests.

Default includes (`**/IT*.java`, `**/*IT.java`, `**/*ITCase.java`) already match
`UC004FindOwnersByLastNameIT` and `TC001VisitBookedForKnownPetIT`, so no
`<includes>` block is needed.

Run a single journey:

```sh
mvn verify -Dit.test=TC001VisitBookedForKnownPetIT
```

## 4. Coverage across both layers (optional but recommended)

Once journeys run in `verify`, a line reached only by a `TC…IT` is invisible to
a report generated from the `test` phase alone — which pushes people to write a
redundant browserless test just to colour a line green.

Give JaCoCo one destination file per layer and merge them. This slots into the
existing `quality` profile from
[`rules/quality-gate.md`](../../../rules/quality-gate.md); keep its JaCoCo
declaration and add the second execution plus the merge.

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>prepare-unit</id>
            <goals><goal>prepare-agent</goal></goals>
            <configuration>
                <destFile>${project.build.directory}/jacoco-unit.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>prepare-it</id>
            <goals><goal>prepare-agent-integration</goal></goals>
            <configuration>
                <destFile>${project.build.directory}/jacoco-it.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>merge</id>
            <phase>verify</phase>
            <goals><goal>merge</goal></goals>
            <configuration>
                <fileSets>
                    <fileSet>
                        <directory>${project.build.directory}</directory>
                        <includes><include>jacoco-*.exec</include></includes>
                    </fileSet>
                </fileSets>
                <destFile>${project.build.directory}/jacoco-merged.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Two consequences worth stating in the project's own docs:

- **Only `mvn verify` produces the merged report.** A coverage number from
  `mvn test` understates the truth and must not be used to judge a gap.
- **Don't add a browserless test purely to cover a line a journey already
  exercises.** With the reports merged it is already covered, and the extra
  test only adds something else to keep in step with the specification.

## 5. Exclude the traceability package from coverage thresholds

The sensors are test-only classes; they neither need nor can receive coverage,
and ArchUnit's own classes skew a duplication report. If the `quality` profile
enforces a threshold, exclude them rather than lowering it:

```xml
<excludes>
    <exclude>**/traceability/**</exclude>
</excludes>
```

## Verification

After wiring, all three must hold:

```sh
mvn -q test -Dgroups=sensor     # runs 5 sensor classes, seconds, no Docker
mvn -q test                     # the sensors run here too, with everything else
mvn -q verify -DskipTests=false # *IT classes actually execute
```

If the first command reports **zero** tests run, the tag is not reaching
Surefire — check for a narrowing `<includes>` block before changing the
sensors.
