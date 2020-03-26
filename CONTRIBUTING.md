# Contributing

Contributions are very welcome. The following will provide some helpful guidelines.


## How to build the project

KeycloakMock requires JDK 8. To build the entire project run
```bash
./gradlew build
```
### Core functionality

The main logic of the KeycloakMock is located in the [mock](mock) module. You can find functionality to generate user-defined access tokens
and the mock endpoint to retrieve the public signature key here. To build, run
```
./gradlew mock:build
```

### JUnit integration

JUnit integration is provided in [mock-junit](mock-junit) (for JUnit 4) and [mock-junit5](mock-junit5).

### Standalone Mock

[standalone](standalone) contains a lightweight standalone application with support for the OpenID Connect authorization code flow.

## How to contribute

If you want to submit a contribution, please follow the following workflow:

* Fork the project
* Create a feature branch
* Add your contribution
* Add sufficient tests along with new features
* When you're completely done, build the project and run all tests via `./gradlew clean build`
* Create a Pull Request

### Commits

Commit messages should be clear and fully elaborate the context and the reason of a change.
If your commit refers to an issue, please post-fix it with the issue number, e.g.

```
Issue: #123
```

Furthermore, commits should be signed off according to the [DCO](DCO).

### Pull Requests

If your Pull Request resolves an issue, please add a respective line to the end, like

```
Resolves #123
```

### Formatting

This project uses [google code style](https://github.com/google/styleguide). Running ```./gradlew googleJavaFormat``` 
will automatically format java code correctly. See [here](https://github.com/google/google-java-format) for IDE integration.
