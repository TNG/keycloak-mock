package com.tngtech.keycloakmock.impl.dagger;

import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import java.security.KeyStore;
import java.security.PublicKey;
import javax.inject.Named;
import javax.inject.Singleton;

@Component(modules = ServerModule.class)
@Singleton
public interface ServerComponent {
  HttpServer server();

  Vertx vertx();

  @Component.Builder
  abstract class Builder {
    Builder() {
      vertx(Vertx.vertx());
    }

    @BindsInstance
    abstract Builder vertx(Vertx vertx);

    @BindsInstance
    public abstract Builder serverConfig(ServerConfig serverConfig);

    @BindsInstance
    public abstract Builder publicKey(PublicKey key);

    @BindsInstance
    public abstract Builder keyId(@Named("keyId") String keyId);

    @BindsInstance
    public abstract Builder keyStore(KeyStore keyStore);

    @BindsInstance
    public abstract Builder tokenGenerator(TokenGenerator tokenGenerator);

    public abstract ServerComponent build();
  }
}
