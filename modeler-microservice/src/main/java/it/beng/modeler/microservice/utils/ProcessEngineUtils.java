package it.beng.modeler.microservice.utils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import it.beng.microservice.common.AsyncHandler;
import it.beng.modeler.config.cpd;
import it.beng.modeler.model.Domain;
import it.beng.modeler.processengine.ProcessDefinitions;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * This class is a member of <strong>modeler-microservice</strong> project.
 *
 * @author vince
 */
public final class ProcessEngineUtils {

  private static final Logger logger = LogManager.getLogger(ProcessEngineUtils.class);

  public static List<String> getActiveCollaborationIds() {
    return cpd.processEngine().getRuntimeService().createProcessInstanceQuery().active().list()
              .stream()
              .map(ProcessInstance::getBusinessKey)
              .collect(Collectors.toList());
  }

  public static List<JsonObject> getCollaborationsTeam(String collaborationId) {
    return cpd.processEngine().getRuntimeService().createProcessInstanceQuery().active()
              .processInstanceBusinessKey(collaborationId).list().stream()
              .map(ProcessInstance::getProcessVariables)
              .map(
                  vars ->
                      new JsonObject()
                          .put("owner", vars.get("owner"))
                          .put("reviewer", vars.get("reviewer"))
                          .put("editor", vars.get("editor"))
                          .put("observer", vars.get("observer")))
              .collect(Collectors.toList());
  }

  public static ProcessInstance startCollaboration(String collaborationId, JsonObject team) {
    return cpd.processEngine()
              .getRuntimeService()
              .startProcessInstanceByKey(
                  ProcessDefinitions.COLLABORATION_PROCESS_KEY,
                  collaborationId,
                  new HashMap<String, Object>() {
                    {
                      put("owner", team.getJsonArray("owner").getString(0));
                      put("reviewer", team.getJsonArray("reviewer").getString(0));
                      put("editor", team.getJsonArray("editor").getString(0));
                      // observer isn't mandatory
                      if (team.getJsonArray("observer") != null
                          && team.getJsonArray("observer").size() > 0) {
                        put("observer", team.getJsonArray("observer").getString(0));
                      }
                    }
                  });
  }

  public static void update(JsonObject update, final AsyncHandler<Void> complete) {
    final JsonObject changes = update.getJsonObject("changes");
    if (changes.isEmpty()) {
      complete.handle(Future.succeededFuture());
      return;
    }
    final JsonObject original = update.getJsonObject("original");
    final ProcessEngine processEngine = cpd.processEngine();
    switch (original.getString("$domain")) {
      case "Model.FPMN.Diagram":
        final JsonObject newTeam = changes.getJsonObject("team");
        if (newTeam != null) {
          logger.debug("TEAM CHANGED: " + newTeam.encodePrettily());
          final String businessKey = original.getString("id");
          newTeam.forEach(entry -> {
            final String role = entry.getKey();
            final JsonArray newUserIds = ((JsonArray) entry.getValue());
            final String newUserId = newUserIds.size() > 0 ? newUserIds.getString(0) : null;
            // (1) set changed roles as process variables
            logger.debug("changing variable of active process: {" + role + ": " + newUserId + "}");
            final RuntimeService runtimeService = processEngine.getRuntimeService();
            runtimeService
                .createExecutionQuery()
                .processInstanceBusinessKey(businessKey)
                .list()
                .forEach(execution -> {
                  Object oldUserId = runtimeService.getVariable(execution.getId(), role);
                  if (!Objects.equals(oldUserId, newUserId)) {
                    logger.debug("writing " + role + " = " + newUserId
                                     + " in execution " + execution.getId());
                    runtimeService.setVariable(execution.getId(), role, newUserId);
                  }
                });
            // (2) change active tasks assignee by role
            // NOTE: assignee role is stored in the mongodb "extensions" collection
            final TaskService taskService = processEngine.getTaskService();
            DBUtils.loadCollection(
                Domain.Collection.EXTENSIONS,
                // query example: { "model.roles": { $in: [ "editor" ] } }
                new JsonObject()
                    .put("model.roles", new JsonObject()
                        .put("$all", new JsonArray().add(role))
                    ), extensions -> {
                  if (extensions.succeeded()) {
                    extensions.result().forEach(extension -> {
                      final JsonObject id = extension.getJsonObject("id");
                      final String processDefinitionKey = id.getString("processKey");
                      final String taskDefinitionKey = id.getString("taskKey");
                      taskService
                          .createTaskQuery()
                          .active()
                          .processInstanceBusinessKey(businessKey)
                          .processDefinitionKey(processDefinitionKey)
                          .taskDefinitionKey(taskDefinitionKey)
                          .list()
                          .forEach(task -> {
                            logger.debug(
                                "changing assignee of «" + extension.getJsonObject("name")
                                                                    .getString("en") + "»: "
                                    + task.getAssignee()
                                    + " => "
                                    + newUserId);
                            taskService.setAssignee(task.getId(), newUserId);
                          });
                    });
                    complete.handle(Future.succeededFuture());
                  } else {
                    complete.handle(Future.failedFuture(extensions.cause()));
                  }
                });
          });
        } else { complete.handle(Future.succeededFuture()); }
        break;
      default:
        complete.handle(Future.succeededFuture());
    }
  }

  public static void deleteCollaboration(String collaborationId) {
    RuntimeService runtimeService = cpd.processEngine().getRuntimeService();
    runtimeService
        .createProcessInstanceQuery()
        .processInstanceBusinessKey(collaborationId)
        .list()
        .forEach(
            processInstance -> {
              logger.debug("deleting process " + processInstance.getId() + "...");
              runtimeService.deleteProcessInstance(processInstance.getId(), "removed by admin");
              logger.debug("...process " + processInstance.getId() + " deleted!");
            });
  }

  public static ProcessInstance getRootProcess(String executionId) {
    RuntimeService runtimeService = cpd.processEngine().getRuntimeService();
    Execution execution = runtimeService.createExecutionQuery()
                                        .executionId(executionId)
                                        .singleResult();
    if (execution != null) {
      return runtimeService.createProcessInstanceQuery()
                           .processInstanceId(execution.getRootProcessInstanceId())
                           .singleResult();
    }
    return null;
  }

  public static HistoricProcessInstance getHistoricProcess(String processId,
                                                           boolean includeProcessVariables) {
    if (processId != null) {
      HistoricProcessInstanceQuery processQuery = cpd.processEngine().getHistoryService()
                                                     .createHistoricProcessInstanceQuery();
      if (includeProcessVariables) {
        processQuery = processQuery.includeProcessVariables();
      }
      return processQuery.processInstanceId(processId).singleResult();
    }
    return null;
  }

  public static void completeTask(JsonObject task, JsonObject variable,
                                  AsyncHandler<Void> complete) {
    Objects.requireNonNull(task);
    final String taskId = task.getString("id");
    Objects.requireNonNull(taskId);
    final TaskService taskService = cpd.processEngine().getTaskService();
    try {
      taskService.complete(
          taskId,
          new HashMap<String, Object>() {{
            if (variable != null) {
              String key = variable.getMap().keySet().iterator().next();
              put(key, variable.getValue(key));
            }
          }});
      complete.handle(Future.succeededFuture());
    } catch (Exception e) {
      complete.handle(Future.failedFuture(e));
    }
  }
}
