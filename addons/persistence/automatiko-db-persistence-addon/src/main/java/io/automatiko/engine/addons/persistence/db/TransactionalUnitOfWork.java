package io.automatiko.engine.addons.persistence.db;

import io.automatiko.engine.api.event.EventManager;
import io.automatiko.engine.services.uow.CollectingUnitOfWork;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

public class TransactionalUnitOfWork extends CollectingUnitOfWork {

    private UserTransaction transaction;

    private boolean owned;

    public TransactionalUnitOfWork(EventManager eventManager, UserTransaction transaction) {
        super(eventManager);
        this.transaction = transaction;
    }

    @Override
    public void start() {
        try {

            if (transaction.getStatus() == Status.STATUS_NO_TRANSACTION) {
                owned = true;
                transaction.begin();
            }
            super.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void end() {
        try {
            super.end();

            if (owned) {
                transaction.commit();
            }
        } catch (Exception e) {
            try {
                transaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException e1) {
                e1.printStackTrace();
            }
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void abort() {
        try {
            super.abort();
            if (owned) {
                transaction.rollback();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
