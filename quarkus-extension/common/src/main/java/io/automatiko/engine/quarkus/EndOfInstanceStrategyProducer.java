package io.automatiko.engine.quarkus;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.workflow.ArchiveStore;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy;
import io.automatiko.engine.api.workflow.EndOfInstanceStrategy.Type;
import io.automatiko.engine.workflow.base.instance.impl.end.ArchiveEndOfInstanceStrategy;
import io.automatiko.engine.workflow.base.instance.impl.end.KeepEndOfInstanceStrategy;
import io.automatiko.engine.workflow.base.instance.impl.end.RemoveEndOfInstanceStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@ApplicationScoped
public class EndOfInstanceStrategyProducer {

    @Named("default")
    @Produces
    public EndOfInstanceStrategy produceDefault(
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

    @Named("remove")
    @Produces
    public EndOfInstanceStrategy produceRemove() {
        return new RemoveEndOfInstanceStrategy();
    }

    @Named("keep")
    @Produces
    public EndOfInstanceStrategy produceKeepStrategy() {

        return new KeepEndOfInstanceStrategy();
    }

    @Named("archive")
    @Produces
    public EndOfInstanceStrategy produceArchive(Instance<ArchiveStore> storage) {

        return new KeepEndOfInstanceStrategy();
    }
}
