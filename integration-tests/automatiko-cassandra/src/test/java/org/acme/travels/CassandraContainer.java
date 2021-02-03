package org.acme.travels;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class CassandraContainer implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraContainer.class);
    private GenericContainer<?> cassandra;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        cassandra = new FixedHostPortGenericContainer<>("launcher.gcr.io/google/cassandra3").withFixedExposedPort(9042, 9042)
                // wait for the server to be fully started
                .waitingFor(Wait.forLogMessage(".*\\bStarting listening for CQL clients\\b.*", 1))
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        cassandra.start();

        return Collections.emptyMap();

    }

    @Override
    public void stop() {
        cassandra.stop();
    }

}
