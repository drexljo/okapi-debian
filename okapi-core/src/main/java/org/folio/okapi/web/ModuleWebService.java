package org.folio.okapi.web;

import io.vertx.core.Handler;
import org.folio.okapi.bean.ModuleDescriptor;
import io.vertx.core.Vertx;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.okapi.bean.ModuleDescriptorBrief;
import org.folio.okapi.service.ModuleManager;
import org.folio.okapi.service.ModuleStore;
import org.folio.okapi.service.TimeStampStore;
import static org.folio.okapi.common.ErrorType.*;
import static org.folio.okapi.common.HttpResponse.*;
import org.folio.okapi.common.ExtendedAsyncResult;
import org.folio.okapi.common.Failure;
import org.folio.okapi.common.Success;

/**
 * Services related to adding and deleting modules. All operations try to do the
 * thing on the locally running system first. If that succeeds, they update the
 * database, and tell other instances to reload the configuration.
 */
public class ModuleWebService {

  private final Logger logger = LoggerFactory.getLogger("okapi");

  ModuleManager moduleManager;
  ModuleStore moduleStore;
  EventBus eb;
  private final String eventBusName = "okapi.conf.modules";
  final private TimeStampStore timeStampStore;
  final String timestampId = "modules";
  private Long timestamp = (long) -1;

  public ModuleWebService(Vertx vertx,
          ModuleManager moduleService, ModuleStore moduleStore,
          TimeStampStore timeStampStore) {
    this.moduleManager = moduleService;
    this.moduleStore = moduleStore;
    this.timeStampStore = timeStampStore;

    this.eb = vertx.eventBus();
    eb.consumer(eventBusName, message -> {
      Long receivedStamp = (Long) (message.body());
      if (this.timestamp < receivedStamp) {
        reloadModules(rres -> {
          if (rres.succeeded()) {
            logger.debug("Reload of modules succeeded");
          } else {
            logger.fatal("Reload modules FAILED - No idea what to do about that!");
            // TODO - What can we do if reload fails here ?
            // We have nowhere to report failures. Declare the whole node dead?
          }
        });
      } else {
      }
    });
  }

  public void updateTimeStamp(Handler<ExtendedAsyncResult<Long>> fut) {
    timeStampStore.updateTimeStamp(timestampId, this.timestamp, res -> {
      if (res.succeeded()) {
        this.timestamp = res.result();
        fut.handle(new Success<>(timestamp));
      } else {
        fut.handle(res);
      }
    });
  }

  private void sendReloadSignal(Handler<ExtendedAsyncResult<Long>> fut) {
    updateTimeStamp(res -> {
      if (res.failed()) {
        fut.handle(res);
      } else {
        eb.publish(eventBusName, res.result());
        fut.handle(new Success<>(null));
      }
    });
  }

  public void create(RoutingContext ctx) {
    try {
      final ModuleDescriptor md = Json.decodeValue(ctx.getBodyAsString(),
              ModuleDescriptor.class);
      if (md.getId() == null || md.getId().isEmpty()) {
        md.setId(UUID.randomUUID().toString());
      }
      String validerr = md.validate();
      if (!validerr.isEmpty()) {
       responseError(ctx, 400, validerr);
      } else {
        moduleManager.create(md, cres -> {
          if (cres.failed()) {
            responseError(ctx, cres.getType(), cres.cause());
          } else {
            moduleStore.insert(md, ires -> {
              if (ires.succeeded()) {
                sendReloadSignal(sres -> {
                  if (sres.succeeded()) {
                    final String s = Json.encodePrettily(md);
                    responseJson(ctx, 201)
                            .putHeader("Location", ctx.request().uri() + "/" + md.getId())
                            .end(s);
                  } else { // TODO - What to if this fails ??
                    responseError(ctx, sres.getType(), sres.cause());
                  }
                });
              } else {
                // This can only happen in some kind of race condition, we should
                // have detected duplicates when creating in the manager.
                // TODO - How to test these cases?
                logger.warn("create failed " + ires.cause().getMessage());
                moduleManager.delete(md.getId(), dres -> { // remove from runtime too
                  if (dres.succeeded()) {
                    responseError(ctx, 500, ires.cause());
                    // Note, we return ires.cause, the reason why the insert failed.
                  } else {
                    // TODO - What to do now - the system may be inconsistent!
                    responseError(ctx, 500, ires.cause());
                  }
                });
              }
            });
          }
        });
      }
    } catch (DecodeException ex) {
      logger.debug("Failed to decode md: " + ctx.getBodyAsString());
      responseError(ctx, 400, ex);
    }
  }

