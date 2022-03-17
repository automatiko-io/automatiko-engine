package io.automatiko.engine.services.uow;

import io.automatiko.engine.api.uow.TransactionLog;
import io.automatiko.engine.api.uow.WorkUnit;

public class TransactionLogWorkUnit implements WorkUnit<TransactionLog> {

    private String transactionId;
    private TransactionLog transactionLog;

    public TransactionLogWorkUnit(String transactionId, TransactionLog transactionLog) {
        this.transactionId = transactionId;
        this.transactionLog = transactionLog;
    }

    @Override
    public TransactionLog data() {
        return transactionLog;
    }

    @Override
    public void perform() {
        cleanup();
    }

    @Override
    public void abort() {
        cleanup();
    }

    @Override
    public Integer priority() {
        return 9000;
    }

    protected void cleanup() {
        try {
            transactionLog.complete(transactionId);
        } catch (Exception e) {

        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TransactionLogWorkUnit other = (TransactionLogWorkUnit) obj;
        if (transactionId == null) {
            if (other.transactionId != null)
                return false;
        } else if (!transactionId.equals(other.transactionId))
            return false;
        return true;
    }

}
