package io.automatiko.addons.fault.tolerance.internal;

import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import io.automatiko.addons.fault.tolerance.CircuitClosedEvent;
import io.automatiko.engine.api.workflow.ServiceExecutionError;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;
import io.smallrye.faulttolerance.core.FaultToleranceContext;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.core.Future;
import io.smallrye.faulttolerance.core.circuit.breaker.CircuitBreakerEvents;

public class NotifyingCircuitBreaker<V> implements FaultToleranceStrategy<V> {

    private final String name;
    private final Consumer<CircuitClosedEvent> consumer;
    private final FaultToleranceStrategy<V> delegate;

    public NotifyingCircuitBreaker(String name, FaultToleranceStrategy<V> delegate, Consumer<CircuitClosedEvent> consumer) {
        this.name = name;
        this.delegate = delegate;
        this.consumer = consumer;
    }

    @Override
    public Future<V> apply(FaultToleranceContext<V> ctx) {
        ctx.registerEventHandler(CircuitBreakerEvents.StateTransition.class, e -> {

            if (e.targetState.equals(CircuitBreakerEvents.StateTransition.TO_CLOSED.targetState)) {
                consumer.accept(new CircuitClosedEvent(name));
            }
        });
        try {
            V value = delegate.apply(ctx).awaitBlocking();
            return Future.of(value);
        } catch (WorkItemExecutionError e) {
            throw new ServiceExecutionError(e.getMessage(), e.getErrorCode(), name, e.getErrorData());
        } catch (CircuitBreakerOpenException e) {
            throw new ServiceExecutionError("Service not available", "503", name,
                    Collections.singletonMap("cause", "service call has been prevented due to too many failures"));
        } catch (TimeoutException e) {
            throw new ServiceExecutionError(e.getMessage(), "408", "timeout occured",
                    Collections.singletonMap("cause", "timeout reached when invoking service"));
        } catch (Throwable e) {
            throw new ServiceExecutionError(e.getMessage(), "500", name, e);
        }
    }

}
