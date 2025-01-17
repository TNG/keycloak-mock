package com.tngtech.keycloakmock.standalone;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;

import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.api.LoginRoleMapping;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    description = "Starts a stand-alone keycloak mock.",
    name = BuildConfig.NAME,
    version = BuildConfig.VERSION,
    mixinStandardHelpOptions = true)
public class Main implements Callable<Void> {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-p", "--port"},
      description = "The port on which to run (default: ${DEFAULT-VALUE}).")
  private int port = 8000;

  @Option(
      names = {"-s", "--https"},
      description = "Whether to use HTTPS instead of HTTP.")
  private boolean tls;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-cp", "--contextPath"},
      description =
          "Keycloak context path (default: ${DEFAULT-VALUE}). "
              + "If present, must be prefixed with '/', eg. --contextPath=/example-path")
  private String contextPath = "/auth";

  @Option(
      names = {"-ncp", "--noContextPath"},
      description = "If present context path will not be used. Good for mocking Keycloak 18.0.0+.")
  private boolean noContextPath;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-a", "--audiences"},
      description =
          "Audiences to set in the token in addition to the client_id (default: [server]).",
      paramLabel = "AUDIENCE",
      split = ",")
  private List<String> audiences = Collections.emptyList();

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-sc", "--scopes"},
      description = "Scopes to add to generated token (default: ${DEFAULT-VALUE}).",
      paramLabel = "SCOPE",
      split = ",")
  private List<String> scopes = Collections.singletonList("openid");

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-tl", "--tokenLifespan"},
      description =
          "Lifespan of generated tokens (default: ${DEFAULT-VALUE}). Valid values are e.g. '10h',"
              + " '15m', '3m45s'.")
  private String tokenLifespan = "10h";

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-rm", "--roleMapping"},
      description =
          "Where to add the roles given in the login dialog (default: ${DEFAULT-VALUE}). Valid"
              + " options: ${COMPLETION-CANDIDATES}")
  private LoginRoleMapping loginRoleMapping = LoginRoleMapping.TO_REALM;

  public static void main(@Nonnull final String[] args) {
    if (System.getProperty("org.slf4j.simpleLogger.logFile") == null) {
      System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    }
    int exitCode = new CommandLine(new Main()).execute(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  @Override
  public Void call() {
    String usedContextPath = noContextPath ? "" : contextPath;

    new KeycloakMock(
            aServerConfig()
                .withPort(port)
                .withTls(tls)
                .withContextPath(usedContextPath)
                .withDefaultAudiences(audiences)
                .withDefaultScopes(scopes)
                .withDefaultTokenLifespan(getParsedLifespan())
                .withLoginRoleMapping(loginRoleMapping)
                .build())
        .start();

    String url = (tls ? "https" : "http") + "://localhost:" + port;
    LOG.info("Server is running on {}{}", url, usedContextPath);
    LOG.info("A documentation of all endpoints is available at {}/docs", url);

    return null;
  }

  private Duration getParsedLifespan() {
    // simple trick: just interpret the given string as the suffix part of a Duration string
    return Duration.parse("PT" + tokenLifespan.toUpperCase(Locale.ROOT));
  }
}
