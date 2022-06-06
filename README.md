# Artemis

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.devocative.artemis/artemis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.devocative.artemis/artemis)

![Artemis Structure](/doc/logo.png)

- [Introduction](#introduction)
- [An Example](#an-example)
- [Execution](#execution)
  - [JUnit Integration](#junit-integration)
  - [Maven Integration](#maven-integration)
  - [Output](#output)
- [Description of the Files](#description-of-the-files)
- [Parallel Execution and Performance Test](#parallel-execution-and-performance-test)
- [Start Developing](#start-developing)

## Introduction

Artemis is an easy and simple `REST API`, `Integration`, and `Performance` test library. You can use your most powerful
tool to apply Artemis, your IDE. You just need to develop an XML file, and a Groovy file to test your REST APIs. The XML
file describes the structure flow of your scenarios by defining all HTTP requests and then asserting the status of
response and its body. The Groovy file contains lots of functions, called by XML file, to present all the behavioral
parts during your test, such as creating complex dynamic data, asserting the response body with all `Assertions`
libraries, and processing the data necessary to the test scenarios. From version 1.3, a cookie store is enabled for
Artemis, and you can assert cookie presence in `<assertRs cookies=''/>`.

You can integrate Artemis in two ways into your project:

1. Call its API in a JUnit method to integrate it with your other tests
2. Add its maven plugin to your pom and then execute its goal in a more manual way

## An Example

**Note:** You can see the complete example at [OnFood](https://github.com/mbizhani/OnFood).

The following is an `artemis.xml` file:

```xml
<!DOCTYPE artemis PUBLIC "-//Devocative.Org//Artemis 1.0//EN"
    "https://devocative.org/dtd/artemis-1.0.dtd">

<artemis>
  <vars>
    <var name="cell" value="09${_.generate(9, '0'..'9')}"/>
    <var name="firstName" value="${_.generate(4, 'a'..'z')}"/>
    <var name="lastName" value="${_.generate(4, 'a'..'z')}"/>
    <!-- "password" is generated in before() -->
  </vars>

  <scenario name="RegisterRestaurateur">
    <get url="/restaurateurs/registrations/${cell}">

      <!-- `body` can be `empty`, `text` or `json` (default value) -->
      <assertRs status="200" body="empty"/>
    </get>

    <get url="/j4d/registrations/${cell}">
      <assertRs status="200" properties="code"/>
    </get>

    <post url="/restaurateurs/registrations" id="register">

      <!-- `_prev.rs` refers to the response body of previous request -->
      <body><![CDATA[
{
    "firstName": "${firstName}",
    "lastName": "${lastName}",
    "cell": "${cell}",
    "code": "${_prev.rs.code}",
    "password": "${password}"
}
            ]]></body>

      <!-- 
        Due to `call="true"`, `assertRs_register(Context, Map)` is called in Groovy file.
        These assert functions must have syntax of `assertRs_ID(Context, Map | List)`, and `ID`
        is the value of XML's `id` attribute.  
      -->
      <assertRs status="200" properties="userId,token" call="true"/>
    </post>

    <get url="/restaurateurs/${register.rs.userId}">
      <headers>
        <header name="Authorization" value="${register.rs.token}"/>
      </headers>

      <assertRs status="200" properties="id,firstName,lastName,cell,createdBy,createdDate,version"/>
    </get>
  </scenario>
</artemis>
```

and here is the `artemis.groovy` file:

```groovy
import org.devocative.artemis.Context
import org.junit.jupiter.api.Assertions

// Called at the beginning, `before` sending any request
def before(Context ctx) {
  def password = generate(5, 'a'..'z')
  def encPass = Base64.getEncoder().withoutPadding().encodeToString(password.getBytes())
  ctx.addVar("password", encPass)

  // `Artemis` is a utility class with helper functions such as log  
  Artemis.log("Password: main=${password} enc=${encPass}")
}

def generate(int n, List<String>... alphaSet) {
  def list = alphaSet.flatten()
  new Random().with {
    (1..n).collect { list[nextInt(list.size())] }.join()
  }
}

def assertRs_register(Context ctx, Map rsBody) {
  // `ctx.vars` refers to all defined variables until now  
  Assertions.assertNotNull(ctx.vars.cell)
  Assertions.assertNotNull(ctx.vars.firstName)
  Assertions.assertNotNull(ctx.vars.lastName)

  // By default, `org.junit.jupiter:junit-jupiter-api` is added to Artemis dependency, to 
  // call all `Assertions` methods. You can use any assertions library here.   
  Assertions.assertNotNull(rsBody.userId)
  Assertions.assertNotNull(rsBody.token)
}
```

By default, these two files should be in your `src/test/resources` directory of your project or project's module.

## Execution

Some of Artemis parameters are passed before its execution. The most important one is `baseUrl`, which is prepended to
the `url` of each request in the XML file. These parameters are passed to `Config` object (`name` is just passed as
constructor parameter) or inside `<configuration>` in the maven.

| Parameter    | Default Value           | Description                                                                              |
|--------------|-------------------------|------------------------------------------------------------------------------------------|
| `name`       | `artemis`               | looking for `<name>.xml` and `<name>.groovy` files for execution                         |
| `xmlName`    |                         | in case of different name for XML and groovy files                                       |
| `groovyName` |                         | in case of different name for XML and groovy files                                       |
| `baseUrl`    | `http://localhost:8080` | prepend it to the `url` of each request in the XML file                                  |
| `devMode`    | `false`                 | store a memory object as the state of the test for incremental development of test files |
| `baseDir`    | `src/test/resources`    | looking for the XML and Groovy files in this directory                                   |
| `parallel`   | `1`                     | number of **parallel** executions of the entire XML file in a thread                     |
| `loop`       | `1`                     | number of **sequential** executions of the entire XML file in a thread                   |
| `vars`       |                         | pass variables for scenarios from outside                                                |
| `proxy`      |                         | pass requests through proxy server, format `socks://HOST:PORT` or `http://HOST:PORT`     |

### JUnit Integration

First add the following dependency

```xml
<dependency>
  <groupId>org.devocative.artemis</groupId>
  <artifactId>artemis-core</artifactId>
  <version>1.5</version>
  <scope>test</scope>
</dependency>
```

and then call the Artemis API such as the following example in a Spring Boot application:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OnFoodApplicationTests {

  @LocalServerPort
  private int port;

  @Test
  void contextLoads() {
    ArtemisExecutor.run(new Config()
      .setParallel(5)
      .setBaseUrl(String.format("http://localhost:%s/api", port))
    );
  }
}
```

### Maven Integration

Add the following plugin to your `pom.xml` file. Then you can execute the `artemis:run` goal.

```xml
<plugin>
  <groupId>org.devocative.artemis</groupId>
  <artifactId>artemis-maven-plugin</artifactId>
  <version>1.5</version>
  <configuration>
    <baseUrl>http://localhost:8080/api</baseUrl>
    <devMode>true</devMode>
  </configuration>
</plugin>
```

You can also execute following command in the root of your project or module without adding the above plugin:

```shell
mvn org.devocative.artemis:artemis-maven-plugin:1.5:run -DbaseUrl=http://localhost:8080/api -DdevMode=true
```

### Output

Artemis uses the most common way to output the result: log files. During execution, Artemis creates a `logs` directory
and flush all log files such as `artemis.log` in this directory. The log files will be rotated based on the time and
size. At the end of successful execution, a statistical summary is illustrated.

```text
2021-09-28 10:04:53,917 INFO  - *---------*---------*
2021-09-28 10:04:53,930 INFO  - |   A R T E M I S   |
2021-09-28 10:04:53,931 INFO  - *---------*---------*
2021-09-28 10:04:54,028 INFO  - [Groovy] - Password: main=zhkcn enc=emhrY24
2021-09-28 10:04:54,064 INFO  - Global Var: name=[cell] value=[09735929444]
2021-09-28 10:04:54,075 INFO  - Global Var: name=[firstName] value=[zevm]
2021-09-28 10:04:54,085 INFO  - Global Var: name=[lastName] value=[ttgl]
2021-09-28 10:04:54,086 INFO  - =============== [RegisterRestaurateur] ===============
2021-09-28 10:04:54,119 INFO  - --------------- [step #1] ---------------
2021-09-28 10:04:54,135 INFO  - RQ: GET - http://localhost:8080/api/restaurateurs/registrations/09735929444
2021-09-28 10:04:54,170 INFO  - RS: GET (200) - /api/restaurateurs/registrations/09735929444 [34 ms]
  ContentType: null
  
2021-09-28 10:04:54,183 INFO  - --------------- [step #2] ---------------
2021-09-28 10:04:54,192 INFO  - RQ: GET - http://localhost:8080/api/j4d/registrations/09735929444
2021-09-28 10:04:54,199 INFO  - RS: GET (200) - /api/j4d/registrations/09735929444 [6 ms]
  ContentType: application/json
  {"code":"3867"}
2021-09-28 10:04:54,232 INFO  - RS Properties = [code]
2021-09-28 10:04:54,236 INFO  - --------------- [register] ---------------
2021-09-28 10:04:54,274 INFO  - RQ: POST - http://localhost:8080/api/restaurateurs/registrations
{
    "firstName": "zevm",
    "lastName": "ttgl",
    "cell": "09735929444",
    "code": "3867",
    "password": "emhrY24"
}
2021-09-28 10:04:54,370 INFO  - RS: POST (200) - /api/restaurateurs/registrations [96 ms]
  ContentType: application/json
  {"userId":2,"token":"eyJhbGciOiJIUzUxMiJ9.eyJ1aWQiOjIsInN1YiI6IjA5NzM1OTI5NDQ0Iiwicm9sZSI6IlJlc3RhdXJhdGV1ciIsImV4cCI6MTYzMjgxNDQ5NCwiaWF0IjoxNjMyODEwODk0fQ.tEWlnWC908zOj4tdAC0UeS_u8in4JoadjZ2YfIfH3vx5BRSoPGL6F33_wziTFxNQV_2W-LqYYjetBXN-ylvIDg"}
2021-09-28 10:04:54,382 INFO  - RS Properties = [userId, token]
2021-09-28 10:04:54,386 INFO  - AssertRs Call: assertRs_register(Context, Map)
2021-09-28 10:04:54,401 INFO  - --------------- [step #4] ---------------
2021-09-28 10:04:54,422 INFO  - RQ: GET - http://localhost:8080/api/restaurateurs/2
HEADERS = {Authorization=eyJhbGciOiJIUzUxMiJ9.eyJ1aWQiOjIsInN1YiI6IjA5NzM1OTI5NDQ0Iiwicm9sZSI6IlJlc3RhdXJhdGV1ciIsImV4cCI6MTYzMjgxNDQ5NCwiaWF0IjoxNjMyODEwODk0fQ.tEWlnWC908zOj4tdAC0UeS_u8in4JoadjZ2YfIfH3vx5BRSoPGL6F33_wziTFxNQV_2W-LqYYjetBXN-ylvIDg}
2021-09-28 10:04:54,428 INFO  - RS: GET (200) - /api/restaurateurs/2 [6 ms]
  ContentType: application/json
  {"firstName":"zevm","lastName":"ttgl","cell":"09735929444","email":null,"id":2,"createdBy":"anonymous","createdDate":{"year":1400,"month":7,"day":6},"lastModifiedBy":"anonymous","lastModifiedDate":{"year":1400,"month":7,"day":6},"version":0}
2021-09-28 10:04:54,438 INFO  - RS Properties = [firstName, lastName, cell, email, id, createdBy, createdDate, lastModifiedBy, lastModifiedDate, version]
2021-09-28 10:04:54,442 INFO  - ***** [STATISTICS] *****
2021-09-28 10:04:54,443 INFO  - ID                             URI                                           Method  Status  Duration
2021-09-28 10:04:54,443 INFO  - RegisterRestaurateur.step #1   /api/restaurateurs/registrations/09735929444  GET     200     34
2021-09-28 10:04:54,443 INFO  - RegisterRestaurateur.step #2   /api/j4d/registrations/09735929444            GET     200     6
2021-09-28 10:04:54,443 INFO  - RegisterRestaurateur.register  /api/restaurateurs/registrations              POST    200     96
2021-09-28 10:04:54,443 INFO  - RegisterRestaurateur.step #4   /api/restaurateurs/2                          GET     200     6
2021-09-28 10:04:54,443 INFO  - ***** [PASSED SUCCESSFULLY in 512 ms] *****
```

## Description of the XML and Groovy Files

The `<vars/>` are defining some variables for providing initial data for the test. In the `value`, you can call a
function in the Groovy file by placing `${_.FUNCTION(PARAMS)}` in your attribute like the `cell` variable (the `_` is a
variable in Artemis's Groovy integration which refers to the object of `Script`
created from the Groovy file).

The XML file consists of one or more `<scenario>`. The given name for the scenario is necessary. Each scenario has one
or more HTTP request(s). The tags `<get>`, `<post>`, `<put>`, `<patch>`, and `<delete>` are defining an HTTP request as
the tag name is the HTTP method. The `id` attribute assigns an identifier to the request, which is not required but
highly recommended. Most of the conventions in the Artemis is based on this `id`. For example, you can refer to the
response body of a request via its `id`, such as `${register.rs.token}` used as a header parameter in the latest `<get>`
request.

For `url`, you should set the resource part without the server or common prefix part of the URL. In the above example,
the final url for `register` request is `http://localhost:8080/api/restaurateurs/registrations` (
the `http://localhost:8080/api` part is passed as `baseUrl`).

After sending the request, the result can be asserted in `<asserRs>` tag. The `status` attribute has the expected value
for the http response code. Since Artemis supposes your response is an JSON object by default, this JSON object should
have the list of `properties`.

## Parallel Execution and Performance Test

You can do performance test with Artemis. Just set the `parallel` config property with a number greater than 1. Then
Artemis executes the XML file concurrently, and creates a thread for each execution. For each thread, a log
file `artemis-th-NN.log` is created, and all the execution output is written in that log file. At the end a
comprehensive statistical report is published in `artemis.log` file such as the following one:

```text
2021-09-28 10:09:34,212 INFO  - ID                             Status  Avg     Count  Min  Min(th)          Max  Max(th)
2021-09-28 10:09:34,213 INFO  - RegisterRestaurateur.step #1   200     24.00   5      2    [artemis-th-03]  88   [artemis-th-01]
2021-09-28 10:09:34,213 INFO  - RegisterRestaurateur.step #2   200     7.00    5      5    [artemis-th-05]  10   [artemis-th-01]
2021-09-28 10:09:34,213 INFO  - RegisterRestaurateur.register  200     180.80  5      117  [artemis-th-03]  242  [artemis-th-01]
2021-09-28 10:09:34,213 INFO  - RegisterRestaurateur.step #4   200     9.00    5      5    [artemis-th-03]  18   [artemis-th-05]
```

## Start Developing

You can create both XML and Groovy files by calling the following maven command in the root of your project or module:

```shell
mvn org.devocative.artemis:artemis-maven-plugin:1.5:create
```

After successful execution, the two files `artemis.xml` and `artemis.groovy`, are generated in `src/test/resources`
directory.

**Note:** To alter the generation, you can append `-Dname=NAME` to the command to generate files as `NAME.xml`
and `NAME.groovy`.

