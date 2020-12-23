
package io.automatiko.engine.services.uow;

import java.util.function.Supplier;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.uow.UnitOfWork;
import io.automatiko.engine.api.uow.UnitOfWorkManager;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;

public class UnitOfWorkExecutor {

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
        } catch (Exception e) {
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
