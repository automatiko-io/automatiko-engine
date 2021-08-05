package io.automatiko.extras.gcp.pubsub;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.GoogleCredentials;
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

    private String url;
    private HttpClient httpClient;

    @Inject
    public GcpPubSubEventSource(ObjectMapper mapper,
            @ConfigProperty(name = "quarkus.google.cloud.project-id") Optional<String> projectConfig,
            @ConfigProperty(name = "quarkus.http.host") Optional<String> host,
            @ConfigProperty(name = "quarkus.http.port") Optional<String> port) {
        this.mapper = mapper;
        this.url = "http://" + host.get() + ":" + port.get();
        this.project = projectConfig.orElseThrow(() -> new IllegalArgumentException(
                "Google Cloud Platform project is required and should be given with property named 'quarkus.google.cloud.project-id'"));

    }

    @Override
    public void produce(String type, String source, Object data) {
        LOGGER.debug("GCP: publishing event with type {}", type);
        if (!runsInGoogleEnvironment()) {
            LOGGER.warn(
                    "Running in non Google environment so falling back to simple HTTP based event publishing, use only for local testing");
            if (this.httpClient == null) {
                this.httpClient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();
            }
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("ce-specversion", "1.0")
                        .header("ce-type", "google.cloud.pubsub.topic.v1.messagePublished")
                        .header("ce-source", "//pubsub.googleapis.com/projects/" + project + "/topics/" + type)
                        .header("ce-id", UUID.randomUUID().toString())
                        .POST(BodyPublishers.ofByteArray(mapper.writeValueAsBytes(data))).build();

                httpClient.sendAsync(request, BodyHandlers.ofString()).handle((res, t) -> {
                    if (res != null && res.statusCode() < 300) {
                        LOGGER.debug("Successfully produced event to {} with source {} and type {}", url, source, type);
                    } else {
                        LOGGER.error(
                                "Failed at publishing event to {} with source {} and type {}, returned response code {} and body {}",
                                url, source, type, res.statusCode(), res.body(), t);
                    }
                    return null;
                });
            } catch (JsonProcessingException e) {
                LOGGER.error("Error marshalling event data", e);
            }
        } else {

            try {

                ByteString mdata = ByteString.copyFrom(mapper.writeValueAsBytes(data));
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                        .setData(mdata)
                        .build();
                TopicName topicName = TopicName.of(project, type);
                Publisher publisher = Publisher.newBuilder(topicName).build();
                try {
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
                                    LOGGER.debug("Published message ID: {}", messageId);
                                }
                            },
                            MoreExecutors.directExecutor());

                } finally {
                    publisher.shutdown();
                    publisher.awaitTermination(1, TimeUnit.MINUTES);
                }

            } catch (Throwable e) {
                LOGGER.error("Unexpected error while publishing message to Google PubSub", e);
            }
        }
    }

    private boolean runsInGoogleEnvironment() {
        try {
            GoogleCredentials.getApplicationDefault();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
