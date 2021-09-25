# Artemis

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.devocative.artemis/artemis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.devocative.artemis/artemis)

![Artemis Structure](/doc/logo.png)

## Introduction

Artemis is an easy and simple REST API test library. You can use your most powerful tool to apply Artemis, your IDE. You
just need to develop an XML file, and a Groovy file to test your REST APIs. The XML file describes the structure flow of
your scenarios by defining all HTTP requests and then asserting the status of response and its body. The Groovy file
contains lots of functions, called by XML file, to present all the behavioral parts during your test, such as creating
complex dynamic data, asserting the response body with all Assertions libraries, and processing the data necessary to
the test scenarios.

You can integrate Artemis in two ways into your project:

1. Calling its API in a JUnit method to integrate it with your other tests
2. Adding its maven plugin to your pom and then executing its goal in a more manual way

## An Example

The following is an `artemis.xml` file:

```xml
<!DOCTYPE artemis PUBLIC "-//Devocative.Org//Artemis 1.0//EN"
		"https://devocative.org/dtd/artemis-1.0.dtd">

<artemis>

	<vars>
		<var name="cell" value="09${_.generate(9, '0'..'9')}"/>
		<var name="firstName" value="${_.generate(4, 'a'..'z')}"/>
		<var name="lastName" value="${_.generate(4, 'a'..'z')}"/>
		<var name="password" value="${_.generate(9, '0'..'9', 'a'..'z')}"/>
	</vars>

	<scenario name="RegisterRestaurateur">
		<get url="/restaurateurs/registrations/${cell}">
			<assertRs status="200" body="empty"/>
		</get>

		<get url="/j4d/registrations/${cell}">
			<assertRs status="200" properties="code"/>
		</get>

		<post url="/restaurateurs/registrations" id="register">
			<body><![CDATA[
{
  "firstName": "${firstName}",
  "lastName": "${lastName}",
  "cell": "${cell}",
  "code": "${_prev.rs.code}",
  "password": "${password}"
}
            ]]></body>

			<assertRs status="200" properties="userId,token"/>
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

and here is the `artemis.groovy`:

```groovy
import org.devocative.artemis.Context

def before(Context ctx) {
}

def generate(int n, List<String>... alphaSet) {
	def list = alphaSet.flatten()
	new Random().with {
		(1..n).collect { list[nextInt(list.size())] }.join()
	}
}
```

By default, these two files should be in your `src/test/resources` directory.

### Execution

Some of Artemis parameters are passed before its execution. The most important one is `baseUrl`, which is prepended to
the `url` of each request in the XML file. These parameters are passed to `Config` object (`name` is just passed as
constructor parameter)
or `<configuration>` in the maven.

Parameter | Default Value           | Description
----------|-------------------------|-------------
`name`    | `artemis`               | looking for `<name>.xml` and `<name>.groovy` files for execution
`baseUrl` | `http://localhost:8080` | prepended to the `url` of each request in the XML file
`devMode` | `false`                 | store a memory object as the state of the test for incremental development of XML and Groovy files
`baseDir` | `src/test/resources`    | looking for the XML and Groovy files in this directory

#### JUnit Integration

First add the following dependency

```xml

<dependency>
	<groupId>org.devocative.artemis</groupId>
	<artifactId>artemis-core</artifactId>
	<version>1.1</version>
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
			.setBaseUrl(String.format("http://localhost:%s/api", port))
		);
	}
}
```

#### Maven

Add the following plugin to your `pom.xml` file. Then you can execute the `artemis:run` goal.

```xml

<plugin>
	<groupId>org.devocative.artemis</groupId>
	<artifactId>artemis-maven-plugin</artifactId>
	<version>1.1</version>
	<configuration>
		<baseUrl>http://localhost:8080/api</baseUrl>
	</configuration>
</plugin>
```

You can also execute following command in the root of your project or module without adding the above plugin:

```shell
mvn org.devocative.artemis:artemis-maven-plugin:1.1:run -DbaseUrl=http://localhost:8080/api
```

### Output

Artemis uses the most common way to output the result: log files. After execution, Artemis creates a `logs` directory
and flush all the result in `artemis.log` file. This file will be rotated based on the time and size. At the end of
successful execution, a statistical summary is illustrated.

