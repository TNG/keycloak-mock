# Keycloak Mock

Keycloak is a single sign-on server that supports the Open ID connect standard.
However, it does not deliver any test support. This library is intended to fill that gap.

## Usage

TODO: set up CI + release process + publishing

### Testing authenticated backend calls

When testing a REST backend that is protected by a Keycloak adapter, the mock allows to generate
valid access tokens with configurable content (e.g. roles).

You can create and start the mock directly:

```
import com.tngtech.keycloakmock.api.KeycloakVerificationMock;

...

KeycloakVerificationMock mock = new KeycloakVerificationMock(8000, "master);
mock.start();
```

or use the JUnit4 test rule from module mock-junit

```
import com.tngtech.keycloakmock.api.junit.KeycloakMock;

public class Test {
    @ClassRule
    public static KeyCloakMock mock = new KeycloakMock();

    ...
    
}
```

or JUnit5 extension from module mock-junit5

```
import com.tngtech.keycloakmock.api.junit5.KeycloakMock;

class Test {
    @RegisterExtension
    static KeyCloakMock mock = new KeyCloakMock();

    ...
    
}
```

You can then get a token of your choosing by providing a TokenConfig:

```
import static com.tngtech.keycloakmock.api.TokenConfig.aTokenConfig;

...

String accessToken = mock.getAccessToken(aTokenConfig().withRole("ROLE_ADMIN").build());
```

### Developing / testing frontends

It is also possible to run a stand-alone mock server that provides a login page where a username
and an optional list of roles can be specified. Just download the (self-contained) standalone JAR
and run it:

```bash
$ java -jar standalone.jar &
Server is running on http://localhost:8000
```

Please note that currently, only the authorization code flow is supported.

## License

This project is licensed under the Apache 2.0 license (see [LICENSE](LICENSE)).
