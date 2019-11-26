package it.beng.microservice.db;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.ext.mongo.WriteOption;
import it.beng.microservice.common.AsyncHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * This class is a member of <strong>modeler-microservice</strong> project.
 * </p>
 *
 * @author vince
 */
public class MongoDB {

  private static final Logger logger = LogManager.getLogger(MongoDB.class);

  private static final Map<String, Map<String, String>> COMMANDS = new HashMap<>();

  private final Vertx vertx;
  private final MongoClient client;
  private final String commandPath;
  private final JsonMapper mapper;

  private MongoDB(Vertx vertx, MongoClient mongoClient, String commandPath,
                  Map<String, String> mappings) {
    this.vertx = vertx;
    this.client = mongoClient;
    this.commandPath = commandPath != null ? commandPath : "./";
    this.mapper = new JsonMapper(mappings);
  }

  public static MongoDB create(Vertx vertx, MongoClient mongoClient, String commandPath,
                               Map<String, String> mappings) {
    return new MongoDB(vertx, mongoClient, commandPath, mappings);
  }

  public void close() {
    client.close();
  }

  /**
   * implementation
   */

  public JsonMapper getMapper() {
    return this.mapper;
  }

  private AsyncResult<JsonObject> objectDecoderHandler(AsyncResult<JsonObject> h) {
    if (h.succeeded()) {
      JsonObject result = mapper.decode(h.result());
      logger.debug("decoded: " + (result != null ? result.encodePrettily() : "null"));
      return Future.succeededFuture(result);
    }
    return Future.failedFuture(h.cause());
  }

  private AsyncResult<List<JsonObject>> listDecoderHandler(AsyncResult<List<JsonObject>> h) {
    if (h.succeeded()) {
      List<JsonObject> result = mapper.decode(h.result());
      logger
          .debug("decoded: " + (result != null ? new JsonArray(result).encodePrettily() : "null"));
      return Future.succeededFuture(result);
    }
    return Future.failedFuture(h.cause());
  }

