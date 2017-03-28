package org.folio.okapi.deployment;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.folio.okapi.bean.DeploymentDescriptor;
import static org.folio.okapi.common.HttpResponse.responseError;
import static org.folio.okapi.common.HttpResponse.responseJson;
import static org.folio.okapi.common.HttpResponse.responseText;

/**
 * Web service to manage deployments.
 */
public class DeploymentWebService {
  private final Logger logger = LoggerFactory.getLogger("okapi");
  private final DeploymentManager md;
  private final static String INST_ID = "instid";

  public DeploymentWebService(DeploymentManager md) {
    this.md = md;
  }

  public void create(RoutingContext ctx) {
    try {
      final DeploymentDescriptor pmd = Json.decodeValue(ctx.getBodyAsString(),
              DeploymentDescriptor.class);
      md.deploy(pmd, res -> {
        if (res.failed()) {
          responseError(ctx, res.getType(), res.cause());
        } else {
          final String s = Json.encodePrettily(res.result());
          responseJson(ctx, 201)
                  .putHeader("Location", ctx.request().uri() + "/" + res.result().getInstId())
                  .end(s);
        }
      });
    } catch (DecodeException ex) {
      responseError(ctx, 400, ex);
    }
  }

  public void delete(RoutingContext ctx) {
    final String id = ctx.request().getParam(INST_ID);
    md.undeploy(id, res -> {
      if (res.failed()) {
        responseError(ctx, res.getType(), res.cause());
      } else {
        responseText(ctx, 204).end();
      }
    });
  }

  public void list(RoutingContext ctx) {
    md.list(res -> {
      if (res.failed()) {
        responseError(ctx, res.getType(), res.cause());
      } else {
        final String s = Json.encodePrettily(res.result());
        responseJson(ctx, 200).end(s);
      }
    });
  }

  public void get(RoutingContext ctx) {
    final String id = ctx.request().getParam(INST_ID);
    md.get(id, res -> {
      if (res.failed()) {
        responseError(ctx, res.getType(), res.cause());
      } else {
        final String s = Json.encodePrettily(res.result());
        responseJson(ctx, 200).end(s);
      }
    });
  }

}
