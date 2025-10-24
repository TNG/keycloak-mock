package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;

import dagger.Lazy;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.common.template.TemplateEngine;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DocumentationRoute implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(DocumentationRoute.class);

  @Nonnull private final Lazy<Router> lazyRouter;
  @Nonnull private final TemplateEngine engine;

  @Inject
  public DocumentationRoute(@Nonnull Lazy<Router> lazyRouter, @Nonnull TemplateEngine engine) {
    this.lazyRouter = lazyRouter;
    this.engine = engine;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    List<Route> descriptions =
        lazyRouter.get().getRoutes().stream()
            // annoyingly, if a path is set but the name is null, the path is returned instead
            .filter(r -> r.getName() != null && !Objects.equals(r.getName(), r.getPath()))
            .sorted(Comparator.comparing(Route::getPath))
            .collect(Collectors.toList());
    if (APPLICATION_JSON.contentEquals(routingContext.getAcceptableContentType())) {
      JsonObject result = new JsonObject();
      descriptions.forEach(
          r -> {
            JsonObject routeDescription = new JsonObject();
            routeDescription.put(
                "methods",
                r.methods().stream().map(HttpMethod::name).sorted().collect(Collectors.toList()));
            routeDescription.put("description", r.getName());
            result.put(r.getPath(), routeDescription);
          });
      routingContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).end(result.encode());
    } else {
      routingContext.put("descriptions", descriptions);
      engine
          .render(routingContext.data(), "documentation.ftl")
          .onSuccess(b -> routingContext.response().putHeader(CONTENT_TYPE, TEXT_HTML).end(b))
          .onFailure(
              t -> {
                LOG.error("Unable to render documentation page", t);
                routingContext.fail(t);
              });
    }
  }
}
