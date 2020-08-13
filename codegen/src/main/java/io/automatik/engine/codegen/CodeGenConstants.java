
package io.automatik.engine.codegen;

public class CodeGenConstants {

	private CodeGenConstants() {

	}

	public static final String VALIDATION_CLASS = "javax.validation.constraints.NotNull";

	public static final String INCOMING_PROP_PREFIX = "mp.messaging.incoming.";
	public static final String MQTT_CONNECTOR = "smallrye-mqtt";
	public static final String KAFKA_CONNECTOR = "smallrye-kafka";
	public static final String AMQP_CONNECTOR = "smallrye-amqp";
	public static final String CAMEL_CONNECTOR = "smallrye-camel";

	public static final String MQTT_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.mqtt.MqttConnector";
	public static final String KAFKA_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.kafka.KafkaConnector";
	public static final String AMQP_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.amqp.AmqpConnector";
	public static final String CAMEL_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.camel.CamelConnector";

}
