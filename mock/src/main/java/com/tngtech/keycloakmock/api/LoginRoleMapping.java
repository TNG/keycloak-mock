package com.tngtech.keycloakmock.api;

/** Where to apply roles given in the Login page. */
public enum LoginRoleMapping {
  /** The roles should be added only to the realm. */
  TO_REALM,
  /**
   * The roles should be added only to the resources.
   *
   * <p>The audiences of the token are used as the list of resources.
   */
  TO_RESOURCE,
  /** The roles should be added both to the realm and the resources. */
  TO_BOTH
}
