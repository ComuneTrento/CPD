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
import it.beng.modeler.microservice.utils.ProcessEngineUtils;
import it.beng.modeler.model.Domain;
import it.beng.modeler.model.Domain.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;

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
          final RepositoryService repositoryService = cpd.processEngine().getRepositoryService();
          final RuntimeService runtimeService = cpd.processEngine().getRuntimeService();
          final TaskService taskService = cpd.processEngine().getTaskService();
          final List<JsonObject> tasks =
              (context == null || context.user() == null
                  // user is not logged in => keep tasks empty
                  ? Collections.<Task>emptyList()
                  // take all active tasks of processes that have diagramId as business key
                  : taskService.createTaskQuery()
                               .processInstanceBusinessKey(diagramId())
                               .active()
                               .list()
              ).stream()
               .map(task -> {
                 final HistoricProcessInstance process = ProcessEngineUtils
                     .getHistoricProcess(task.getProcessInstanceId(), true);
                 final String processKey = process.getProcessDefinitionKey();
                 final String taskKey = "notification-process".equals(processKey)
                     // notification-process is a callable activity => it uses "taskKey" variable
                     ? process.getProcessVariables().get("taskKey").toString()
                     : task.getTaskDefinitionKey();
                 // transform flowable task to partial JsonObject Task
                 // NOTE: language dependent fields ("name", "documentation" and "model")
                 //       will be added in a 2nd stage using the "extensions" mongodb collection
                 return new JsonObject()
                     .put("id", task.getId())
                     .put("processKey", processKey)
                     .put("taskKey", taskKey)
                     .put("businessKey", diagramId())
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
            // return an empty task list
            reply(new DefinitionLoadedAction(definition.put("tasks", new JsonArray())), handler);
          } else {
            // 2nd stage (for translations retrieval)
            final String languageCode = cpd.languageCode(context);
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
                            // if the model has outputs, override theyr values with "languageCode" translations
                            model.put("outputs", new JsonObject(
                                outputs.stream()
                                       .collect(Collectors.toMap(
                                           Map.Entry::getKey,
                                           entry -> DBUtils.languageCodeOrEN(
                                               (JsonObject) entry.getValue(), languageCode)))));
                          }
                          return extension
                              // translate name and documentation
                              .put("name", DBUtils.languageCodeOrEN(
                                  extension.getJsonObject("name"), languageCode))
                              .put("documentation", DBUtils.languageCodeOrEN(
                                  extension.getJsonObject("documentation"), languageCode))
                              .put("model", model);
                        })
                        .collect(Collectors.toList());
                    // add computed tasks to the definition
                    final DefinitionLoadedAction definitionLoadedAction = new DefinitionLoadedAction(
                        definition.put("tasks", new JsonArray(
                            tasks.stream()
                                 .map(task -> {
                                   JsonObject extension = extensions
                                       .stream()
                                       .filter(e -> e.getJsonObject("id")
                                                     .equals(
                                                         new JsonObject()
                                                             .put("processKey",
                                                                  task.getString("processKey"))
                                                             .put("taskKey",
                                                                  task.getString("taskKey"))))
                                       .findFirst()
                                       .orElse(null);
                                   if (extension == null) {
                                     logger.warn(
                                         "no extension found for task " + task.encodePrettily());
                                     return null;
                                   }
                                   return task
                                       .put("language", cpd.language(languageCode))
                                       .put("name", extension
                                           .getString("name"))
                                       .put("documentation", extension
                                           .getString("documentation"))
                                       .put("model", extension
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

  protected void reply(DefinitionLoadedAction action, AsyncHandler<JsonObject> handler) {
    mongodb.find(Collection.USER_FEEDBACKS, new JsonObject(), findFeedbacks -> {
      if (findFeedbacks.succeeded()) {
        action.definition().put("feedback", new JsonArray(findFeedbacks.result()));
        super.reply(action, handler);
      } else { handler.handle(Future.failedFuture(findFeedbacks.cause())); }
    });
  }

}
