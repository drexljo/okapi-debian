## 1.2.5 2017-04-28

 * Okapi initdatabase hangs when pg db is unavailable OKAPI-255
 * Check dependencies before calling tenant interface OKAPI-277
 * Fix ModuleDescriptor handler wo method issues NPE OKAPI-308
 * Log proxy HTTP requests - with session tracking info OKAPI-311
 * Upgrade to Vert.x 3.4.1 OKAPI-313
 * initdatabase / purgedatabase updates and clarifications OKAPI-316

## 1.2.4 2017-04-20

 * Fix hang in recursive calls for parallel requests OKAPI-312 / FOLIO-516
 * Document PostrgreSQL init commands OKAPI-310

## 1.2.3 2017-04-07

 * Warn about deprecated features in ModuleDescriptors OKAPI-295
 * Inherit stderr again for deployed modules OKAPI-307

## 1.2.2 2017-04-04

 * Fix Okapi failure when invoking tenantPermissions with a v1.0
   ModuleDescriptor OKAPI-301
 * Fix tenantPermissions for the permission module itself OKAPI-304

## 1.2.1 2017-03-31

 * Fix Null diagnostic when Docker can not be reached OKAPI-299
 * Fix HTTP connection hangs after throwing error OKAPI-298
 * Fix DockerTest Unit Test may timeout OKAPI-297
 * Documentation: Link to API docs section and improve presentation
   of Instrumentation section

## 1.2.0 2017-03-24

 * Add ability to pull Docker images from a remote repository OKAPI-283
 * Allow Handlers/filters instead of routingEntries OKAPI-284
 * Allow phase instead of level inside routingEntries  OKAPI-284
 * Rewrite the Okapi guide examples OKAPI-286
 * Make RoutingEntry type optional and default to request-response OKAPI-288
 * Fix garbage character for Docker logging OKAPI-291
 * Increase wait time before warning in Unit Test for Docker OKAPI-294

## 1.1.0 2017-03-20

 * New property pathPattern which is an alternative to path in
   routingEntries/handlers/filters. OKAPI-274
 * routingEntries (handler) may be given for an interface OKAPI-269
   This is the preferred way of declaring handlers (that implement
   an interface). Filters, on the other hand, do not implement
   an interface and stays in the usual top-level (non-interface specific
   place).
 * Permission loading interface `/_/tenantPermissions` OKAPI-268
 * Define permission sets in ModuleDescriptor OKAPI-267
 * Cleaned up a few things issued by SonarQube OKAPI-279 OKAPI-280
 * Fix Okapi may hang due to standard error being buffered OKAPI-282

## 1.0.0 2017-02-27

 * Postgres storage option. Mongodb still supported
 * New commands: initdatabase and purgedatabase. Deprecated are
   properties -Dmongo_db_init and -Dpostgres_db_init
 * Docker support. Triggered in launchDescriptor with
   dockerImage property. It can be tuned further with properties
   dockerCMD and dockerArgs.
 * Tenant initialization: Okapi may call Module when it is
   associated with a module and when changing from one module to
   another (upgrade).
 * Everything else since 0.3

## 0.3 2016-05-03

 * Split of Okapi into three services: deployment, discovery and proxy
 * Hazelcast can be configured (including work with AWS)
 * Running mode must be given in command line: dev (for development)
   and cluster (for clustered mode)
 * Module version dependencies
 * Bug fixes and more tests

## 0.2 2016-03-23

 * RAML updates and verified in many tests
 * Persistent storage with MongoDB
 * Event Bus in use to synchronize a set of Okapi nodes
 * Using log4j as logger everywhere (using SLF4J to relay for all things)

## 0.1 2016-01-29

 * First code release
 * Offers simple gateway API and HTTP proxy functionality
 * On GitHub!!
 * Some documentation
 * Uses Vert.x for non-blocking services
 * Two dummy example modules - used by Unit tests

### TODO

 * Interface with Consul for Clustering, Service Discovery, etc.
 * Header merging (#33)


