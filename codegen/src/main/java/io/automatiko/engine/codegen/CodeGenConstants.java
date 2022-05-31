
package io.automatiko.engine.codegen;

public class CodeGenConstants {

    private CodeGenConstants() {

    }

    public static final String VALIDATION_CLASS = "javax.validation.constraints.NotNull";

    public static final String USERTASK_MGMT_DATA_CLASS = "io.automatiko.engine.addons.usertasks.management.UserTaskManagementResource";

    public static final String OPENA_API_SCHEMA_CLASS = "org.eclipse.microprofile.openapi.annotations.media.Schema";

    public static final String ENTITY_CLASS = "io.automatiko.engine.addons.persistence.db.model.ProcessInstanceEntity";

    public static final String GRAPHQL_CLASS = "io.smallrye.graphql.cdi.producer.GraphQLProducer";

    public static final String DMN_CLASS = "io.automatiko.engine.decision.dmn.DmnDecisionModel";

    public static final String INCOMING_PROP_PREFIX = "mp.messaging.incoming.";
    public static final String OUTGOING_PROP_PREFIX = "mp.messaging.outgoing.";
    public static final String MQTT_CONNECTOR = "smallrye-mqtt";
    public static final String KAFKA_CONNECTOR = "smallrye-kafka";
    public static final String AMQP_CONNECTOR = "smallrye-amqp";
    public static final String CAMEL_CONNECTOR = "smallrye-camel";
    public static final String JMS_CONNECTOR = "smallrye-jms";
    public static final String OPERATOR_CONNECTOR = "javaoperatorsdk";
    public static final String FUNCTION_FLOW_CONNECTOR = "function-flow";
    public static final String HTTP_CONNECTOR = "quarkus-http";

    public static final String MONGO_PERSISTENCE = "mongodb";
    public static final String CASSANDRA_PERSISTENCE = "cassandra";
    public static final String DB_PERSISTENCE = "db";
    public static final String DYNAMODB_PERSISTENCE = "dynamodb";
    public static final String FS_PERSISTENCE = "filesystem";

    public static final String MONGO_PERSISTENCE_CLASS = "io.automatiko.engine.addons.persistence.mongodb.MongodbProcessInstances";
    public static final String CASSANDRA_PERSISTENCE_CLASS = "io.automatiko.engine.addons.persistence.cassandra.CassandraProcessInstances";
    public static final String DB_PERSISTENCE_CLASS = "io.automatiko.engine.addons.persistence.db.DatabaseProcessInstances";
    public static final String DYNAMODB_PERSISTENCE_CLASS = "io.automatiko.engine.addons.persistence.dynamodb.DynamoDBProcessInstances";
    public static final String FS_PERSISTENCE_CLASS = "io.automatiko.engine.addons.persistence.filesystem.FileSystemProcessInstances";

    public static final String MQTT_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.mqtt.MqttConnector";
    public static final String JMS_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.jms.JmsConnector";
    public static final String KAFKA_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.kafka.KafkaConnector";
    public static final String AMQP_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.amqp.AmqpConnector";
    public static final String CAMEL_CONNECTOR_CLASS = "io.smallrye.reactive.messaging.camel.CamelConnector";
    public static final String OPERATOR_CONNECTOR_CLASS = "io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration";
    public static final String HTTP_CONNECTOR_CLASS = "io.quarkus.reactivemessaging.http.runtime.QuarkusHttpConnector";

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
