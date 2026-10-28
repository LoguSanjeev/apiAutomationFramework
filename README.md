# API Automation Framework

A Java API testing framework using Rest Assured, TestNG, ExtentReports, Log4j 2,
Maven, and plain POJOs. Classes use normal methods, getters/setters, and simple
collections so the framework is easy for a QA team to extend.

## Prerequisites and first run

- JDK 17 or newer
- Maven 3.9 or newer
- Internet access for Maven's first dependency download and JSONPlaceholder tests

Check `mvn -version` to confirm Maven uses the intended JDK. If your shell uses an
older Java version, set `JAVA_HOME` to a JDK 17+ installation and add its `bin`
directory to `PATH`. Set the same JDK in your IDE.

From the project root:

```bash
# Default: development data, smoke runner (2 public API tests)
mvn clean test

# Regression runner (9 public API + 3 file utility tests)
mvn clean test -Denv=development -DsuiteXmlFile=config/testng-regression.xml

# Smoke tests using development data
mvn clean test -Denv=development

# Regression tests using staging data
mvn clean test -Denv=staging -DsuiteXmlFile=config/testng-regression.xml

# Smoke tests using production data
mvn clean test -Denv=production

# Public JSONPlaceholder API tests only (9 tests)
mvn clean test -Denv=development -DsuiteXmlFile=config/testng-jsonplaceholder.xml

# Local utility checks only (3 tests, no external API calls)
mvn clean test -DsuiteXmlFile=config/testng-local.xml
```

`JsonPlaceholderPostTest` uses the environment's `base.url`. All three environment
files currently point to `https://jsonplaceholder.typicode.com`, since this demo
service provides a single public endpoint. The environment-specific test data
still comes from separate YAML sections.

## JSONPlaceholder API coverage

