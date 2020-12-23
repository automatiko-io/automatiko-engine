
package io.automatiko.engine.services.uow;

import io.automatiko.engine.api.event.EventManager;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.UnitOfWorkFactory;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.services.event.impl.BaseEventManager;

/**
 * Default implementation of the UnitOfWorkManager that is backed by thread
 * local to keep the associated unit of work.
 *
 */
public class DefaultUnitOfWorkManager implements UnitOfWorkManager {
    // uses thread local to associate unit of works to execution context/thread
    protected ThreadLocal<UnitOfWork> currentUnitOfWork = new ThreadLocal<>();
    // uses pass through unit of work as fallback if no unit of work has been
    // started
    protected UnitOfWork fallbackUnitOfWork = new PassThroughUnitOfWork();
    // factory used to create unit of work
    protected UnitOfWorkFactory factory;

    protected EventManager eventManager = new BaseEventManager();

    public DefaultUnitOfWorkManager(UnitOfWorkFactory factory) {
        super();
        this.factory = factory;
    }

    public DefaultUnitOfWorkManager(UnitOfWork fallbackUnitOfWork, UnitOfWorkFactory factory) {
        super();
        this.fallbackUnitOfWork = fallbackUnitOfWork;
        this.factory = factory;
    }

    @Override
    public UnitOfWork currentUnitOfWork() {
        UnitOfWork unit = currentUnitOfWork.get();

        if (unit == null) {
            return fallbackUnitOfWork;
        }
        return unit;
    }

    @Override
    public UnitOfWork newUnitOfWork() {

        return new ManagedUnitOfWork(factory.create(eventManager), this::associate, this::dissociate, this::dissociate);
    }

    protected void associate(UnitOfWork unit) {
        currentUnitOfWork.set(unit);
    }

    protected void dissociate(UnitOfWork unit) {
        currentUnitOfWork.set(null);
    }

    @Override
    public EventManager eventManager() {
        return eventManager;
    }
}
