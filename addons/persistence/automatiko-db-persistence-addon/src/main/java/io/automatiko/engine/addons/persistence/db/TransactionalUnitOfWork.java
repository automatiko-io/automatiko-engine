package io.automatiko.engine.addons.persistence.db;

import io.automatiko.engine.api.event.EventManager;
import io.automatiko.engine.services.uow.CollectingUnitOfWork;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

public class TransactionalUnitOfWork extends CollectingUnitOfWork {

    private UserTransaction transaction;

    public TransactionalUnitOfWork(EventManager eventManager, UserTransaction transaction) {
        super(eventManager);
        this.transaction = transaction;
    }

    @Override
    public void start() {
        try {
            transaction.begin();
            super.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void end() {
        try {
            super.end();

            transaction.commit();
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
            transaction.rollback();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
