package io.automatik.engine.addons.persistence.db;

import javax.transaction.UserTransaction;

import io.automatik.engine.api.event.EventManager;
import io.automatik.engine.services.uow.CollectingUnitOfWork;

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