  /**
   * Get a list of all collections in the database.
   *
   * @param resultHandler will be called with a list of collections.
   */
  @Fluent
  public MongoDB listCollections(AsyncHandler<List<String>> resultHandler) {
    client.getCollections(h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Create a new collection
   *
   * @param collectionName the name of the collection
   * @param resultHandler  will be called when complete
   */
  @Fluent
  public MongoDB createCollection(String collectionName, AsyncHandler<Void> resultHandler) {
    client.createCollection(collectionName, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Drop a collection
   *
   * @param collectionName the collection
   * @param resultHandler  will be called when complete
   */
  @Fluent
  public MongoDB dropCollection(String collectionName, AsyncHandler<Void> resultHandler) {
    client.dropCollection(collectionName, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Get all the indexes in this collection.
   *
   * @param collection    the collection
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB listIndexes(String collection, AsyncHandler<JsonArray> resultHandler) {
    client.listIndexes(collection, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Creates an index.
   *
   * @param collection    the collection
   * @param key           A document that contains the field and value pairs where the field is the
   *                      index key and the value describes the type of index for that field. For an
   *                      ascending index on a field, specify a value of 1; for descending index,
   *                      specify a value of -1.
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB createIndex(String collection, JsonObject key, AsyncHandler<Void> resultHandler) {
    client.createIndex(collection, key, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Creates an index.
   *
   * @param collection    the collection
   * @param key           A document that contains the field and value pairs where the field is the
   *                      index key and the value describes the type of index for that field. For an
   *                      ascending index on a field, specify a value of 1; for descending index,
   *                      specify a value of -1.
   * @param indexOptions  the options for the index
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB createIndexWithOptions(String collection, JsonObject key,
                                        IndexOptions indexOptions,
                                        AsyncHandler<Void> resultHandler) {
    client.createIndexWithOptions(collection, key, indexOptions, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Drops the index given its name.
   *
   * @param collection    the collection
   * @param indexName     the name of the index to remove
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB dropIndex(String collection, String indexName, AsyncHandler<Void> resultHandler) {
    client.dropIndex(collection, indexName, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Count matching documents in a collection.
   *
   * @param collection    the collection
   * @param query         query used to match documents
   * @param resultHandler will be provided with the number of matching documents
   */
  @Fluent
  public MongoDB count(String collection, JsonObject query, AsyncHandler<Long> resultHandler) {
    client.count(collection, mapper.encode(query), h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(h.result()));
      } else {
        resultHandler.handle(Future.failedFuture(h.cause()));
      }
    });
    return this;
  }

  /**
   * Find matching documents in the specified collection
   *
   * @param collection    the collection
   * @param query         query used to match documents
   * @param resultHandler will be provided with list of documents
   */
  @Fluent
  public MongoDB find(String collection, JsonObject query,
                      AsyncHandler<List<JsonObject>> resultHandler) {
    client.find(collection, mapper.encode(query), h -> resultHandler.handle(listDecoderHandler(h)));
    return this;
  }

  /**
   * Find matching documents in the specified collection. This method use batchCursor for returning
   * each found document.
   *
   * @param collection the collection
   * @param query      query used to match documents
   */
  @Fluent
  public ReadStream<JsonObject> findBatch(String collection, JsonObject query) {
    return client.findBatch(collection, mapper.encode(query));
  }

  /**
   * Find matching documents in the specified collection, specifying options. This method use
   * batchCursor for returning each found document.
   *
   * @param collection  the collection
   * @param query       query used to match documents
   * @param findOptions options to configure the find
   */
  @Fluent
  public ReadStream<JsonObject> findBatchWithOptions(String collection, JsonObject query,
                                                     FindOptions findOptions) {
    findOptions.setFields(mapper.encode(findOptions.getFields()))
               .setSort(mapper.encode(findOptions.getSort()));
    return client.findBatchWithOptions(collection,
                                       mapper.encode(query),
                                       findOptions);
  }

  /**
   * Find a single matching document in the specified collection
   * <p>
   * This operation might change <i>_id</i> field of <i>query</i> parameter
   *
   * @param collection    the collection
   * @param query         the query used to match the document
   * @param fields        the fields
   * @param resultHandler will be provided with the document, if any
   */
  @Fluent
  public MongoDB findOne(String collection, JsonObject query, JsonObject fields,
                         AsyncHandler<JsonObject> resultHandler) {
    client.findOne(collection,
                   mapper.encode(query),
                   mapper.encode(fields),
                   h -> resultHandler.handle(objectDecoderHandler(h)));
    return this;
  }

  /**
   * Find a single matching document in the specified collection and delete it.
   * <p>
   * This operation might change <i>_id</i> field of <i>query</i> parameter
   *
   * @param collection    the collection
   * @param query         the query used to match the document
   * @param resultHandler will be provided with the deleted document, if any
   */
  @Fluent
  public MongoDB findOneAndDelete(String collection, JsonObject query,
                                  AsyncHandler<JsonObject> resultHandler) {
    client.findOneAndDelete(collection, mapper.encode(query),
                            h -> resultHandler.handle(objectDecoderHandler(h)));
    return this;
  }

  /**
   * Find a single matching document in the specified collection and delete it.
   * <p>
   * This operation might change <i>_id</i> field of <i>query</i> parameter
   *
   * @param collection    the collection
   * @param query         the query used to match the document
   * @param findOptions   options to configure the find
   * @param resultHandler will be provided with the deleted document, if any
   */
  @Fluent
  public MongoDB findOneAndDeleteWithOptions(String collection, JsonObject query,
                                             FindOptions findOptions,
                                             AsyncHandler<JsonObject> resultHandler) {
    findOptions.setFields(mapper.encode(findOptions.getFields()))
               .setSort(mapper.encode(findOptions.getSort()));
    client.findOneAndDeleteWithOptions(collection,
                                       mapper.encode(query),
                                       findOptions,
                                       h -> resultHandler.handle(objectDecoderHandler(h)));
    return this;
  }

  /**
   * Find a single matching document in the specified collection and replace it.
   * <p>
   * This operation might change <i>_id</i> field of <i>query</i> parameter
   *
   * @param collection    the collection
   * @param query         the query used to match the document
   * @param replace       the replacement document
   * @param resultHandler will be provided with the document, if any
   */
  @Fluent
  public MongoDB findOneAndReplace(String collection, JsonObject query, JsonObject replace,
                                   AsyncHandler<JsonObject> resultHandler) {
    client.findOneAndReplace(collection,
                             mapper.encode(query),
                             mapper.encode(replace),
                             h -> resultHandler
                                 .handle(objectDecoderHandler(h)));
    return this;
  }

  /**
   * Find a single matching document in the specified collection and replace it.
   * <p>
   * This operation might change <i>_id</i> field of <i>query</i> parameter
   *
   * @param collection    the collection
   * @param query         the query used to match the document
   * @param replace       the replacement document
   * @param findOptions   options to configure the find
   * @param updateOptions options to configure the update
   * @param resultHandler will be provided with the document, if any
   */
  @Fluent
  public MongoDB findOneAndReplaceWithOptions(String collection, JsonObject query,
                                              JsonObject replace,
                                              FindOptions findOptions, UpdateOptions updateOptions,
                                              AsyncHandler<JsonObject> resultHandler) {
    findOptions.setFields(mapper.encode(findOptions.getFields()))
               .setSort(mapper.encode(findOptions.getSort()));
    client.findOneAndReplaceWithOptions(collection,
                                        mapper.encode(query),
                                        mapper.encode(replace),
                                        findOptions,
                                        updateOptions,
                                        h -> resultHandler.handle(objectDecoderHandler(h)));
    return this;
  }

  /**
   * Find a single matching document in the specified collection and update it.
   * <p>
   * This operation might change <i>_id</i> field of <i>query</i> parameter
   *
   * @param collection    the collection
   * @param query         the query used to match the document
   * @param update        used to describe how the documents will be updated
   * @param resultHandler will be provided with the document that was updated before the update was
   *                      applied. If no documents matched the query filter, then null will be
   *                      returned
   */
  @Fluent
  public MongoDB findOneAndUpdate(String collection, JsonObject query, JsonObject update,
                                  AsyncHandler<JsonObject> resultHandler) {
    client.findOneAndUpdate(
        collection,
        mapper.encode(query),
        mapper.encode(update),
        h -> resultHandler.handle(objectDecoderHandler(h)));
    return this;
  }

  @Fluent
  public MongoDB findOneAndUpdateWithOptions(String collection, JsonObject query, JsonObject update,
                                             FindOptions findOptions, UpdateOptions updateOptions,
                                             AsyncHandler<JsonObject> resultHandler) {
    findOptions.setFields(mapper.encode(findOptions.getFields()))
               .setSort(mapper.encode(findOptions.getSort()));
    client.findOneAndUpdateWithOptions(
        collection,
        mapper.encode(query),
        mapper.encode(update),
        findOptions,
        updateOptions,
        h -> resultHandler.handle(objectDecoderHandler(h)));
    return this;
  }

  /**
   * Find matching documents in the specified collection, specifying options
   *
   * @param collection    the collection
   * @param query         query used to match documents
   * @param findOptions   options to configure the find
   * @param resultHandler will be provided with list of documents
   */
  @Fluent
  public MongoDB findWithOptions(String collection, JsonObject query, FindOptions findOptions,
                                 AsyncHandler<List<JsonObject>> resultHandler) {
    findOptions.setFields(mapper.encode(findOptions.getFields()))
               .setSort(mapper.encode(findOptions.getSort()));
    client.findWithOptions(collection,
                           mapper.encode(query),
                           findOptions,
                           h -> resultHandler
                               .handle(listDecoderHandler(h)));
    return this;
  }

  /**
   * Insert a document in the specified collection
   * <p>
   * This operation might change <i>_id</i> field of <i>document</i> parameter
   *
   * @param collection    the collection
   * @param document      the document
   * @param resultHandler result handler will be provided with the inserted document
   */
  @Fluent
  public MongoDB insert(String collection, JsonObject document,
                        AsyncHandler<JsonObject> resultHandler) {
    String idField = mapper.inverse().get("_id");
    if (idField == null) {
      resultHandler.handle(Future.failedFuture("no mapping for 'id' field"));
    } else {
      client.insert(collection, mapper.encode(document), h -> {
        if (h.succeeded()) {
          if (document.getValue(idField) == null) {
            document.put(idField, h.result());
          }
          resultHandler.handle(Future.succeededFuture(document));
        } else { resultHandler.handle(Future.failedFuture(h.cause())); }
      });
    }
    return this;
  }

  /**
   * Insert a document in the specified collection with the specified write option
   * <p>
   * This operation might change <i>_id</i> field of <i>document</i> parameter
   *
   * @param collection    the collection
   * @param document      the document
   * @param writeOption   the write option to use
   * @param resultHandler result handler will be provided with the inserted document
   */
  @Fluent
  public MongoDB insertWithOptions(String collection, JsonObject document, WriteOption writeOption,
                                   AsyncHandler<JsonObject> resultHandler) {
    String idField = mapper.inverse().get("_id");
    if (idField == null) {
      resultHandler.handle(Future.failedFuture("no mapping for 'id' field"));
    } else {
      client.insertWithOptions(collection, mapper.encode(document), writeOption, h -> {
        if (h.succeeded()) {
          if (document.getValue(idField) == null) {
            document.put(idField, h.result());
          }
          resultHandler.handle(Future.succeededFuture(document));
        } else { resultHandler.handle(Future.failedFuture(h.cause())); }
      });
    }
    return this;
  }

  /**
   * Save a document in the specified collection
   * <p>
   * This operation might change <i>_id</i> field of <i>document</i> parameter
   *
   * @param collection    the collection
   * @param document      the document
   * @param resultHandler result handler will be provided with the saved document
   */
  @Fluent
  public MongoDB save(String collection, JsonObject document,
                      AsyncHandler<JsonObject> resultHandler) {
    // TODO: check language field changing from 'italian' to 'it'
    String idField = mapper.inverse().get("_id");
    if (idField == null) {
      resultHandler.handle(Future.failedFuture("no mapping for 'id' field"));
    } else {
      client.save(collection, mapper.encode(document), h -> {
        if (h.succeeded()) {
          if (document.getValue(idField) == null) {
            document.put(idField, h.result());
          }
          resultHandler.handle(Future.succeededFuture(document));
        } else { resultHandler.handle(Future.failedFuture(h.cause())); }
      });
    }
    return this;
  }

  /**
   * Save a document in the specified collection with the specified write option
   * <p>
   * This operation might change <i>_id</i> field of <i>document</i> parameter
   *
   * @param collection    the collection
   * @param document      the document
   * @param writeOption   the write option to use
   * @param resultHandler result handler will be provided with the saved document
   */
  @Fluent
  public MongoDB saveWithOptions(String collection, JsonObject document, WriteOption writeOption,
                                 AsyncHandler<JsonObject> resultHandler) {
    String idField = mapper.inverse().get("_id");
    if (idField == null) {
      resultHandler.handle(Future.failedFuture("no mapping for 'id' field"));
    } else {
      client.saveWithOptions(collection, mapper.encode(document), writeOption, h -> {
        if (h.succeeded()) {
          if (document.getValue(idField) == null) {
            document.put(idField, h.result());
          }
          resultHandler.handle(Future.succeededFuture(document));
        } else { resultHandler.handle(Future.failedFuture(h.cause())); }
      });
    }
    return this;
  }

  /**
   * Update matching documents in the specified collection and return the handler with
   * MongoClientUpdateResult result
   *
   * @param collection    the collection
   * @param query         query used to match the documents
   * @param update        used to describe how the documents will be updated
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB updateCollection(String collection, JsonObject query, JsonObject update,
                                  AsyncHandler<UpdateResult> resultHandler) {
    client.updateCollection(collection, mapper.encode(query), mapper.encode(update), h -> {
      if (h.succeeded()) {
        UpdateResult result = new UpdateResult(h.result());
        resultHandler.handle(Future.succeededFuture(result));
      } else { resultHandler.handle(Future.failedFuture(h.cause())); }
    });
    return this;
  }

  /**
   * Update matching documents in the specified collection, specifying options and return the
   * handler with MongoClientUpdateResult result
   *
   * @param collection    the collection
   * @param query         query used to match the documents
   * @param update        used to describe how the documents will be updated
   * @param updateOptions options to configure the update
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB updateCollectionWithOptions(String collection, JsonObject query, JsonObject update,
                                             UpdateOptions updateOptions,
                                             AsyncHandler<UpdateResult> resultHandler) {
    client.updateCollectionWithOptions(collection,
                                       mapper.encode(query),
                                       mapper
                                           .encode(update),
                                       updateOptions,
                                       h -> {
                                         if (h.succeeded()) {
                                           resultHandler.handle(Future.succeededFuture(
                                               new UpdateResult(h.result())));
                                         } else {
                                           resultHandler.handle(Future.failedFuture(h.cause()));
                                         }
                                       });
    return this;
  }

  /**
   * Replace matching documents in the specified collection and return the handler with
   * MongoClientUpdateResult result
   *
   * @param collection    the collection
   * @param query         query used to match the documents
   * @param replace       all matching documents will be replaced with this
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB replaceDocuments(String collection, JsonObject query, JsonObject replace,
                                  AsyncHandler<UpdateResult> resultHandler) {
    client.replaceDocuments(collection, mapper.encode(query), mapper.encode(replace), h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(new UpdateResult(h.result())));
      } else { resultHandler.handle(Future.failedFuture(h.cause())); }
    });
    return this;
  }

  /**
   * Replace matching documents in the specified collection, specifying options and return the
   * handler with MongoClientUpdateResult result
   *
   * @param collection    the collection
   * @param query         query used to match the documents
   * @param replace       all matching documents will be replaced with this
   * @param updateOptions options to configure the replace
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB replaceDocumentsWithOptions(String collection, JsonObject query,
                                             JsonObject replace,
                                             UpdateOptions updateOptions,
                                             AsyncHandler<UpdateResult> resultHandler) {
    client.replaceDocumentsWithOptions(collection,
                                       mapper.encode(query),
                                       mapper
                                           .encode(replace),
                                       updateOptions,
                                       h -> {
                                         if (h.succeeded()) {
                                           resultHandler.handle(Future.succeededFuture(
                                               new UpdateResult(h.result())));
                                         } else {
                                           resultHandler.handle(Future.failedFuture(h.cause()));
                                         }
                                       });
    return this;
  }

  /**
   * Remove a single matching document from a collection and return the handler with
   * MongoClientDeleteResult result
   *
   * @param collection    the collection
   * @param query         query used to match document
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB removeDocument(String collection, JsonObject query,
                                AsyncHandler<DeleteResult> resultHandler) {
    client.removeDocument(collection, mapper.encode(query), h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(new DeleteResult(collection, h.result())));
      } else { resultHandler.handle(Future.failedFuture(h.cause())); }
    });
    return this;
  }

  /**
   * Remove a single matching document from a collection with the specified write option and return
   * the handler with MongoClientDeleteResult result
   *
   * @param collection    the collection
   * @param query         query used to match document
   * @param writeOption   the write option to use
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB removeDocumentWithOptions(String collection, JsonObject query,
                                           WriteOption writeOption,
                                           AsyncHandler<DeleteResult> resultHandler) {
    client.removeDocumentWithOptions(collection, mapper.encode(query), writeOption, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(new DeleteResult(collection, h.result())));
      } else { resultHandler.handle(Future.failedFuture(h.cause())); }
    });
    return this;
  }

  /**
   * Remove matching documents from a collection and return the handler with MongoClientDeleteResult
   * result
   *
   * @param collection    the collection
   * @param query         query used to match documents
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB removeDocuments(String collection, JsonObject query,
                                 AsyncHandler<DeleteResult> resultHandler) {
    client.removeDocuments(collection, mapper.encode(query), h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(new DeleteResult(collection, h.result())));
      } else { resultHandler.handle(Future.failedFuture(h.cause())); }
    });
    return this;
  }

  /**
   * Remove matching documents from a collection with the specified write option and return the
   * handler with MongoClientDeleteResult result
   *
   * @param collection    the collection
   * @param query         query used to match documents
   * @param writeOption   the write option to use
   * @param resultHandler will be called when complete
   */
  @Fluent
  public MongoDB removeDocumentsWithOptions(String collection, JsonObject query,
                                            WriteOption writeOption,
                                            AsyncHandler<DeleteResult> resultHandler) {
    client.removeDocumentsWithOptions(collection, mapper.encode(query), writeOption, h -> {
      if (h.succeeded()) {
        resultHandler.handle(Future.succeededFuture(new DeleteResult(collection, h.result())));
      } else { resultHandler.handle(Future.failedFuture(h.cause())); }
    });
    return this;
  }

  /**
   * Gets the distinct values of the specified field name. Return a JsonArray containing distinct
   * values (eg: [ 1 , 89 ])
   *
   * @param collection      the collection
   * @param fieldName       the field name
   * @param resultClassname the class name for the field values
   * @param resultHandler   will be provided with array of values.
   */
  @Fluent
  public MongoDB distinct(String collection, String fieldName, String resultClassname,
                          AsyncHandler<JsonArray> resultHandler) {
    client.distinct(collection,
                    fieldName,
                    resultClassname,
                    h -> resultHandler.handle(Future.succeededFuture(h.result())));
    return this;
  }

  /**
   * Gets the distinct values of the specified field name. This method use batchCursor for returning
   * each found value. Each value is a json fragment with fieldName key (eg: {"num": 1}).
   *
   * @param collection      the collection
   * @param fieldName       the field name
   * @param resultClassname the class name for the field values
   */
  public ReadStream<JsonObject> distinctBatch(String collection, String fieldName,
                                              String resultClassname) {
    return client.distinctBatch(collection,
                                fieldName,
                                resultClassname);
  }

  /* MONGODB COMMANDS */

  public Command command(String key, Map<String, String> params) {
    return this.new Command(key, params);
  }

  public Command command(String key) {
    return this.new Command(key, null);
  }

  /**
   * Run an arbitrary MongoDB command.
   *
   * @param commandName   the name of the command
   * @param command       the command
   * @param resultHandler will be called with the result.
   */
  @Fluent
  public MongoDB runCommand(String commandName, Command command,
                            AsyncHandler<JsonObject> resultHandler) {
    command.json(commandJsonHandler -> {
      if (commandJsonHandler.succeeded()) {
        client.runCommand(commandName,
                          commandJsonHandler.result(),
                          h -> resultHandler.handle(objectDecoderHandler(h)));
      } else { resultHandler.handle(Future.failedFuture(commandJsonHandler.cause())); }
    });
    return this;
  }

  public final class Command {

    public final String key;
    private final Map<String, String> params;

    private Command(String key, Map<String, String> params) {
      this.key = key;
      this.params = params;
    }

    private void doJson(String json, AsyncHandler<JsonObject> handler) {
      for (Map.Entry<String, String> param : params.entrySet()) {
        String key = param.getKey();
        String value = param.getValue();
        json = json.replace("{" + key + "}", value);
      }
      JsonObject command = new JsonObject(json);
      logger.debug(key + ": " + command.encodePrettily());
      handler.handle(Future.succeededFuture(command));
    }

    public void json(AsyncHandler<JsonObject> handler) {
      Map<String, String> fileCommands = COMMANDS.get(commandPath);
      if (fileCommands != null) {
        String json = fileCommands.get(key);
        if (json != null) {
          doJson(json, handler);
          return;
        }
      } else {
        COMMANDS.put(commandPath, new HashMap<>());
      }
      final String commandFile = commandPath + key + ".json";
      vertx.fileSystem().readFile(commandFile, fileHandler -> {
        if (fileHandler.succeeded()) {
          String json = fileHandler.result().toString(StandardCharsets.UTF_8)
                                   .replaceAll("( |\n)", "");
          COMMANDS.get(commandPath).put(key, json);
          doJson(json, handler);
        } else {
          handler.handle(Future.failedFuture(fileHandler.cause()));
        }
      });
    }

  }

}
