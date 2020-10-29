package com.tngtech.keycloakmock.test;

import java.util.List;

public class ConfigurationResponse {
  public String issuer;
  public String authorization_endpoint;
  public String token_endpoint;
  public String jwks_uri;
  public List<String> response_types_supported;
  public List<String> subject_types_supported;
  public List<String> id_token_signing_alg_values_supported;
  public String end_session_endpoint;
}
