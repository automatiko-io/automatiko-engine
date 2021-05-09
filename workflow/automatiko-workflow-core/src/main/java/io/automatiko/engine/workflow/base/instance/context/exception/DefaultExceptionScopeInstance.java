
package io.automatiko.engine.workflow.base.instance.context.exception;

import io.automatiko.engine.api.jobs.DurationExpirationTime;
import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.workflow.base.core.context.ProcessContext;
import io.automatiko.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatiko.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatiko.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatiko.engine.workflow.base.instance.ProcessInstance;
import io.automatiko.engine.workflow.base.instance.impl.Action;
import io.automatiko.engine.workflow.base.instance.impl.workitem.WorkItemImpl;
import io.automatiko.engine.workflow.process.instance.NodeInstance;
import io.automatiko.engine.workflow.process.instance.impl.NodeInstanceImpl;
import io.automatiko.engine.workflow.process.instance.node.WorkItemNodeInstance;

public class DefaultExceptionScopeInstance extends ExceptionScopeInstance {

    private static final long serialVersionUID = 510l;

    public String getContextType() {
        return ExceptionScope.EXCEPTION_SCOPE;
    }

    public void handleException(io.automatiko.engine.api.runtime.process.NodeInstance nodeInstance, ExceptionHandler handler,
            String exception, Object params) {

        if (handler instanceof ActionExceptionHandler) {
            ActionExceptionHandler exceptionHandler = (ActionExceptionHandler) handler;

            if (retryAvailable(nodeInstance, exceptionHandler)) {
                Integer retryAttempts = ((NodeInstanceImpl) nodeInstance).getRetryAttempts();
                if (retryAttempts == null) {
                    retryAttempts = 1;
                } else {
                    retryAttempts = retryAttempts + 1;
                }
                long delay = calculateDelay(exceptionHandler.getRetryAfter().longValue(), retryAttempts,
                        exceptionHandler.getRetryIncrement(), exceptionHandler.getRetryIncrementMultiplier());

                DurationExpirationTime expirationTime = DurationExpirationTime.after(delay);

                JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();

                String jobId = jobService
                        .scheduleProcessInstanceJob(ProcessInstanceJobDescription.of(nodeInstance.getNodeId(),
                                "retry:" + nodeInstance.getId(),
                                expirationTime, ((NodeInstanceImpl) nodeInstance).getProcessInstanceIdWithParent(),
                                getProcessInstance().getRootProcessInstanceId(),
                                getProcessInstance().getProcessId(), getProcessInstance().getProcess().getVersion(),
                                getProcessInstance().getRootProcessId()));
                ((NodeInstanceImpl) nodeInstance).internalSetRetryJobId(jobId);
                ((NodeInstanceImpl) nodeInstance).internalSetRetryAttempts(retryAttempts);
                ((NodeInstanceImpl) nodeInstance).registerRetryEventListener();

                if (nodeInstance instanceof WorkItemNodeInstance) {
                    ((WorkItemImpl) ((WorkItemNodeInstance) nodeInstance).getWorkItem()).setState(WorkItem.RETRYING);
                }
            } else {

                Action action = (Action) exceptionHandler.getAction().getMetaData("Action");
                try {
                    ProcessInstance processInstance = getProcessInstance();
                    ProcessContext processContext = new ProcessContext(processInstance.getProcessRuntime());
                    ContextInstanceContainer contextInstanceContainer = getContextInstanceContainer();
                    if (contextInstanceContainer instanceof NodeInstance) {
                        processContext.setNodeInstance((NodeInstance) contextInstanceContainer);
                    } else {
                        processContext.setProcessInstance(processInstance);
                    }
                    String faultVariable = exceptionHandler.getFaultVariable();
                    if (faultVariable != null) {
                        processContext.setVariable(faultVariable, params);
                    }
                    action.execute(processContext);
                } catch (Exception e) {
                    throw new RuntimeException("unable to execute Action", e);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown exception handler " + handler);
        }
    }

    private boolean retryAvailable(io.automatiko.engine.api.runtime.process.NodeInstance nodeInstance,
            ActionExceptionHandler exceptionHandler) {
        if (exceptionHandler.getRetryAfter() != null && exceptionHandler.getRetryAfter() > 0) {

            Integer retryAttempts = ((NodeInstanceImpl) nodeInstance).getRetryAttempts();
            if (retryAttempts == null || retryAttempts < exceptionHandler.getRetryLimit()) {
                return true;
            }
        }
        return false;
    }

    private long calculateDelay(long delay, int attempts, Integer increment, Float multiplier) {
        // if multiplier is set use it to calculate delay
        if (multiplier != null && multiplier > 0) {
            for (int i = 1; i < attempts; i++) {
                delay = (long) (delay * multiplier);
            }

            return delay;
        }
        // otherwise check if increment is given and use it to calculate delay
        // this only applies to subsequent retries not the first one
        if (increment != null && attempts > 1) {
            return delay + (increment * attempts);
        }
        // lastly use the delay itself
        return delay;
    }

}
