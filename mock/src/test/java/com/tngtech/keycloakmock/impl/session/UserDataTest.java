package com.tngtech.keycloakmock.impl.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UserDataTest {
  static Stream<Arguments> inputAndExpectedOutput() {
    return Stream.of(
        arguments(
            "jane.doe@example.com",
            "example.org",
            "jane.doe",
            "Jane",
            "Doe",
            "Jane Doe",
            "jane.doe@example.com"),
        arguments(
            "jAnE.DoE@example.com",
            "example.org",
            "jAnE.DoE",
            "JAnE",
            "DoE",
            "JAnE DoE",
            "jAnE.DoE@example.com"),
        arguments(
            "john.frederic.doe",
            "example.org",
            "john.frederic.doe",
            "John Frederic",
            "Doe",
            "John Frederic Doe",
            "john.frederic.doe@example.org"),
        arguments("jane", "example.org", "jane", null, "Jane", "Jane", "jane@example.org"),
        arguments(
            "john_doe@example",
            "example.org",
            "john_doe",
            "John",
            "Doe",
            "John Doe",
            "john_doe@example"),
        arguments(
            "Jane_Doe",
            "example.org",
            "Jane_Doe",
            "Jane",
            "Doe",
            "Jane Doe",
            "Jane_Doe@example.org"),
        arguments(
            "john doe",
            "example.org",
            "john doe",
            "John",
            "Doe",
            "John Doe",
            "john+doe@example.org"),
        arguments("j", "example.org", "j", null, "J", "J", "j@example.org"),
        arguments("J", "example.org", "J", null, "J", "J", "J@example.org"),
        arguments(".", "example.org", ".", null, ".", ".", ".@example.org"),
        arguments("_", "example.org", "_", null, "_", "_", "_@example.org"));
  }

  @ParameterizedTest
  @MethodSource("inputAndExpectedOutput")
  void userData_is_extracted_correctly(
      String username,
      String hostname,
      String expectedPreferredUsername,
      String expectedGivenName,
      String expectedFamilyName,
      String expectedName,
      String expectedEmail) {
    UserData result = UserData.fromUsernameAndHostname(username, hostname);

    assertThat(result.getSubject()).isEqualTo(username);
    assertThat(result.getPreferredUsername()).isEqualTo(expectedPreferredUsername);
    assertThat(result.getGivenName()).isEqualTo(expectedGivenName);
    assertThat(result.getFamilyName()).isEqualTo(expectedFamilyName);
    assertThat(result.getName()).isEqualTo(expectedName);
    assertThat(result.getEmail()).isEqualTo(expectedEmail);
  }
}
