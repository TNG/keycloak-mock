package com.tngtech.keycloakmock.standalone;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    description = "Starts a stand-alone keycloak mock.",
    name = "keycloakmock",
    mixinStandardHelpOptions = true)
public class Main implements Callable<Void> {

  @Option(
      names = {"-p", "--port"},
      description = "The port on which to run (default: ${DEFAULT-VALUE}).")
  private int port = 8000;

  @Option(
      names = {"-s", "--https"},
      description = "Whether to use HTTPS instead of HTTP.")
  private boolean tls;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Void call() {
    new Server(port, tls);
    System.out.println("Server is running on " + (tls ? "https" : "http") + "://localhost:" + port);
    return null;
  }
}
