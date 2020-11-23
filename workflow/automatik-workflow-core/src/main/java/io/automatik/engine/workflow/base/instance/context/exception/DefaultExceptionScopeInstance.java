
package io.automatik.engine.workflow.base.instance.context.exception;

import io.automatik.engine.api.jobs.DurationExpirationTime;
import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.workflow.base.core.context.ProcessContext;
import io.automatik.engine.workflow.base.core.context.exception.ActionExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionHandler;
import io.automatik.engine.workflow.base.core.context.exception.ExceptionScope;
import io.automatik.engine.workflow.base.instance.ContextInstanceContainer;
import io.automatik.engine.workflow.base.instance.ProcessInstance;
import io.automatik.engine.workflow.base.instance.impl.Action;
import io.automatik.engine.workflow.process.instance.NodeInstance;
import io.automatik.engine.workflow.process.instance.impl.NodeInstanceImpl;

public class DefaultExceptionScopeInstance extends ExceptionScopeInstance {

    private static final long serialVersionUID = 510l;

    public String getContextType() {
        return ExceptionScope.EXCEPTION_SCOPE;
    }

    public void handleException(io.automatik.engine.api.runtime.process.NodeInstance nodeInstance, ExceptionHandler handler,
            String exception, Object params) {

        if (handler instanceof ActionExceptionHandler) {
            ActionExceptionHandler exceptionHandler = (ActionExceptionHandler) handler;

            if (retryAvailable(nodeInstance, exceptionHandler)) {
                Integer retryAttempts = ((NodeInstanceImpl) nodeInstance).getRetryAttempts();
                if (retryAttempts == null) {
                    retryAttempts = 1;
                    DurationExpirationTime expirationTime = DurationExpirationTime.repeat(
                            exceptionHandler.getRetryAfter().longValue(),
                            exceptionHandler.getRetryAfter().longValue(), exceptionHandler.getRetryLimit());

                    JobsService jobService = getProcessInstance().getProcessRuntime().getJobsService();

                    String jobId = jobService
                            .scheduleProcessInstanceJob(ProcessInstanceJobDescription.of(nodeInstance.getNodeId(),
                                    "retry:" + nodeInstance.getId(),
                                    expirationTime, getProcessInstance().getId(),
                                    getProcessInstance().getRootProcessInstanceId(),
                                    getProcessInstance().getProcessId(), getProcessInstance().getProcess().getVersion(),
                                    getProcessInstance().getRootProcessId()));
                    ((NodeInstanceImpl) nodeInstance).internalSetRetryJobId(jobId);
                    ((NodeInstanceImpl) nodeInstance).internalSetRetryAttempts(retryAttempts);
                } else {
                    ((NodeInstanceImpl) nodeInstance).internalSetRetryAttempts(retryAttempts + 1);
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

    private boolean retryAvailable(io.automatik.engine.api.runtime.process.NodeInstance nodeInstance,
            ActionExceptionHandler exceptionHandler) {
        if (exceptionHandler.getRetryAfter() > 0) {

            Integer retryAttempts = ((NodeInstanceImpl) nodeInstance).getRetryAttempts();
            if (retryAttempts == null || retryAttempts < exceptionHandler.getRetryLimit()) {
                return true;
            }
        }
        return false;
    }

}
