
package io.automatiko.engine.workflow.serverless;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.api.io.Resource;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.workflow.ProcessConfig;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.StaticProcessConfig;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;

public class ServerlessProcess extends AbstractProcess<ServerlessModel> {

    public ServerlessProcess(Process p) {
        process = p;
    }

    public ServerlessProcess(Process p, ProcessConfig config) {
        super(config);
        process = p;
    }

    @Override
    public ProcessInstance<ServerlessModel> createInstance(Model m) {
        ServerlessModel variables = createModel();
        variables.fromMap(m.toMap());
        return new ServerlessProcessInstance(this, variables, this.createProcessRuntime());
    }

    public ProcessInstance<ServerlessModel> createInstance() {
        return new ServerlessProcessInstance(this, createModel(), this.createProcessRuntime());
    }

    @Override
    public ProcessInstance<ServerlessModel> createInstance(String businessKey, ServerlessModel variables) {
        ServerlessModel variablesModel = createModel();
        variablesModel.fromMap(variables.toMap());
        return new ServerlessProcessInstance(this, variablesModel, businessKey, this.createProcessRuntime());
    }

    @Override
    public ProcessInstance<ServerlessModel> createInstance(ServerlessModel variables) {
        ServerlessModel variablesModel = createModel();
        variablesModel.fromMap(variables.toMap());
        return new ServerlessProcessInstance(this, variablesModel, this.createProcessRuntime());
    }

    @Override
    public ProcessInstance<ServerlessModel> createInstance(WorkflowProcessInstance wpi, ServerlessModel model,
            long versionTrack) {

        return new ServerlessProcessInstance(this, model, this.createProcessRuntime(), wpi, versionTrack);
    }

    @Override
    public ProcessInstance<ServerlessModel> createReadOnlyInstance(WorkflowProcessInstance wpi, ServerlessModel model) {

        return new ServerlessProcessInstance(this, model, wpi);
    }

    @Override
    public Process process() {
        return process;
    }

    @Override
    public ServerlessModel createModel() {
        return new ServerlessModel();
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();
        List<SubProcessNode> subprocessNodes = new ArrayList<SubProcessNode>();
        collectSubprocessNodes(subprocessNodes, (NodeContainer) process());

        for (SubProcessNode sp : subprocessNodes) {
            services.getSignalManager().addEventListener(sp.getProcessId(), completionEventListener);
        }
    }

    protected void collectSubprocessNodes(Collection<SubProcessNode> items, NodeContainer container) {

        for (Node node : container.getNodes()) {
            if (node instanceof SubProcessNode) {
                items.add((SubProcessNode) node);
            } else if (node instanceof NodeContainer) {
                collectSubprocessNodes(items, (NodeContainer) node);
            }
        }
    }

    /**
     *
     */
    public static List<ServerlessProcess> from(Resource... resource) {
        return from(processConfig(), resource);
    }

    public static List<ServerlessProcess> from(ProcessConfig config, Resource... resources) {

        List<ServerlessProcess> compiled = new ArrayList<>();
        ServerlessWorkflowParser parser = new ServerlessWorkflowParser();
        for (Resource resource : resources) {

            try {
                compiled.add(new ServerlessProcess(parser.parse(resource.getReader()), config));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return compiled;
    }

    @Override
    public Process buildProcess() {
        return null;
    }

    public static ProcessConfig processConfig() {

        DefaultWorkItemHandlerConfig workItemHandlerConfig = new DefaultWorkItemHandlerConfig();
        DefaultProcessEventListenerConfig processEventListenerConfig = new DefaultProcessEventListenerConfig();
        return new StaticProcessConfig(workItemHandlerConfig, processEventListenerConfig,
                new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()), null, new DefaultVariableInitializer(), null);
    }
}
