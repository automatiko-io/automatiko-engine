package io.automatiko.extras.gcp.pubsub;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import io.automatiko.engine.api.event.EventSource;

@Dependent
public class GcpPubSubEventSource implements EventSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSource.class);

    ObjectMapper mapper;

    private String project;

    private Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    @Inject
    public GcpPubSubEventSource(ObjectMapper mapper,
            @ConfigProperty(name = "quarkus.google.cloud.project-id") Optional<String> projectConfig) {
        this.mapper = mapper;
        this.project = projectConfig.orElseThrow(() -> new IllegalArgumentException(
                "Google Cloud Platform project is required and should be given with property named 'quarkus.google.cloud.project-id'"));

    }

    @PreDestroy
    public void close() {
        if (publishers != null) {

            for (Publisher publisher : publishers.values()) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                try {
                    publisher.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Override
    public void produce(String type, String source, Object data) {
        LOGGER.debug("GCP: publishing event with type {}", type);
        try {
            ByteString mdata = ByteString.copyFrom(mapper.writeValueAsBytes(data));
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(mdata)
                    .build();

            Publisher publisher = publishers.computeIfAbsent(type, k -> {
                TopicName topicName = TopicName.of(project, k);

                try {
                    return Publisher.newBuilder(topicName).build();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(
                    messageIdFuture,
                    new ApiFutureCallback<String>() {

                        @Override
                        public void onFailure(Throwable throwable) {
                            if (throwable instanceof ApiException) {
                                ApiException apiException = ((ApiException) throwable);
                                // details on the API exception
                                LOGGER.error("ApiException during publishing message, code {}",
                                        apiException.getStatusCode().getCode());
                            }
                            LOGGER.error("Error publishing message ", throwable);
                        }

                        @Override
                        public void onSuccess(String messageId) {
                            // Once published, returns server-assigned message ids (unique within the topic)
                            System.out.println("Published message ID: " + messageId);
                        }
                    },
                    MoreExecutors.directExecutor());

        } catch (Throwable e) {
            LOGGER.error("Unexpected error while publishing message to Google PubSub", e);
        }
    }

}
