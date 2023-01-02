package io.automatiko.addons.fault.tolerance.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.automatiko.addons.fault.tolerance.CircuitClosedEvent;
import io.smallrye.faulttolerance.FaultToleranceOperationProvider;
import io.smallrye.faulttolerance.SpecCompatibility;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.core.FaultToleranceStrategy;
import io.smallrye.faulttolerance.internal.InterceptionPoint;
import io.smallrye.faulttolerance.internal.StrategyCache;

@Alternative
@Singleton
public class AutomatikoStrategyCache extends StrategyCache {

    Event<CircuitClosedEvent> events;
    FaultToleranceOperationProvider operationProvider;
    Set<String> circuitBreakers = new HashSet<>();

    @Inject
    public AutomatikoStrategyCache(Event<CircuitClosedEvent> events, FaultToleranceOperationProvider operationProvider,
            SpecCompatibility specCompatibility) {
        super(specCompatibility);
        this.events = events;
        this.operationProvider = operationProvider;
    }

    @Override
    public <V> FaultToleranceStrategy<V> getStrategy(InterceptionPoint point, Supplier<FaultToleranceStrategy<V>> producer) {

        return super.getStrategy(point,
                () -> {
                    FaultToleranceOperation operation = operationProvider.get(point.method().getDeclaringClass(),
                            point.method());
                    FaultToleranceStrategy<V> t = producer.get();
                    if (operation.hasCircuitBreaker()) {
                        String cbName = operation.hasCircuitBreakerName()
                                ? operation.getCircuitBreakerName().value()
                                : UUID.randomUUID().toString();

                        circuitBreakers.add(cbName);

                        return new NotifyingCircuitBreaker<V>(cbName, t, event -> events.fire(event));
                    } else if (operation.hasTimeout()) {
                        return new WrappedTimout<V>(t);
                    } else {
                        return t;
                    }
                });

    }

    public Set<String> circuitBreakerNames() {
        return circuitBreakers;
    }

}
