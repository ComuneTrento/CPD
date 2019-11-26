package it.beng.microservice.db;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientDeleteResult;

/**
 * <p>This class is a member of <strong>microservice-stack</strong> project.</p>
 *
 * @author vince
 */
public class DeleteResult {

  private static final String COLLECTION = "collection";
  private static final String REMOVED_COUNT = "removedCount";
  public final String collection;
  public final long removedCount;

  public DeleteResult(String collection, long removedCount) {
    this.collection = collection;
    this.removedCount = removedCount;
  }

  DeleteResult(String collection, MongoClientDeleteResult mongoClientDeleteResult) {
    this(collection, mongoClientDeleteResult.getRemovedCount());
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put(COLLECTION, collection)
        .put(REMOVED_COUNT, removedCount);
  }
}