  public void update(RoutingContext ctx) {
    try {
      final ModuleDescriptor md = Json.decodeValue(ctx.getBodyAsString(),
              ModuleDescriptor.class);
      final String id = ctx.request().getParam("id");
      if (!id.equals(md.getId())) {
        responseError(ctx, 400, "Module.id=" + md.getId() + " id=" + id);
        return;
      }
      String validerr = md.validate();
      if (!validerr.isEmpty()) {
        responseError(ctx, 400, validerr);
      } else {
        moduleManager.update(md, cres -> {
          if (cres.failed()) {
            responseError(ctx, cres.getType(), cres.cause());
          } else {
            moduleStore.update(md, ires -> {
              if (ires.succeeded()) {
                sendReloadSignal(sres -> {
                  if (sres.succeeded()) {
                    final String s = Json.encodePrettily(md);
                    responseJson(ctx, 200).end(s);
                  } else { // TODO - What to do if this fails ??
                    responseError(ctx, sres.getType(), sres.cause());
                  }
                });
              } else {
                responseError(ctx, ires.getType(), ires.cause());
              }
            });
          }
        });
      }
    } catch (DecodeException ex) {
      responseError(ctx, 400, ex);
    }
  }

  public void get(RoutingContext ctx) {
    final String id = ctx.request().getParam("id");
    final String q = "{ \"id\": \"" + id + "\"}";
    JsonObject jq = new JsonObject(q);
    //cli.find(collection, jq, res -> {
    moduleStore.get(id, res -> {
      if (res.succeeded()) {
        responseJson(ctx, 200).end(Json.encodePrettily(res.result()));
      } else {
        responseError(ctx, res.getType(), res.cause());
      }
    });
  }

  public void list(RoutingContext ctx) {
    moduleStore.getAll(res -> {
      if (res.succeeded()) {
        List<ModuleDescriptorBrief> ml = new ArrayList<>(res.result().size());
        for (ModuleDescriptor md : res.result()) {
          ml.add(new ModuleDescriptorBrief(md));
        }
        responseJson(ctx, 200).end(Json.encodePrettily(ml));
      } else {
        responseError(ctx, res.getType(), res.cause());
      }
    });
    // moduleManager.listIds(ctx);
  }

  /**
   * Delete a module. TODO - Is the logic the right way around? What to check
   * first for notfound? Deletes first from the running system, then from the
   * database.
   *
   * @param ctx
   */
  public void delete(RoutingContext ctx) {
    final String id = ctx.request().getParam("id");
    moduleManager.delete(id, sres -> {
      if (sres.failed()) {
        logger.error("delete (runtime) failed: " + sres.getType()
                + ":" + sres.cause().getMessage());
        responseError(ctx, sres.getType(), sres.cause());
      } else {
        moduleStore.delete(id, rres -> {
          if (rres.succeeded()) {
            sendReloadSignal(res -> {
              if (res.succeeded()) {
                responseText(ctx, 204).end();
              } else { // TODO - What can be done if sending signal fails?
                // Probably best to report failure of deleting the module
                // we can not really undelete it here.
                responseError(ctx, 500, res.cause());
              }
            });
          } else {
            responseError(ctx, rres.getType(), rres.cause());
          }
        });
      }
    });
  }

  public void reloadModules(RoutingContext ctx) {
    reloadModules(res -> {
      if (res.succeeded()) {
        responseText(ctx, 204).end();
      } else {
        responseError(ctx, res.getType(), res.cause());
      }
    });
  }

  public final void reloadModules(Handler<ExtendedAsyncResult<Void>> fut) {
    moduleManager.deleteAll(res -> {
      if (res.failed()) {
        logger.error("ReloadModules: Failed to delete all");
        fut.handle(res);
      } else {
        loadModules(fut);
      }
    });
  }

  public void loadModules(Handler<ExtendedAsyncResult<Void>> fut) {
    moduleStore.getAll(res -> {
      if (res.failed()) {
        fut.handle(new Failure<>(INTERNAL, res.cause()));
      } else {
        moduleManager.createList(res.result(), fut);
      }
    });
  }

} // class
