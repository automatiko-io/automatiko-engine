package io.automatiko.engine.addons.persistence.db;

import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

@ApplicationScoped
public class TransactionalUnitOfWorkManager extends DefaultUnitOfWorkManager {

    @Inject
    UserTransaction transaction;

    public TransactionalUnitOfWorkManager() {
        super(null);
    }

    @PostConstruct
    public void setup() {
        this.factory = new TransactionalUnitOfWorkFactory(transaction);
    }

}
