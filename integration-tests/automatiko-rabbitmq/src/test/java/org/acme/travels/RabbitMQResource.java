package org.acme.travels;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RabbitMQResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQResource.class);
    public static final String IMAGE_NAME = "rabbitmq:3.7.25-management-alpine";

    public static final int AMQP_PORT = 5672;

    private RabbitMQContainer container;

    @Override
    public Map<String, String> start() {
        container = new RabbitMQContainer(IMAGE_NAME).withPluginsEnabled("rabbitmq_amqp1_0").withExposedPorts(AMQP_PORT)
                .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1))
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .withUser("guest", "guest");

        container.start();

        Map<String, String> properties = new HashMap<>();
        properties.put("amqp-host", container.getHost());
        properties.put("amqp-port", container.getMappedPort(AMQP_PORT).toString());
        properties.put("amqp-username", "guest");
        properties.put("amqp-password", "guest");

        return properties;
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
            container.close();
        }
    }
}
