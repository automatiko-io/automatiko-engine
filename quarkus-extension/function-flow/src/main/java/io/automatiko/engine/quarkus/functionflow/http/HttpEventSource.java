package io.automatiko.engine.quarkus.functionflow.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.event.EventSource;
import io.quarkus.arc.DefaultBean;

@DefaultBean
@Dependent
public class HttpEventSource implements EventSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpEventSource.class);

    ObjectMapper mapper;

    String url;

    private HttpClient httpClient;

    @Inject
    public HttpEventSource(ObjectMapper mapper, @ConfigProperty(name = "k.sink") Optional<String> url,
            @ConfigProperty(name = "quarkus.http.host") Optional<String> host,
            @ConfigProperty(name = "quarkus.http.port") Optional<String> port) {
        this.mapper = mapper;
        this.url = url.orElse("http://" + host.get() + ":" + port.get());

        this.httpClient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();
    }

    @Override
    public void produce(String type, String source, Object data) {
        if (url == null) {
            LOGGER.warn("No broker url is given, returning without publishing an event");
            return;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("ce-specversion", "1.0")
                    .header("ce-type", type)
                    .header("ce-source", source)
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
    }

}
