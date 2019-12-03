package it.beng.modeler.microservice.actions.diagram.send;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import it.beng.microservice.common.AsyncHandler;
import it.beng.microservice.db.MongoDB;
import it.beng.modeler.config.cpd;
import it.beng.modeler.microservice.actions.SendAction;
import it.beng.modeler.microservice.actions.diagram.DiagramAction;
import it.beng.modeler.microservice.actions.diagram.reply.DefinitionLoadedAction;
import it.beng.modeler.microservice.utils.DBUtils;
import it.beng.modeler.microservice.utils.JsonUtils;
import it.beng.modeler.model.Domain;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;

public class LoadDefinitionAction extends SendAction implements DiagramAction {

  private static final Logger logger = LogManager.getLogger(LoadDefinitionAction.class);

  public static final String TYPE = "[Diagram Action Send] Load Definition";

  public LoadDefinitionAction(JsonObject action) {
    super(action);
  }

  @Override
  protected String innerType() {
    return TYPE;
  }

  @Override
  public boolean isValid() {
    return super.isValid() && diagramId() != null;
  }

  public String diagramId() {
    return json.getString("diagramId");
  }

  @Override
  public void handle(RoutingContext context, AsyncHandler<JsonObject> handler) {
    MongoDB.Command command =
        mongodb.command(
            COMMAND_PATH + "getDiagramDefinition",
            new HashMap<String, String>() {
              {
                put("diagramId", diagramId());
              }
            });

    mongodb.runCommand("aggregate", command, getDiagramDefinition -> {
      if (getDiagramDefinition.succeeded()) {
        final JsonObject definition =
            JsonUtils.firstOrNull(getDiagramDefinition.result().getJsonArray("result"));
        if (definition != null) {
          final ProcessDefinitionQuery processDefinitionQuery =
              cpd.processEngine().getRepositoryService().createProcessDefinitionQuery();
          final TaskQuery taskQuery = cpd.processEngine().getTaskService().createTaskQuery();
          final List<JsonObject> tasks =
              (context == null || context.user() == null
               // user is not logged in => keep tasks empty
               ? Collections.<Task>emptyList()
               // take all active tasks of processes that have diagramId as business key
               : taskQuery.processInstanceBusinessKey(diagramId()).active().list()
              ).stream()
               .map(task -> {
                 String processKey = processDefinitionQuery
                     .processDefinitionId(task.getProcessDefinitionId())
                     .singleResult()
                     .getKey();
                 final String taskKey = task.getTaskDefinitionKey();
                 // transform flowable task to partial JsonObject Task
                 // NOTE: language dependent fields ("name", "documentation" and "model")
                 //       will be added in a 2nd stage using the "extensions" mongodb collection
                 return new JsonObject()
                     .put("processKey", processKey)
                     .put("taskKey", taskKey)
                     .put("id", task.getId())
                     .put("processId", task.getProcessInstanceId())
                     .put("assignee", task.getAssignee())
                     .put("createTime", DBUtils.mongoDateTime(
                         DBUtils.parseDateTime(
                             task.getCreateTime().toInstant().toString())))
                     // TODO: create Collaboration.Process and Collaboration.Process.Task schemas
                     .put("$domain", "Model.Thing");
               })
               .collect(Collectors.toList());
          if (tasks.isEmpty()) {
            reply(
                // return an empty task list
                new DefinitionLoadedAction(definition.put("tasks", new JsonArray())), handler);
          } else {
            // 2nd stage (for translations retrieval)
            final String lang = cpd.languageCode(context);
            DBUtils.loadCollection(
                Domain.Collection.EXTENSIONS,
                new JsonObject()
                    // filter "extensions" collection by process-task key of fetched tasks
                    .put("id", new JsonObject()
                        .put("$in", new JsonArray(
                            tasks.stream()
                                 .map(task -> new JsonObject()
                                     .put("processKey", task.getString("processKey"))
                                     .put("taskKey", task.getString("taskKey")))
                                 .collect(Collectors.toList())))), loadExtensions -> {
                  if (loadExtensions.succeeded()) {
                    final List<JsonObject> extensions = loadExtensions
                        .result()
                        .stream()
                        .map(extension -> {
                          JsonObject model = extension.getJsonObject("model");
                          JsonObject outputs = model.getJsonObject("outputs");
                          if (outputs != null) {
                            // if the model has outputs, override theyr values with "lang" translations
                            model.put("outputs", new JsonObject(
                                outputs.stream()
                                       .collect(Collectors.toMap(
                                           Map.Entry::getKey,
                                           entry -> DBUtils
                                               .langOrEN((JsonObject) entry.getValue(), lang)))));
                          }
                          return extension
                              // translate name and documentation
                              .put("name", DBUtils.langOrEN(extension.getJsonObject("name"), lang))
                              .put("documentation",
                                   DBUtils.langOrEN(extension.getJsonObject("documentation"), lang))
                              .put("model", model);
                        })
                        .collect(Collectors.toList());
                    // add computed tasks to the definition
                    final DefinitionLoadedAction definitionLoadedAction = new DefinitionLoadedAction(
                        definition.put("tasks", new JsonArray(
                            tasks.stream()
                                 .map(task -> {
                                   JsonObject foundExtension = extensions
                                       .stream()
                                       .filter(extension -> extension
                                           .getJsonObject("id")
                                           .equals(new JsonObject()
                                                       .put("processKey",
                                                            task.getString("processKey"))
                                                       .put("taskKey", task.getString("taskKey"))))
                                       .findFirst()
                                       .orElse(null);
                                   if (foundExtension == null) {
                                     logger.error(
                                         "no extension found for task " + task.encodePrettily());
                                     return null;
                                   }
                                   return task
                                       .put("name", foundExtension
                                           .getString("name"))
                                       .put("documentation", foundExtension
                                           .getString("documentation"))
                                       .put("model", foundExtension
                                           .getJsonObject("model"));
                                 })
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList()))));
                    reply(definitionLoadedAction, handler);
                  } else { handler.handle(Future.failedFuture(loadExtensions.cause())); }
                });
          }
        } else { handler.handle(Future.failedFuture("definition not found")); }
      } else { handler.handle(Future.failedFuture(getDiagramDefinition.cause())); }
    });
  }
}
