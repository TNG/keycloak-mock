[![Java CI](https://github.com/TNG/keycloak-mock/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/TNG/keycloak-mock/actions?query=branch%3Amain)
[![Github release date](https://img.shields.io/github/release-date/TNG/keycloak-mock.svg?logo=github)
![Github release](https://img.shields.io/github/release/TNG/keycloak-mock.svg?logo=github)](https://github.com/TNG/keycloak-mock/releases)
[![Maven release](https://img.shields.io/maven-central/v/com.tngtech.keycloakmock/mock?color=informational)](https://search.maven.org/search?q=com.tngtech.keycloakmock)
[![Sonarcloud](https://img.shields.io/sonar/quality_gate/TNG_keycloak-mock?server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=TNG_keycloak-mock)

# Keycloak Mock

[Keycloak](https://www.keycloak.org) is a single sign-on solution that supports the
[Open ID connect](https://openid.net/connect/) standard. However, it does not deliver any test
support. This library is intended to fill that gap.

## Recent changes

Have a look at our [release notes](https://github.com/TNG/keycloak-mock/releases) for recent
releases and changes.

## Usage

All artifacts are available on [Maven Central Repository](https://search.maven.org/) under the group
ID `com.tngtech.keycloakmock`.

### Testing authenticated backend calls

When testing a REST backend that is protected by a Keycloak adapter, the mock allows to generate
valid access tokens with configurable content (e.g. roles).

You can create and start the mock directly from the `mock` artifact using Maven

```maven
<dependency>
    <groupId>com.tngtech.keycloakmock</groupId>
    <artifactId>mock</artifactId>
    <scope>test</scope>
    <version>0.17.0</version>
</dependency>
```

or Gradle

```gradle
testImplementation 'com.tngtech.keycloakmock:mock:0.17.0'
```

like this:

```java
import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;

import com.tngtech.keycloakmock.api.KeycloakMock;

class Test {

  void checkSomething() {
    KeycloakMock mock = new KeycloakMock(aServerConfig().withPort(8000).withDefaultRealm("master").build());
    mock.start();

    // do your test stuff

    mock.stop();
  }

  void quarkusKeycloakMocks() {
    // to mock Keycloak without context path (v18.0.0+)
    KeycloakMock mockNoContextPath = new KeycloakMock(aServerConfig().withNoContextPath().build());
    // or to use custom one
    KeycloakMock mockCustomContextPath = new KeycloakMock(aServerConfig().withContextPath("/context-path").build());
    // if context path is not provided, '/auth' will be used as default due to backward compatibility reasons
    KeycloakMock mockDefaultContextPath = new KeycloakMock(aServerConfig().build());
    // ...
  }
}
```

You can also use the convenience wrapper `mock-junit` for JUnit4

```java
import com.tngtech.keycloakmock.junit.KeycloakMockRule;

public class Test {
  @ClassRule
  public static KeycloakMockRule mock = new KeycloakMockRule();

  // ...

}
```

or `mock-junit5` for JUnit5

```java
import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;

class Test {
  @RegisterExtension
  static KeycloakMockExtension mock = new KeycloakMockExtension();

  // ...

}
```

to let JUnit start the mock for you.

You can then generate a token of your choosing by providing a TokenConfig:

```java
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

class Test {

  String accessToken = mock.getAccessToken(aTokenConfig().withRole("ROLE_ADMIN").build());

  // ...

}
```

For a more in-detail test case, please have a look at
the [AuthenticationTest](example-backend/src/test/java/com/tngtech/keycloakmock/examplebackend/AuthenticationTest.java)
in our example backend project.

In addition to generating and signing tokens programmatically, the mock also offers

* user login (using implicit or authorization code flow, including support for redirect
  to `http://localhost` and `urn:ietf:wg:oauth:2.0:oob` for desktop applications)
  * instead of a password, you can enter the roles of the user
* client credentials authentication
* resource owner password credentials authentication (both for public and confidential clients)

Note that as this is a mock, all flows are allowed for any client. For simplicity all successful
calls to the token endpoint return the same response including a refresh token, even for flows which
should not contain it according to the specifications.

### Developing / testing frontends

It is also possible to run the mock server as a stand-alone application. Just get the (
self-contained)
`standalone` artifact, e.g.
from [Maven Central](https://search.maven.org/artifact/com.tngtech.keycloakmock/standalone), and run
it:

```bash
$ java -jar standalone.jar &
[main] INFO com.tngtech.keycloakmock.standalone.Main - Server is running on http://localhost:8000
```

The stand-alone server can be configured using command line parameters. You can call it
with `--help` to get a list of all options.

You can even use it as a replacement in end-to-end tests, as the server is e.g. compatible with
`cypress-keycloak`. Have a look at the [example-frontend-react](example-frontend-react) project on
this can be set up.

## Server Method documentation

You can get a list of all implemented endpoints of the mock at `http://localhost:8000/docs`. This is mainly meant for
checking if a specific endpoint you want to use is supported by the mock (yet).

## License

This project is licensed under the Apache 2.0 license (see [LICENSE](LICENSE)).
