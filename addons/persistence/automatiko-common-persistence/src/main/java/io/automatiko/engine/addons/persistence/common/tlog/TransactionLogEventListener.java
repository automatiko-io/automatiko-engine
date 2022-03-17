package io.automatiko.engine.addons.persistence.common.tlog;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessNodeInitializedEvent;
import io.automatiko.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.runtime.process.WorkflowProcessInstance;
import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.services.uow.TransactionLogWorkUnit;
import io.automatiko.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatiko.engine.workflow.process.core.node.StateBasedNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.instance.RecoveryItem;
import io.automatiko.engine.workflow.process.instance.node.LambdaSubProcessNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.StateBasedNodeInstance;
import io.automatiko.engine.workflow.process.instance.node.TimerNodeInstance;

@ApplicationScoped
public class TransactionLogEventListener extends DefaultProcessEventListener {

    private boolean enabled;

    public TransactionLogEventListener(
            @ConfigProperty(name = "quarkus.automatiko.persistence.transaction-log.enabled") Optional<Boolean> enabled) {
        this.enabled = enabled.orElse(false);
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        if (!enabled) {
            return;
        }

        if (requiresInitialization(event.getNodeInstance())) {
            return;
        }

        io.automatiko.engine.api.workflow.ProcessInstance<?> pi = ((io.automatiko.engine.api.workflow.ProcessInstance<?>) ((WorkflowProcessInstance) event
                .getProcessInstance())
                        .getMetaData("AutomatikProcessInstance"));

        TransactionLog transactionLog = pi.process().instances().transactionLog();
        if (transactionLog != null) {
            InternalProcessRuntime processRuntime = (InternalProcessRuntime) event.getProcessRuntime();
            String transactionId = processRuntime.getUnitOfWorkManager().currentUnitOfWork().identifier();

            RecoveryItem recoveryItem = new RecoveryItem();
            recoveryItem.setTransactionId(transactionId);
            recoveryItem.setNodeDefinitionId(event.getNodeInstance().getNodeDefinitionId());

            ((io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance) event.getProcessInstance())
                    .setRecoveryItem(recoveryItem);

            processRuntime.getUnitOfWorkManager().currentUnitOfWork()
                    .intercept(new TransactionLogWorkUnit(transactionId, transactionLog));
            transactionLog.record(transactionId, pi.process().id(), pi.id(), event.getNodeInstance());

        }
    }

    @Override
    public void afterNodeInitialized(ProcessNodeInitializedEvent event) {
        if (!enabled) {
            return;
        }

        if (requiresInitialization(event.getNodeInstance())) {

            io.automatiko.engine.api.workflow.ProcessInstance<?> pi = ((io.automatiko.engine.api.workflow.ProcessInstance<?>) ((WorkflowProcessInstance) event
                    .getProcessInstance())
                            .getMetaData("AutomatikProcessInstance"));

            TransactionLog transactionLog = pi.process().instances().transactionLog();
            if (transactionLog != null) {
                InternalProcessRuntime processRuntime = (InternalProcessRuntime) event.getProcessRuntime();
                String transactionId = processRuntime.getUnitOfWorkManager().currentUnitOfWork().identifier();

                RecoveryItem recoveryItem = new RecoveryItem();
                recoveryItem.setTransactionId(transactionId);
                recoveryItem.setNodeDefinitionId(event.getNodeInstance().getNodeDefinitionId());
                if (event.getNodeInstance() instanceof LambdaSubProcessNodeInstance) {
                    recoveryItem.setInstanceId(((LambdaSubProcessNodeInstance) event.getNodeInstance()).getProcessInstanceId());
                } else if (event.getNodeInstance() instanceof TimerNodeInstance) {
                    recoveryItem.setTimerId(((TimerNodeInstance) event.getNodeInstance()).getTimerId());
                } else if (event.getNodeInstance() instanceof StateBasedNodeInstance) {
                    recoveryItem.setStateTimerIds(((StateBasedNodeInstance) event.getNodeInstance()).getTimerInstances());
                }
                ((io.automatiko.engine.workflow.process.instance.WorkflowProcessInstance) event.getProcessInstance())
                        .setRecoveryItem(recoveryItem);

                processRuntime.getUnitOfWorkManager().currentUnitOfWork()
                        .intercept(new TransactionLogWorkUnit(transactionId, transactionLog));
                transactionLog.record(transactionId, pi.process().id(), pi.id(), event.getNodeInstance());

            }
        }
    }

    public boolean requiresInitialization(NodeInstance nodeInstance) {
        if (nodeInstance.getNode() instanceof SubProcessNode) {
            return true;
        }
        if (nodeInstance.getNode() instanceof TimerNode) {
            return true;
        }
        if (nodeInstance.getNode() instanceof StateBasedNode) {
            return true;
        }

        return false;
    }
}
