package org.acme.travels;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ArtemisResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisResource.class);

    public static final String IMAGE_NAME = "quay.io/artemiscloud/activemq-artemis-broker";

    public static final int AMQP_PORT = 5672;

    private GenericContainer<?> artemisContainer;

    @Override
    public Map<String, String> start() {
        artemisContainer = new GenericContainer<>(IMAGE_NAME).withExposedPorts(AMQP_PORT).withEnv("AMQ_USER", "automatiko")
                .withEnv("AMQ_PASSWORD", "automatiko").withLogConsumer(new Slf4jLogConsumer(LOGGER));
        artemisContainer.waitingFor(new HostPortWaitStrategy()).start();
        Map<String, String> properties = new HashMap<>();
        properties.put("amqp-host", artemisContainer.getHost());
        properties.put("amqp-port", artemisContainer.getMappedPort(AMQP_PORT).toString());
        properties.put("amqp-username", "automatiko");
        properties.put("amqp-password", "automatiko");

        return properties;
    }

    @Override
    public void stop() {
        if (artemisContainer != null) {
            artemisContainer.stop();
        }
    }
}
