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

public class DynamoDBContainer implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBContainer.class);
    private GenericContainer<?> dynamodb;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        dynamodb = new FixedHostPortGenericContainer<>("amazon/dynamodb-local:1.11.477").withFixedExposedPort(8000, 8000)
                // wait for the server to be fully started
                .waitingFor(Wait.forLogMessage(".*\\bInitializing DynamoDB Local\\b.*", 1))
                .withCommand("-jar DynamoDBLocal.jar -inMemory -sharedDb")
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
        dynamodb.start();

        return Collections.emptyMap();

    }

    @Override
    public void stop() {
        dynamodb.stop();
    }

}
