package io.automatiko.engine.addons.persistence.db;

import io.automatiko.engine.api.event.EventManager;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.UnitOfWorkFactory;
import jakarta.transaction.UserTransaction;

public class TransactionalUnitOfWorkFactory implements UnitOfWorkFactory {

    private UserTransaction transaction;

    public TransactionalUnitOfWorkFactory(UserTransaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public UnitOfWork create(EventManager eventManager) {
        return new TransactionalUnitOfWork(eventManager, transaction);
    }

}