The public suite tests the `/posts` resource using the
[JSONPlaceholder API guide](https://jsonplaceholder.typicode.com/guide/).

| Test | Main validations |
| --- | --- |
| GET an existing post | 200 OK, JSON content type, schema, ID, user ID, non-blank title/body |
| GET posts for a user | 200 OK, array schema, non-empty list, every post belongs to the requested user |
| POST a dynamic post | 201 Created, schema, echoed request fields, generated ID, follow-up GET |
| PUT a replacement | 200 OK, schema, all replacement fields, stable ID, follow-up GET |
| PATCH the title | 200 OK, schema, new title, unchanged body/user ID, follow-up GET |
| DELETE a post | 200 OK, empty JSON object and schema, follow-up GET |
| GET a missing post | 404 Not Found, empty JSON object and schema |
| GET an unknown route | 404 Not Found, empty JSON object and schema |
| Filter using a missing user | 200 OK, array schema, empty list |

Every POST and PUT builds a fresh title and body using the environment's YAML
prefixes and a UUID. PATCH sends only a newly generated title. Request and response
POJOs are in `pojo`, payload builders in `payloads`, and chainable response assertions
in `tests.assets.ResponseAssert`. Each validation and HTTP action is logged to
Log4j and ExtentReports. These tests run independently without TestNG dependencies.

**Writes are simulated by JSONPlaceholder.** Each mutation test makes a follow-up
GET and explicitly checks that documented behavior:

- After POST, GET uses the ID returned by POST and expects 404 with `{}`.
- Before PUT, PATCH, and DELETE, GET captures the original post. After the write,
  another GET validates that every original field is still present and unchanged.

These checks validate the public API's behavior; they do not demonstrate saved
changes. Test names and `EXPECTED LIMITATION` log entries state this explicitly.
JSONPlaceholder is public and has no authentication requirement, so 401/403 tests
are not applicable. Its responses have no application `message` field; the tests
validate HTTP response messages (`OK`, `Created`, `Not Found`) and actual body
fields instead. POST correctly expects 201 rather than 200.

## Writing readable tests

Keep endpoint paths in `endpoints/Endpoints.java` and schema resource paths in
`schemas/Schemas.java`. Use endpoint constants in requests and Rest Assured's
`pathParam` for IDs. Base URLs remain in environment properties.

Each request uses `given()` for its configuration and data, `when()` for the HTTP
method, and `then()` to extract the response for the logged `ResponseAssert`
validations. Add `import static io.restassured.RestAssured.given;` to the test.
For example:

```java
TestLogger.info("STEP: Send GET request for an existing post");
Response response =
        given()
            .spec(apiClient.request())
            .pathParam("id", testData.getString("posts.existingId"))
        .when()
            .get(Endpoints.POST_BY_ID)
        .then()
            .extract().response();

TestLogger.info("STEP: Validate status, JSON header, response time, schema, and body");
ResponseAssert.assertThat(response)
        .returns_200_OK()
        .hasHeaderApplicationJson()
        .isWithinAcceptedTimeLimit(responseTimeLimit)
        .hasValidSchema(Schemas.POST_RESPONSE)
        .hasNonBlankJsonValue("title")
        .hasNonBlankJsonValue("body");
```

`ResponseAssert` uses simple methods that perform a check and return `this` so
another check can follow. Each check logs its start and result through the common
logging helpers. Test start/end, final status, and failures are handled by the
listener automatically. Tests use local response variables and remain independent.

Use `.returns_201_Created()` for creation and `.returns_404_NotFound()` for missing
resources. For other APIs, use `.hasStatusCode(403)` and
`.hasStatusMessage("Forbidden")`. These generic methods do not enable 403 tests
against JSONPlaceholder, which has no authentication requirement.

`response.time.limit.ms=10000` sets the maximum response duration in each
environment file. `BaseTest` exposes it as `responseTimeLimit`. Override it at
runtime with `-Dresponse.time.limit.ms=20000` when needed. HTTP connection/read
timeouts are separate settings. The validation measures Rest Assured's response
time in milliseconds and checks it is within the configured limit.

## Project layout

```text
apiAutomationFramework/
├── pom.xml
├── config/
│   ├── testng-smoke.xml
│   ├── testng-regression.xml
│   ├── testng-jsonplaceholder.xml
│   └── testng-local.xml
└── src/test/
    ├── java/com/qa/api/
    │   ├── base/
    │   │   └── BaseTest.java
    │   ├── client/
    │   │   ├── ApiClient.java
    │   │   └── ApiLoggingFilter.java
    │   ├── endpoints/
    │   │   └── Endpoints.java
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
    │   │   └── PostPayloads.java
    │   ├── pojo/
    │   │   ├── PostRequest.java
    │   │   ├── PostPatchRequest.java
    │   │   └── PostResponse.java
    │   ├── reporting/
    │   │   └── ReportManager.java
    │   ├── schemas/
    │   │   └── Schemas.java
    │   └── tests/
    │       ├── CommonUtilsTest.java
    │       ├── JsonPlaceholderPostTest.java
    │       └── assets/
    │           ├── ApiAssertions.java
    │           └── ResponseAssert.java
    └── resources/
        ├── environments/
        │   ├── dev.properties
        │   ├── stag.properties
        │   └── prod.properties
        ├── testdata/
        │   └── test-data.yml
        ├── schemas/
        │   ├── post-response.schema.json
        │   ├── post-list.schema.json
        │   └── empty-object.schema.json
        └── log4j2.xml
```

All Java files live under `src/test/java` because this project is a test suite.
Schemas live in their own resource folder so they can be loaded from the classpath.
`tests.assets` contains the reusable assertions requested for the tests.

## Configuration and test data

Environment properties hold settings such as the base URL and HTTP timeouts:

```properties
base.url=https://jsonplaceholder.typicode.com
connection.timeout.ms=10000
read.timeout.ms=10000
response.time.limit.ms=10000
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
  posts:
    existingId: "1"
    missingId: "999999"
    userId: "1"
    titlePrefix: "Development API test"
    bodyPrefix: "Development dynamic post body"
```

Read values in a test with:

```java
String baseUrl = config.get("base.url");
String titlePrefix = testData.getString("posts.titlePrefix");
```

JVM properties override environment file values, for example:

```bash
mvn test -Denv=staging -Dbase.url=https://your-staging-api.example.com -Dread.timeout.ms=20000
```

To add an environment, add its property file and matching YAML section, then add
the full runtime name mapping in `EnvironmentConfig`. Keep credentials out of
committed files.

## Add another API test

1. Replace the appropriate environment's `base.url`.
2. Add its paths to `endpoints/Endpoints.java`.
3. Add request/response POJOs under `pojo` when needed.
4. Add payload-building methods under `payloads`, reading values from YAML.
5. Add a response schema under `src/test/resources/schemas` and its constant to `Schemas.java`.
6. Create a test in `tests` that extends `BaseTest` and validates with `ResponseAssert`.
7. Add its fully qualified class name to the runner's `<classes>` section.

This is an example to adapt to your API contract, not an enabled test:

```java
package com.qa.api.tests;

import static io.restassured.RestAssured.given;

import com.qa.api.base.BaseTest;
import com.qa.api.commonUtils.TestLogger;
import com.qa.api.endpoints.Endpoints;
import com.qa.api.payloads.PostPayloads;
import com.qa.api.pojo.PostRequest;
import com.qa.api.schemas.Schemas;
import com.qa.api.tests.assets.ResponseAssert;
import io.restassured.response.Response;
import org.testng.annotations.Test;

public class PostApiExampleTest extends BaseTest {
    @Test(groups = {"smoke", "regression"})
    public void createPost() {
        PostRequest payload = PostPayloads.createPost(testData);
        TestLogger.info("STEP: Send the create-post request");
        Response response =
                given()
                    .spec(apiClient.request())
                    .body(payload)
                .when()
                    .post(Endpoints.POSTS)
                .then()
                    .extract().response();

        TestLogger.info("STEP: Validate the create-post response");
        ResponseAssert.assertThat(response)
                .returns_201_Created()
                .hasHeaderApplicationJson()
                .isWithinAcceptedTimeLimit(responseTimeLimit)
                .hasValidSchema(Schemas.POST_RESPONSE)
                .hasJsonValue("title", payload.getTitle())
                .hasJsonValue("body", payload.getBody())
                .hasJsonValue("userId", payload.getUserId());
    }
}
```

For headers, authentication, query parameters, or path parameters, use the same
logged request specification:

```java
Response response =
        given()
            .spec(apiClient.request())
            .header("Authorization", "Bearer " + System.getProperty("api.token"))
            .queryParam("page", 1)
            .pathParam("id", testData.getString("posts.existingId"))
        .when()
            .get(Endpoints.POST_BY_ID)
        .then()
            .extract().response();
```

Shared authentication or prerequisites can be added to `BaseTest.beforeClass()`
and `ApiClient.request()` once the API requirements are known. A new request is
created for every call. The client supplies GET, POST, PUT, PATCH, and DELETE helpers.

## Logging and reporting

Generated output:

| Output | Location |
| --- | --- |
| Extent HTML reports | `test-output/ExtentReport-yyyy-MM-dd_HH-mm-ss-SSS.html` |
| Rolling Log4j log | `target/logs/automation.log` |
| TestNG/Maven results | `target/surefire-reports/` |
| File utility demo output | `target/utility-test-output/` |

Open the latest timestamped Extent HTML report in a browser after a run. The
`test-output` folder keeps the newest 30 Extent reports and automatically deletes
the oldest report when the limit is exceeded. `mvn clean` does not remove this
report history.

`TestListener` records test starts, pass/fail/skip results, duration, failure stack
traces, and setup/teardown failures. `ApiLoggingFilter` records every HTTP request
and its response status and duration. Utilities log file operations and data keys.
`ResponseAssert` and `ApiAssertions` log validation starts and successful results; failures propagate
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
