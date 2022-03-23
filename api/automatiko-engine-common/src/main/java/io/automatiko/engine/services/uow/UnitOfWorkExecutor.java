
package io.automatiko.engine.services.uow;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;

public class UnitOfWorkExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitOfWorkExecutor.class);

    public static <T> T executeInUnitOfWork(UnitOfWorkManager uowManager, Supplier<T> supplier) {
        T result = null;
        UnitOfWork uow = uowManager.newUnitOfWork();

        try {
            uow.start();

            result = supplier.get();
            uow.end();

            return result;
        } catch (ProcessInstanceExecutionException e) {
            uow.end();

            throw e;
        } catch (DefinedProcessErrorException e) {
            uow.end();

            throw e;
        } catch (ConflictingVersionException e) {
            LOGGER.warn(
                    "A conflict was identified for current unit of work with message '{}', aborting current unit of work and retrying",
                    e.getMessage());
            uow.abort();
            return executeInUnitOfWork(uowManager, supplier);
        } catch (Exception e) {
            e.printStackTrace();
            uow.abort();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            // reset identity provider upon completion of the unit of work
            IdentityProvider.set(null);
        }

    }
}
