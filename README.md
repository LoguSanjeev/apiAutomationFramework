# API Automation Framework

A Java API testing skeleton using Rest Assured, TestNG, ExtentReports, Log4j 2,
Maven, and plain POJOs. Classes use normal methods, getters/setters, and simple
collections so the framework is easy for a QA team to extend.

## Prerequisites and first run

- JDK 17 or newer
- Maven 3.9 or newer
- Internet access for Maven's first dependency download

Check `mvn -version` to confirm Maven uses the intended JDK. If your shell uses an
older Java version, set `JAVA_HOME` to a JDK 17+ installation and add its `bin`
directory to `PATH`. Set the same JDK in your IDE.

From the project root:

```bash
# Default: development data, smoke runner (2 tests)
mvn clean test

# Regression runner (4 dummy API tests + 3 file utility tests)
mvn clean test -Denv=development -DsuiteXmlFile=config/testng-regression.xml

# Smoke tests using development data
mvn clean test -Denv=development

# Regression tests using staging data
mvn clean test -Denv=staging -DsuiteXmlFile=config/testng-regression.xml

# Smoke tests using production data
mvn clean test -Denv=production
```

The dummy tests start a small local HTTP server on an available loopback port,
send actual Rest Assured requests, and stop the server afterward. They have no
dependency on an external API. The sample server returns fixed dummy users and
does not save data. Tests do not depend on execution order.

**Only `DummyApiTest` replaces the configured URL with the local server URL.**
The environment URLs are placeholders until you share your real APIs. Changing
`base.url` will affect future real API test classes, not the dummy class.

## Project layout

```text
apiAutomationFramework/
├── pom.xml
├── config/
│   ├── testng-smoke.xml
│   └── testng-regression.xml
└── src/test/
    ├── java/com/qa/api/
    │   ├── base/
    │   │   └── BaseTest.java
    │   ├── client/
    │   │   ├── ApiClient.java
    │   │   └── ApiLoggingFilter.java
    │   ├── commonUtils/
    │   │   ├── EnvironmentConfig.java
    │   │   ├── PropertiesUtils.java
    │   │   ├── FileUtils.java
    │   │   ├── CsvUtils.java
    │   │   ├── YamlUtils.java
    │   │   ├── TestData.java
    │   │   └── TestLogger.java
    │   ├── listeners/
    │   │   └── TestListener.java
    │   ├── payloads/
    │   │   └── UserPayloads.java
    │   ├── pojo/
    │   │   ├── UserRequest.java
    │   │   └── UserResponse.java
    │   ├── reporting/
    │   │   └── ReportManager.java
    │   └── tests/
    │       ├── DummyApiTest.java
    │       ├── CommonUtilsTest.java
    │       ├── assets/
    │       │   └── ApiAssertions.java
    │       └── support/
    │           └── DummyApiServer.java
    └── resources/
        ├── environments/
        │   ├── dev.properties
        │   ├── stag.properties
        │   └── prod.properties
        ├── testdata/
        │   └── test-data.yml
        ├── schemas/
        │   └── user-response.schema.json
        └── log4j2.xml
```

All Java files live under `src/test/java` because this project is a test suite.
Schemas live in their own resource folder so they can be loaded from the classpath.
`tests.assets` contains the reusable assertions requested for the tests.

## Configuration and test data

Environment properties hold settings such as the base URL and HTTP timeouts:

```properties
base.url=https://dev.example.invalid
connection.timeout.ms=10000
read.timeout.ms=10000
```

Use the full environment name at runtime. The framework maps it to the property
file and YAML section:

| Runtime value | Property file | YAML section |
| --- | --- | --- |
| `-Denv=development` | `environments/dev.properties` | `dev` |
| `-Denv=staging` | `environments/stag.properties` | `stag` |
| `-Denv=production` | `environments/prod.properties` | `prod` |

The default is `development` when `-Denv` is omitted. Invalid environments or
missing required keys produce an error instead of silently falling back to other
data. Short names such as `dev`, `stag`, and `prod` are for files and YAML sections;
pass the full names shown above when running tests.

```yaml
dev:
  createUser:
    name: "Development User"
    job: "QA Engineer"
  existingUser:
    id: "1"
```

Read values in a test with:

```java
String baseUrl = config.get("base.url");
String name = testData.getString("createUser.name");
```

JVM properties override environment file values, for example:

```bash
mvn test -Denv=staging -Dbase.url=https://your-staging-api.example.com -Dread.timeout.ms=20000
```

