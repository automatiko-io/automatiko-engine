package io.automatiko.engine.addons.persistence.common.tlog;

import java.util.Collections;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.runtime.process.NodeInstance;
import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.api.uow.TransactionLogStore;
import io.automatiko.engine.workflow.marshalling.ProcessInstanceMarshaller;

public class TransactionLogImpl implements TransactionLog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionLogImpl.class);

    private boolean enabled;
    private final ProcessInstanceMarshaller marshaller;
    private final TransactionLogStore store;

    public TransactionLogImpl(TransactionLogStore store, ObjectMarshallingStrategy... strategies) {
        this.store = store;
        this.marshaller = new ProcessInstanceMarshaller(strategies);
        this.enabled = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.automatiko.persistence.transaction-log.enabled", Boolean.class).orElse(false);
    }

    private boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void record(String transactionId, String processId, String instanceId, NodeInstance currentNodeInstance) {
        if (!isEnabled()) {
            return;
        }
        try {
            byte[] content = this.marshaller.marhsallProcessInstance(currentNodeInstance.getProcessInstance(),
                    currentNodeInstance);
            store.store(transactionId, processId, instanceId, content);
        } catch (Exception e) {
            LOGGER.debug("Unable to record transaction log entry for node {} inside process instance {} (of process {})",
                    currentNodeInstance, instanceId, processId, e);
        }
    }

    @Override
    public void complete(String transactionId) {
        if (!isEnabled()) {
            return;
        }
        store.delete(transactionId);
    }

    @Override
    public byte[] readContent(String processId, String instanceId) {
        if (!isEnabled()) {
            return null;
        }
        return store.load(processId, instanceId);
    }

    @Override
    public Set<String> recoverable(String processId) {
        if (!isEnabled()) {
            return Collections.emptySet();
        }
        return store.list(processId);
    }

    @Override
    public void complete(String transactionId, String processId, String instanceId) {
        if (!isEnabled()) {
            return;
        }
        store.delete(transactionId, processId, instanceId);

    }

    @Override
    public boolean contains(String processId, String instanceId) {
        if (!isEnabled()) {
            return false;
        }
        return store.contains(processId, instanceId);
    }

    @Override
    public boolean requiresRecovery() {
        return store.list().size() > 0;
    }

}
