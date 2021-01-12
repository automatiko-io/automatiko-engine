package io.automatiko.engine.service;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.services.uow.DefaultUnitOfWorkManager;

public class WorkflowThreadContextProvider implements ThreadContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowThreadContextProvider.class);

    public static final String NAME = "Workflow";

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        UnitOfWork capturedUnitOfWork = DefaultUnitOfWorkManager.getUnitOfWork();
        IdentityProvider capturedIdentity = IdentityProvider.isSet() ? IdentityProvider.get() : null;

        return () -> {

            UnitOfWork currentUnitOfWork = DefaultUnitOfWorkManager.getUnitOfWork();
            IdentityProvider currentIdentity = IdentityProvider.isSet() ? IdentityProvider.get() : null;

            if (currentUnitOfWork != capturedUnitOfWork) {
                DefaultUnitOfWorkManager.setUnitOfWork(capturedUnitOfWork);
            }
            if (currentIdentity != capturedIdentity) {
                IdentityProvider.set(capturedIdentity);
            }
            return () -> {
                DefaultUnitOfWorkManager.setUnitOfWork(currentUnitOfWork);
                IdentityProvider.set(currentIdentity);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            UnitOfWork movedUnitOfWork = DefaultUnitOfWorkManager.getUnitOfWork();
            IdentityProvider movedIdentity = IdentityProvider.isSet() ? IdentityProvider.get() : null;
            return () -> {
                if (movedUnitOfWork != null) {
                    DefaultUnitOfWorkManager.setUnitOfWork(movedUnitOfWork);
                    IdentityProvider.set(movedIdentity);
                }
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return NAME;
    }

}
