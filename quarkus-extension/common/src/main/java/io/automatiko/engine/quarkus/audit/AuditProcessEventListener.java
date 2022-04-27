package io.automatiko.engine.quarkus.audit;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.automatiko.engine.api.audit.AuditEntry;
import io.automatiko.engine.api.audit.Auditor;
import io.automatiko.engine.api.event.process.DefaultProcessEventListener;
import io.automatiko.engine.api.event.process.ProcessCompletedEvent;
import io.automatiko.engine.api.event.process.ProcessNodeInstanceFailedEvent;
import io.automatiko.engine.api.event.process.ProcessNodeLeftEvent;
import io.automatiko.engine.api.event.process.ProcessNodeTriggeredEvent;
import io.automatiko.engine.api.event.process.ProcessSignaledEvent;
import io.automatiko.engine.api.event.process.ProcessStartedEvent;
import io.automatiko.engine.api.event.process.ProcessVariableChangedEvent;
import io.automatiko.engine.workflow.audit.BaseAuditEntry;

@ApplicationScoped
public class AuditProcessEventListener extends DefaultProcessEventListener {

    private Auditor auditor;

    @Inject
    public AuditProcessEventListener(Auditor auditor) {
        this.auditor = auditor;
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance())
                .add("message", "Workflow instance started");

        auditor.publish(entry);
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance())
                .add("message", "Workflow instance finished");

        auditor.publish(entry);
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance(), event.getNodeInstance())
                .add("message", "About to execute workflow instance node");

        auditor.publish(entry);

    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance(), event.getNodeInstance())
                .add("message", "Workflow instance node executed");

        auditor.publish(entry);
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry
                .workflow(event.getProcessInstance(), event.getNodeInstance(), event.getVariableId())
                .add("message", "Workflow instance variable changed")
                .add("variablePreviousValue", event.getOldValue())
                .add("variableValue", event.getNewValue())
                .add("variableTags", event.getTags());

        auditor.publish(entry);

    }

    @Override
    public void afterNodeInstanceFailed(ProcessNodeInstanceFailedEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance(), event.getNodeInstance())
                .add("message", "Workflow instance node failed")
                .add("errorId", event.getErrorId())
                .add("errorMessage", event.getErrorMessage());

        auditor.publish(entry);
    }

    @Override
    public void beforeProcessSignaled(ProcessSignaledEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance())
                .add("message", "About to signal workflow instance")
                .add("signalName", event.getSignal())
                .add("signalData", event.getData());

        auditor.publish(entry);
    }

    @Override
    public void afterProcessSignaled(ProcessSignaledEvent event) {
        Supplier<AuditEntry> entry = () -> BaseAuditEntry.workflow(event.getProcessInstance())
                .add("message", "Workflow instance signaled")
                .add("signalName", event.getSignal())
                .add("signalData", event.getData());

        auditor.publish(entry);
    }

}
