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
    public static final int JMS_PORT = 61616;

    private GenericContainer<?> artemisContainer;

    @Override
    public Map<String, String> start() {
        artemisContainer = new GenericContainer<>(IMAGE_NAME).withExposedPorts(JMS_PORT).withEnv("AMQ_USER", "automatiko")
                .withEnv("AMQ_PASSWORD", "automatiko").withLogConsumer(new Slf4jLogConsumer(LOGGER));
        artemisContainer.waitingFor(new HostPortWaitStrategy()).start();
        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.artemis.url",
                String.format("tcp://%s:%d", artemisContainer.getHost(), artemisContainer.getMappedPort(JMS_PORT)));
        properties.put("quarkus.artemis.username", "automatiko");
        properties.put("quarkus.artemis.password", "automatiko");

        return properties;
    }

    @Override
    public void stop() {
        if (artemisContainer != null) {
            artemisContainer.stop();
        }
    }
}
