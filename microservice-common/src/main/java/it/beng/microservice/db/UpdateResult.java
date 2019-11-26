package it.beng.microservice.db;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClientUpdateResult;

/**
 * <p>This class is a member of <strong>microservice-stack</strong> project.</p>
 *
 * @author vince
 */
public class UpdateResult {

  private static final String DOC_MATCHED = "docMatched";
  private static final String DOC_MODIFIED = "docModified";
  private static final String DOC_UPSERTED_ID = "docUpsertedId";
  public final long docMatched;
  public final long docModified;
  public final JsonObject docUpsertedId;

  UpdateResult(MongoClientUpdateResult mongoClientUpdateResult) {
    this.docMatched = mongoClientUpdateResult.getDocMatched();
    this.docModified = mongoClientUpdateResult.getDocModified();
    this.docUpsertedId = mongoClientUpdateResult.getDocUpsertedId();
  }

  public JsonObject toJson() {
    return new JsonObject().put(DOC_MATCHED, docMatched)
        .put(DOC_MODIFIED, docModified)
        .put(DOC_UPSERTED_ID, docUpsertedId);
  }
}
