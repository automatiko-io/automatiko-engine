package io.automatiko.addons.fault.tolerance.uow;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.Timeout;

import io.automatiko.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TimeoutUnitOfWorkManager extends DefaultUnitOfWorkManager {

    public TimeoutUnitOfWorkManager() {
        super(new CollectingUnitOfWorkFactory());
    }

    @Timeout(value = 300, unit = ChronoUnit.SECONDS)
    @Override
    public <T> T execute(Supplier<T> supplier) {
        return super.execute(supplier);
    }

}
