package com.tngtech.keycloakmock.standalone;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;

import com.tngtech.keycloakmock.api.KeycloakMock;
import java.util.Collections;
import java.util.List;
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
      description = "Keycloak context path.")
  private String contextPath = "/auth";

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      names = {"-ncp", "--noContextPath"},
      description = "Keycloak context path.")
  private boolean noContextPath;

  @Option(
      names = {"-r", "--mapRolesToResources"},
      description = "If set, roles will be assigned to these resources instead of the realm.",
      paramLabel = "RESOURCE",
      split = ",")
  private final List<String> resourcesToMapRolesTo = Collections.emptyList();

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
    new KeycloakMock(
            aServerConfig()
                .withPort(port)
                .withTls(tls)
                .withContextPath(noContextPath ? "" : contextPath)
                .withResourcesToMapRolesTo(resourcesToMapRolesTo)
                .build())
        .start();
    LOG.info("Server is running on {}://localhost:{}{}", (tls ? "https" : "http"), contextPath, port);
    return null;
  }
}
