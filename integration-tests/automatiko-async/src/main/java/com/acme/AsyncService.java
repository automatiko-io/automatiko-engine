package com.acme;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.workflow.ServiceExecutionError;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class AsyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncService.class);

    public Uni<String> asyncName(String name) {

        if (name != null) {
            LOGGER.info("Returning value");

            return Uni.createFrom().item("Here is async name");
        } else {
            LOGGER.info("Returing error");
            return Uni.createFrom().item("Here is async name").onItem()
                    .failWith(i -> new ServiceExecutionError("500", "Test error", (Object) "fallback name"));
        }
    }

    public Uni<String> asyncCall() {

        LOGGER.info("Invoked service in background");

        return Uni.createFrom().item("Here is async name");
    }

    public Uni<String> asyncCallDelayed() {

        LOGGER.info("Invoked service delayed in background");

        return Uni.createFrom().item("Here is async name").onItem().delayIt().by(Duration.ofSeconds(2));
    }

    public Uni<String> asyncCallFailing() {

        LOGGER.info("Failing service in background");

        return Uni.createFrom().item("Here is async name").onItem()
                .failWith(i -> new ServiceExecutionError("500", "Test error", (Object) "fallback name"));
    }

    public Uni<Void> asyncNoData() {

        LOGGER.info("Invoked service in fire and forget mode");

        return Uni.createFrom().nullItem();
    }
}