```text
2021-09-25 19:44:31,233 INFO  - *---------*---------*
2021-09-25 19:44:31,268 INFO  - |   A R T E M I S   |
2021-09-25 19:44:31,268 INFO  - *---------*---------*
2021-09-25 19:44:31,400 INFO  - Global Var: name=[cell] value=[09853817189]
2021-09-25 19:44:31,432 INFO  - Global Var: name=[firstName] value=[qmjl]
2021-09-25 19:44:31,443 INFO  - Global Var: name=[lastName] value=[mhht]
2021-09-25 19:44:31,456 INFO  - Global Var: name=[password] value=[hcfbvsren]
2021-09-25 19:44:31,457 INFO  - =============== [RegisterRestaurateur] ===============
2021-09-25 19:44:31,488 INFO  - --------------- [step #1] ---------------
2021-09-25 19:44:31,501 INFO  - RQ: GET - http://localhost:8080/api/restaurateurs/registrations/09853817189
2021-09-25 19:44:31,531 INFO  - RS: GET (200) - /api/restaurateurs/registrations/09853817189 [29 ms]
  ContentType: null
  
2021-09-25 19:44:31,546 INFO  - --------------- [step #2] ---------------
2021-09-25 19:44:31,554 INFO  - RQ: GET - http://localhost:8080/api/j4d/registrations/09853817189
2021-09-25 19:44:31,561 INFO  - RS: GET (200) - /api/j4d/registrations/09853817189 [6 ms]
  ContentType: application/json
  {"code":"9130"}
2021-09-25 19:44:31,598 INFO  - --------------- [register] ---------------
2021-09-25 19:44:31,636 INFO  - RQ: POST - http://localhost:8080/api/restaurateurs/registrations
{
    "firstName": "qmjl",
    "lastName": "mhht",
    "cell": "09853817189",
    "code": "9130",
    "password": "hcfbvsren"
}
2021-09-25 19:44:31,729 INFO  - RS: POST (200) - /api/restaurateurs/registrations [92 ms]
  ContentType: application/json
  {"userId":7,"token":"eyJhbGciOiJIUzUxMiJ9.eyJ1aWQiOjcsInN1YiI6IjA5ODUzODE3MTg5Iiwicm9sZSI6IlJlc3RhdXJhdGV1ciIsImV4cCI6MTYzMjU5MDA3MSwiaWF0IjoxNjMyNTg2NDcxfQ.ZgmUoukkh-mYgVhS1voIj0va36mt4HO1ktSAHwgc5ZtmGVFwcuPoELNVs6tsThLYS-z4X8p8ThwWx22Q_zb_KA"}
2021-09-25 19:44:31,742 INFO  - --------------- [step #4] ---------------
2021-09-25 19:44:31,762 INFO  - RQ: GET - http://localhost:8080/api/restaurateurs/7
HEADERS = {Authorization=eyJhbGciOiJIUzUxMiJ9.eyJ1aWQiOjcsInN1YiI6IjA5ODUzODE3MTg5Iiwicm9sZSI6IlJlc3RhdXJhdGV1ciIsImV4cCI6MTYzMjU5MDA3MSwiaWF0IjoxNjMyNTg2NDcxfQ.ZgmUoukkh-mYgVhS1voIj0va36mt4HO1ktSAHwgc5ZtmGVFwcuPoELNVs6tsThLYS-z4X8p8ThwWx22Q_zb_KA}
2021-09-25 19:44:31,767 INFO  - RS: GET (200) - /api/restaurateurs/7 [4 ms]
  ContentType: application/json
  {"firstName":"qmjl","lastName":"mhht","cell":"09853817189","email":null,"id":7,"createdBy":"anonymous","createdDate":{"year":1400,"month":7,"day":3},"lastModifiedBy":"anonymous","lastModifiedDate":{"year":1400,"month":7,"day":3},"version":0}
2021-09-25 19:44:31,780 INFO  - ***** [STATISTICS] *****
2021-09-25 19:44:31,781 INFO  - ID                             URI                                           Method  Status  Duration
2021-09-25 19:44:31,781 INFO  - RegisterRestaurateur.step #1   /api/restaurateurs/registrations/09853817189  GET     200     29
2021-09-25 19:44:31,781 INFO  - RegisterRestaurateur.step #2   /api/j4d/registrations/09853817189            GET     200     6
2021-09-25 19:44:31,781 INFO  - RegisterRestaurateur.register  /api/restaurateurs/registrations              POST    200     92
2021-09-25 19:44:31,781 INFO  - RegisterRestaurateur.step #4   /api/restaurateurs/7                          GET     200     4
2021-09-25 19:44:31,781 INFO  - ***** [PASSED SUCCESSFULLY in 513 ms] *****
```

### Description of the Files

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

## Start Developing

You can create both XML and Groovy files by calling the following maven command in the root of your project or module:

```shell
mvn org.devocative.artemis:artemis-maven-plugin:1.1:create
```

After successful execution, the two files, `artemis.xml` and `artemis.grrovy`, are generated in `src/test/resources`
directory.

**Note:** To alter the generation, you can append `-Dname=NAME` to the command to generate files as `NAME.xml`
and `NAME.groovy`.

