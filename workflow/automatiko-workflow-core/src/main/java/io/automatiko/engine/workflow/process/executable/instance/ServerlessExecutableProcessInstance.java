
package io.automatiko.engine.workflow.process.executable.instance;

import io.automatiko.engine.workflow.process.executable.core.ServerlessExecutableProcess;

public class ServerlessExecutableProcessInstance extends ExecutableProcessInstance {

    private static final long serialVersionUID = 510l;

    public ServerlessExecutableProcess getRuleFlowProcess() {
        return (ServerlessExecutableProcess) getProcess();
    }

    @Override
    public String toString() {
        return new StringBuilder("ServerlessExecutableProcessInstance").append(" [Id=").append(getId()).append(",processId=")
                .append(getProcessId()).append(",state=").append(getState()).append("]").toString();
    }
}
