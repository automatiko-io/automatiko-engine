package io.automatiko.addons.fault.tolerance.internal;

import java.util.Collections;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.automatiko.engine.api.workflow.ConflictingVersionException;
import io.automatiko.engine.api.workflow.DefinedProcessErrorException;
import io.automatiko.engine.api.workflow.ProcessInstanceExecutionException;
import io.automatiko.engine.api.workflow.ServiceExecutionError;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;

public class WrappedTimout<V> implements FaultToleranceStrategy<V> {

    private final FaultToleranceStrategy<V> delegate;

    public WrappedTimout(FaultToleranceStrategy<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {

        try {
            V value = delegate.apply(ctx).awaitBlocking();
            return Future.of(value);
        } catch (ProcessInstanceExecutionException | DefinedProcessErrorException | ConflictingVersionException e) {
            throw e;
        } catch (WorkItemExecutionError e) {
            throw new ServiceExecutionError(e.getMessage(), e.getErrorCode(), e.getErrorDetails(), e.getErrorData());
        } catch (TimeoutException e) {
            throw new ServiceExecutionError(e.getMessage(), "408", "timeout occured",
                    Collections.singletonMap("cause", "timeout reached when invoking service"));
        } catch (Throwable e) {
            throw new ServiceExecutionError(e.getMessage(), "500", "timeout occured", e);
        }
    }
}
