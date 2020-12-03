
package io.automatik.engine.codegen;

public class CodeGenConstants {

    private CodeGenConstants() {

    }

    public static final String VALIDATION_CLASS = "javax.validation.constraints.NotNull";

    public static final String ENTITY_CLASS = "io.automatik.engine.addons.persistence.db.model.ProcessInstanceEntity";

    public static final String INCOMING_PROP_PREFIX = "mp.messaging.incoming.";
    public static final String OUTGOING_PROP_PREFIX = "mp.messaging.outgoing.";
    public static final String MQTT_CONNECTOR = "smallrye-mqtt";
    public static final String KAFKA_CONNECTOR = "smallrye-kafka";
    public static final String AMQP_CONNECTOR = "smallrye-amqp";
    public static final String CAMEL_CONNECTOR = "smallrye-camel";

    public static final String MQTT_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.mqtt.MqttConnector";
    public static final String KAFKA_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.kafka.KafkaConnector";
    public static final String AMQP_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.amqp.AmqpConnector";
    public static final String CAMEL_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.camel.CamelConnector";

    public static final String MP_RESTCLIENT_PROP_URL = "/mp-rest/url";
    public static final String MP_RESTCLIENT_PROP_AUTH_TYPE = "/mp-rest/auth-type";
    public static final String MP_RESTCLIENT_PROP_BASIC = "/mp-rest/auth-basic";
    public static final String MP_RESTCLIENT_PROP_USER = "/mp-rest/auth-user";
    public static final String MP_RESTCLIENT_PROP_PASSWORD = "/mp-rest/auth-password";
    public static final String MP_RESTCLIENT_PROP_ACCESS_TOKEN = "/mp-rest/auth-access-token";
    public static final String MP_RESTCLIENT_PROP_REFRESH_TOKEN = "/mp-rest/auth-refresh-token";
    public static final String MP_RESTCLIENT_PROP_REFRESH_URL = "/mp-rest/auth-refresh-url";
    public static final String MP_RESTCLIENT_PROP_CLIENT_ID = "/mp-rest/auth-client-id";
    public static final String MP_RESTCLIENT_PROP_CLIENT_SECRET = "/mp-rest/auth-client-secret";
    public static final String MP_RESTCLIENT_PROP_CUSTOM_NAME = "/mp-rest/auth-custom-name";
    public static final String MP_RESTCLIENT_PROP_CUSTOM_VALUE = "/mp-rest/auth-custom-value";
    public static final String MP_RESTCLIENT_PROP_ON_BEHALF_NAME = "/mp-rest/auth-on-behalf-name";
}
