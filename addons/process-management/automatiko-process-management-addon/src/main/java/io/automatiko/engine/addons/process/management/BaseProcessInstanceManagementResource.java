
package io.automatiko.engine.addons.process.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessError;
import io.automatiko.engine.api.workflow.ProcessErrors;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.ProcessInstanceReadMode;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.services.uow.UnitOfWorkExecutor;
import io.automatiko.engine.services.utils.StringUtils;

public abstract class BaseProcessInstanceManagementResource<T> implements ProcessInstanceManagement<T> {

    private static final String PROCESS_AND_INSTANCE_REQUIRED = "Process id and Process instance id must be given";
    private static final String PROCESS_NOT_FOUND = "Process with id %s not found";
    private static final String PROCESS_INSTANCE_NOT_FOUND = "Process instance with id %s not found";
    private static final String PROCESS_INSTANCE_NOT_IN_ERROR = "Process instance with id %s is not in error state";

    protected Map<String, Process<?>> processData = new LinkedHashMap<String, Process<?>>();

    protected Application application;

    public BaseProcessInstanceManagementResource(Map<String, Process<?>> processData, Application application) {
        this.processData = processData;
        this.application = application;

        for (String processId : new ArrayList<>(processData.keySet())) {
            this.processData.put(StringUtils.toDashCase(processId), this.processData.get(processId));
            this.processData.putIfAbsent(StringUtils.toCamelCase(processId), this.processData.get(processId));
            this.processData.putIfAbsent(processId, this.processData.get(processId));
        }
    }

    public T doGetInstanceInError(String processId, String processInstanceId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            ProcessErrors errors = processInstance.errors().get();

            List<Map<String, String>> errorsData = new ArrayList<>();

            for (ProcessError error : errors.errors()) {
                Map<String, String> data = new HashMap<>();
                data.put("id", processInstance.id());
                data.put("failedNodeId", error.failedNodeId());
                data.put("message", error.errorMessage());

                errorsData.add(data);
            }

            return buildOkResponse(errorsData);
        });
    }

    public T doGetWorkItemsInProcessInstance(String processId, String processInstanceId) {

        return executeOnInstance(processId, processInstanceId, "active", processInstance -> {
            // use special security policy to bypass auth check as this is management
            // operation
            List<WorkItem> workItems = processInstance.workItems(new SecurityPolicy(null) {
            });

            return buildOkResponse(workItems);
        });
    }

    public T doRetriggerInstanceInError(String processId, String processInstanceId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            processInstance.errors().get().retrigger();

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doRetriggerInstanceInErrorByErrorId(String processId, String processInstanceId, String errorId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            processInstance.errors().get().errors().stream().filter(e -> e.errorId().equals(errorId)).findFirst()
                    .ifPresent(e -> e.retrigger());

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doSkipInstanceInError(String processId, String processInstanceId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            processInstance.errors().get().skip();

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doSkipInstanceInErrorByErrorId(String processId, String processInstanceId, String errorId) {

        return executeOnInstanceInError(processId, processInstanceId, processInstance -> {
            processInstance.errors().get().errors().stream().filter(e -> e.errorId().equals(errorId)).findFirst()
                    .ifPresent(e -> e.skip());

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doTriggerNodeInstanceId(String processId, String processInstanceId, String nodeId) {

        return executeOnInstance(processId, processInstanceId, "active", processInstance -> {
            processInstance.triggerNode(nodeId);

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doRetriggerNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId) {

        return executeOnInstance(processId, processInstanceId, "active", processInstance -> {
            processInstance.retriggerNodeInstance(nodeInstanceId);

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doCancelNodeInstanceId(String processId, String processInstanceId, String nodeInstanceId) {

        return executeOnInstance(processId, processInstanceId, "active", processInstance -> {
            processInstance.cancelNodeInstance(nodeInstanceId);

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    public T doCancelProcessInstanceId(String processId, String processInstanceId, String status) {

        return executeOnInstance(processId, processInstanceId, status, processInstance -> {
            processInstance.abort();

            if (processInstance.status() == ProcessInstance.STATE_ERROR) {
                throw new ProcessInstanceExecutionException(processInstance.id(),
                        processInstance.errors().get().failedNodeIds(), processInstance.errors().get().errorMessages());
            } else {
                return buildOkResponse(processInstance.variables());
            }
        });
    }

    /*
     * Helper methods
     */

    private T executeOnInstanceInError(String processId, String processInstanceId,
            Function<ProcessInstance<?>, T> supplier) {
        if (processId == null || processInstanceId == null) {
            return badRequestResponse(PROCESS_AND_INSTANCE_REQUIRED);
        }

        Process<?> process = processData.get(processId);
        if (process == null) {
            return notFoundResponse(String.format(PROCESS_NOT_FOUND, processId));
        }

        return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                    .findById(processInstanceId, ProcessInstance.STATE_ERROR, ProcessInstanceReadMode.MUTABLE_WITH_LOCK);
            if (processInstanceFound.isPresent()) {
                ProcessInstance<?> processInstance = processInstanceFound.get();

                if (processInstance.errors().isPresent()) {
                    return supplier.apply(processInstance);
                } else {
                    return badRequestResponse(String.format(PROCESS_INSTANCE_NOT_IN_ERROR, processInstanceId));
                }
            } else {
                return notFoundResponse(String.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId));
            }
        });
    }

    private T executeOnInstance(String processId, String processInstanceId, String status,
            Function<ProcessInstance<?>, T> supplier) {
        if (processId == null || processInstanceId == null) {
            return badRequestResponse(PROCESS_AND_INSTANCE_REQUIRED);
        }

        Process<?> process = processData.get(processId);
        if (process == null) {
            return notFoundResponse(String.format(PROCESS_NOT_FOUND, processId));
        }
        return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
            Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
                    .findById(processInstanceId, mapStatus(status), ProcessInstanceReadMode.MUTABLE_WITH_LOCK);
            if (processInstanceFound.isPresent()) {
                ProcessInstance<?> processInstance = processInstanceFound.get();

                return supplier.apply(processInstance);
            } else {
                return notFoundResponse(String.format(PROCESS_INSTANCE_NOT_FOUND, processInstanceId));
            }
        });
    }

    protected abstract <R> T buildOkResponse(R body);

    protected abstract T badRequestResponse(String message);

    protected abstract T notFoundResponse(String message);

    protected int mapStatus(String status) {
        int state = 1;
        switch (status.toLowerCase()) {
            case "active":
                state = 1;
                break;
            case "completed":
                state = 2;
                break;
            case "aborted":
                state = 3;
                break;
            case "error":
                state = 5;
                break;
            default:
                state = Integer.parseInt(status);
                break;
        }
        return state;
    }

    protected String reverseMapStatus(int status) {
        String state = "active";
        switch (status) {
            case 1:
                state = "active";
                break;
            case 2:
                state = "completed";
                break;
            case 3:
                state = "aborted";
                break;
            case 5:
                state = "error";
                break;
            default:
                break;
        }
        return state;
    }
}
