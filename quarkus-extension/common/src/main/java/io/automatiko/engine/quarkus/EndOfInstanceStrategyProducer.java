package io.automatiko.engine.quarkus;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.workflow.ArchiveStore;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy.Type;
import io.automatiko.engine.workflow.base.instance.impl.end.ArchiveEndOfInstanceStrategy;
import io.automatiko.engine.workflow.base.instance.impl.end.KeepEndOfInstanceStrategy;
import io.automatiko.engine.workflow.base.instance.impl.end.RemoveEndOfInstanceStrategy;

@ApplicationScoped
public class EndOfInstanceStrategyProducer {

    @Produces
    public EndOfInstanceStrategy produce(
            @ConfigProperty(name = "quarkus.automatiko.on-instance-end", defaultValue = "REMOVE") String type,
            Instance<ArchiveStore> storage) {
        EndOfInstanceStrategy.Type strategyType = Type.valueOf(type.toUpperCase());
        EndOfInstanceStrategy strategy = null;
        switch (strategyType) {
            case REMOVE:
                strategy = new RemoveEndOfInstanceStrategy();
                break;
            case KEEP:
                strategy = new KeepEndOfInstanceStrategy();
                break;
            case ARCHIVE:
                strategy = new ArchiveEndOfInstanceStrategy(storage.get());
                break;

            default:
                break;
        }

        return strategy;
    }
}
