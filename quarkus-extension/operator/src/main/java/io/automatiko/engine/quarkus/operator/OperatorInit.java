package io.automatiko.engine.quarkus.operator;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class OperatorInit {

    @Inject
    Operator operator;

    public void start(@Observes StartupEvent event) {

        operator.start();
    }
}