To add an environment, add its property file and matching YAML section, then add
the full runtime name mapping in `EnvironmentConfig`. Keep credentials out of
committed files.

## Add your first real API test

1. Replace the appropriate environment's `base.url`.
2. Add request/response POJOs under `pojo` when needed.
3. Add payload-building methods under `payloads`, reading values from YAML.
4. Add a response schema under `src/test/resources/schemas`.
5. Create a test in `tests` that extends `BaseTest`.
6. Add its fully qualified class name to the runner's `<classes>` section.

This is an example to adapt to your API contract, not an enabled test:

```java
package com.qa.api.tests;

import com.qa.api.base.BaseTest;
import com.qa.api.payloads.UserPayloads;
import com.qa.api.pojo.UserRequest;
import com.qa.api.tests.assets.ApiAssertions;
import io.restassured.response.Response;
import org.testng.annotations.Test;

public class UserApiTest extends BaseTest {
    @Test(groups = {"smoke", "regression"})
    public void createUser() {
        UserRequest payload = UserPayloads.createUser(testData);
        Response response = apiClient.post("/users", payload);

        ApiAssertions.statusCode(response, 201);
        ApiAssertions.matchesSchema(response, "schemas/user-response.schema.json");
        ApiAssertions.jsonValue(response, "name", payload.getName());
    }
}
```

For headers, authentication, query parameters, or path parameters, use the same
logged request specification:

```java
Response response = apiClient.request()
        .header("Authorization", "Bearer " + System.getProperty("api.token"))
        .queryParam("page", 1)
        .pathParam("id", testData.getString("existingUser.id"))
        .get("/users/{id}");
```

Shared authentication or prerequisites can be added to `BaseTest.beforeClass()`
and `ApiClient.request()` once the API requirements are known. A new request is
created for every call. The client supplies GET, POST, PUT, PATCH, and DELETE helpers.

## Logging and reporting

Generated output:

| Output | Location |
| --- | --- |
| Extent HTML report | `target/reports/ExtentReport.html` |
| Rolling Log4j log | `target/logs/automation.log` |
| TestNG/Maven results | `target/surefire-reports/` |
| File utility demo output | `target/utility-test-output/` |

Open the Extent HTML report in a browser after a run. Each run replaces that report;
`mvn clean` deletes all previous output under `target`.

`TestListener` records test starts, pass/fail/skip results, duration, failure stack
traces, and setup/teardown failures. `ApiLoggingFilter` records every HTTP request
and its response status and duration. Utilities log file operations and data keys.
`ApiAssertions` logs validation starts and successful results; failures propagate
to TestNG and the listener records them in the report and log.

Use `TestLogger.info("Describe the action")` for additional steps so they appear
in both outputs. Setup/teardown steps outside a test appear in Log4j; configuration
failures also receive a separate failed entry in ExtentReports. Request headers,
query values, and bodies are not explicitly logged by the HTTP filter.

The runners execute **sequentially**. Keep `parallel="none"`; report tracking is
deliberately simple and supports one active test at a time. Extend `BaseTest` to
inherit the listener, including when running a test directly from your IDE.

## Common utility examples

```java
Path file = Path.of("target", "temporary.properties");
PropertiesUtils.writeProperty(file, "created.user.id", "123");
String id = PropertiesUtils.readFile(file).getProperty("created.user.id");

Path text = Path.of("target", "notes.txt");
FileUtils.writeText(text, "Test started\n");
FileUtils.appendText(text, "Test ended\n");
String contents = FileUtils.readText(text);

Path csv = Path.of("target", "users.csv");
List<List<String>> rows = Arrays.asList(
        Arrays.asList("name", "job"),
        Arrays.asList("QA User", "Automation Engineer"));
CsvUtils.write(csv, rows);
List<List<String>> savedRows = CsvUtils.read(csv);
```

Add the standard `java.nio.file` and `java.util` imports for these examples.
File helpers create parent folders when writing. Property writes preserve other
keys, but rewrite comments/formatting. Text and CSV writes replace existing
content; text append adds exactly the supplied text. CSV headers are treated as
the first row. Apache Commons CSV handles embedded commas, quotes, and newlines.

## Library documentation

- [Rest Assured usage](https://github.com/rest-assured/rest-assured/wiki/Usage)
- [TestNG](https://testng.org/)
- [ExtentReports for Java](https://extentreports.com/docs/versions/5/java/index.html)
- [Log4j 2](https://logging.apache.org/log4j/2.x/manual/)
