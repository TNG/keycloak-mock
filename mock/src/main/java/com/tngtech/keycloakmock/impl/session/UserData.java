package com.tngtech.keycloakmock.impl.session;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UserData {
  @Nonnull private final String subject;
  @Nullable private final String givenName;
  @Nonnull private final String familyName;
  @Nonnull private final String email;
  @Nonnull private final String preferredUsername;

  private UserData(
      @Nonnull String subject,
      @Nullable String givenName,
      @Nonnull String familyName,
      @Nonnull String email,
      @Nonnull String preferredUsername) {
    this.subject = subject;
    this.givenName = givenName;
    this.familyName = familyName;
    this.email = email;
    this.preferredUsername = preferredUsername;
  }

  public static UserData fromUsernameAndHostname(
      @Nonnull String username, @Nonnull String hostname) {
    String email;
    String preferredUsername;
    int atIndex = username.indexOf("@");
    if (atIndex > 0) {
      preferredUsername = username.substring(0, atIndex);
      email = username.replace(' ', '+');
    } else {
      preferredUsername = username;
      email = username.replace(' ', '+') + "@" + hostname;
    }
    Name name = extractName(preferredUsername);
    return new UserData(username, name.givenName, name.familyName, email, preferredUsername);
  }

  @Nonnull
  private static Name extractName(@Nonnull String input) {
    List<String> names =
        Arrays.stream(input.split("[._ ]"))
            .filter(s -> !s.isEmpty())
            .map(
                s -> {
                  String firstChar = s.substring(0, 1);
                  return firstChar.toUpperCase() + s.substring(1);
                })
            .collect(Collectors.toList());
    if (names.isEmpty()) {
      return new Name(null, input);
    } else if (names.size() == 1) {
      return new Name(null, names.get(0));
    } else {
      int index = names.size() - 1;
      String familyName = names.get(index);
      names.remove(index);
      return new Name(String.join(" ", names), familyName);
    }
  }

  @Nonnull
  public String getSubject() {
    return subject;
  }

  @Nullable
  public String getGivenName() {
    return givenName;
  }

  @Nonnull
  public String getFamilyName() {
    return familyName;
  }

  @Nonnull
  public String getEmail() {
    return email;
  }

  @Nonnull
  public String getPreferredUsername() {
    return preferredUsername;
  }

  @Nonnull
  public String getName() {
    return givenName != null ? givenName + " " + familyName : familyName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserData userData = (UserData) o;
    return Objects.equals(subject, userData.subject)
        && Objects.equals(givenName, userData.givenName)
        && Objects.equals(familyName, userData.familyName)
        && Objects.equals(email, userData.email)
        && Objects.equals(preferredUsername, userData.preferredUsername);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, givenName, familyName, email, preferredUsername);
  }

  @Override
  public String toString() {
    return "UserData{"
        + "subject='"
        + subject
        + '\''
        + ", givenName='"
        + givenName
        + '\''
        + ", familyName='"
        + familyName
        + '\''
        + ", email='"
        + email
        + '\''
        + ", preferredUsername='"
        + preferredUsername
        + '\''
        + '}';
  }

  private static class Name {
    @Nullable private final String givenName;
    @Nonnull private final String familyName;

    private Name(@Nullable String givenName, @Nonnull String familyName) {
      this.givenName = givenName;
      this.familyName = familyName;
    }
  }
}
