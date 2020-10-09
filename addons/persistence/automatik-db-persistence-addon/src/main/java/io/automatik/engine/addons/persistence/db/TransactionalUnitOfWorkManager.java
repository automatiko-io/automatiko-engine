package io.automatik.engine.addons.persistence.db;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;

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
