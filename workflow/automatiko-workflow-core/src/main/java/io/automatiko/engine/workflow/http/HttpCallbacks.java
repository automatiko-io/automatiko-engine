package io.automatiko.engine.workflow.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpCallbacks {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCallbacks.class);
    private static HttpCallbacks INSTANCE;

    private HttpClient httpcClient;

    private ObjectMapper mapper = new ObjectMapper();

    private HttpCallbacks() {
        this.httpcClient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).build();
    }

    public void post(String url, Object model, Map<String, String> headers, int status) {
        if (url == null || model == null) {
            return;
        }
        try {
            Builder builder = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofByteArray(mapper.writeValueAsBytes(model)));

            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    builder.header(entry.getKey(), entry.getValue());
                }
            }

            builder.header("X-ATK-Status", mapStatus(status));

            HttpRequest request = builder.build();

            httpcClient.sendAsync(request, BodyHandlers.discarding());

        } catch (Exception e) {
            LOGGER.warn("Unable to post back result of execution to url {}", url, e);
        }
    }

    public static HttpCallbacks get() {
        if (INSTANCE == null) {
            synchronized (HttpCallbacks.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HttpCallbacks();
                }
            }
        }
        return INSTANCE;
    }

    protected String mapStatus(int status) {
        String mapped = null;
        switch (status) {
            case 1:
                mapped = "Active";
                break;
            case 2:
                mapped = "Completed";
                break;
            case 3:
                mapped = "Aborted";
                break;
            case 5:
                mapped = "Failed";
                break;
            default:
                mapped = "unknown";
                break;
        }

        return mapped;
    }
}
