[![Java CI](https://github.com/TNG/keycloak-mock/workflows/Java%20CI/badge.svg?branch=master)](https://github.com/TNG/keycloak-mock/actions?query=branch%3Amaster)
[![Github release date](https://img.shields.io/github/release-date/TNG/keycloak-mock.svg?logo=github)
 ![Github release](https://img.shields.io/github/release/TNG/keycloak-mock.svg?logo=github)](https://github.com/TNG/keycloak-mock/releases)
[![Maven release](https://img.shields.io/maven-central/v/com.tngtech.keycloakmock/mock)](https://search.maven.org/search?q=com.tngtech.keycloakmock)
[![Sonarcloud](https://img.shields.io/sonar/quality_gate/TNG_keycloak-mock?server=https%3A%2F%2Fsonarcloud.io)](https://sonarcloud.io/dashboard?id=TNG_keycloak-mock)

# Keycloak Mock

[Keycloak](https://www.keycloak.org) is a single sign-on solution that supports the
[Open ID connect](https://openid.net/connect/) standard. However, it does not deliver any
test support. This library is intended to fill that gap.

## Usage

All artifacts are available on [Maven Central Repository](https://search.maven.org/) under the
group ID `com.tngtech.keycloakmock`.

### Testing authenticated backend calls

When testing a REST backend that is protected by a Keycloak adapter, the mock allows to generate
valid access tokens with configurable content (e.g. roles).

You can create and start the mock directly from the `mock` artifact using Maven

```maven
<dependency>
    <groupId>com.tngtech.keycloakmock</groupId>
    <artifactId>mock</artifactId>
    <scope>test</scope>
    <version>0.5.0</version>
</dependency>
```

or Gradle

```gradle
testImplementation 'com.tngtech.keycloakmock:mock:0.5.0'
```

like this:

```java
import com.tngtech.keycloakmock.api.KeycloakMock;

class Test {

  KeycloakMock mock = new KeycloakMock(8000, "master");

  static {
    mock.start();
  }

}
```

Or you can use use the `mock-junit` artifact with JUnit4

```java
import com.tngtech.keycloakmock.junit.KeycloakMockRule;

public class Test {
  @ClassRule
  public static KeycloakMockRule mock = new KeycloakMockRule();

  // ...
    
}
```

or the `mock-junit5` artifact with JUnit5

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

### Developing / testing frontends

It is also possible to run a stand-alone mock server that provides a login page where a username
and an optional list of roles can be specified. Just get the (self-contained) `standalone` artifact,
e.g. from [Maven Central](https://search.maven.org/artifact/com.tngtech.keycloakmock/standalone),
and run it:

```bash
$ java -jar standalone.jar &
Server is running on http://localhost:8000
```

Please note that currently only the authorization code flow and the implicit flow are supported.

## License

This project is licensed under the Apache 2.0 license (see [LICENSE](LICENSE)).
