
package io.automatiko.engine.workflow.base.instance;

import java.util.Date;
import java.util.Map;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.process.instance.NodeInstance;

/**
 * A process instance is the representation of a process during its execution.
 * It contains all the runtime status information about the running process. A
 * process can have multiple instances.
 * 
 */
public interface ProcessInstance
        extends io.automatiko.engine.api.runtime.process.ProcessInstance, ContextInstanceContainer, ContextableInstance {

    void setId(String id);

    void setProcess(Process process);

    Process getProcess();

    void setState(int state);

    void setState(int state, String outcome);

    void setState(int state, String outcome, Object faultData);

    void setErrorState(NodeInstance nodeInstanceInError, Exception e);

    void setProcessRuntime(InternalProcessRuntime runtime);

    InternalProcessRuntime getProcessRuntime();

    void start();

    void start(String tigger, Object triggerData);

    String getOutcome();

    void setParentProcessInstanceId(String parentId);

    void setRootProcessInstanceId(String parentId);

    Map<String, Object> getMetaData();

    Object getFaultData();

    void setSignalCompletion(boolean signalCompletion);

    boolean isSignalCompletion();

    Date getStartDate();

    void setStartDate(Date date);

    int getSlaCompliance();

    Date getSlaDueDate();

    void configureSLA();

    void setReferenceId(String referenceId);

    String getInitiator();

    void setInitiator(String initiator);

}
