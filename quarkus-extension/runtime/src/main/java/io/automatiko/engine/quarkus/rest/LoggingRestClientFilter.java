package io.automatiko.engine.quarkus.rest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class LoggingRestClientFilter implements ClientResponseFilter, ClientRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingRestClientFilter.class);
    private static final int DEFAULT_MAX_ENTITY_SIZE = 8 * 1024;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        if (LOGGER.isDebugEnabled()) {

            final StringBuilder content = new StringBuilder();
            content.append("Request:\n URI: ").append(requestContext.getUri()).append(",\n method: ")
                    .append(requestContext.getMethod())
                    .append(",\n media type: ").append(requestContext.getMediaType()).append(",\n headers: \n");

            for (Entry<String, List<Object>> header : requestContext.getHeaders().entrySet()) {
                content.append("\t").append(header.getKey()).append(":").append(header.getValue()).append("\n");
            }

            if (requestContext.hasEntity()) {
                content.append("Payload:\n").append(requestContext.getEntity());
            }
            LOGGER.debug(content.toString());
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        if (LOGGER.isDebugEnabled()) {

            final StringBuilder content = new StringBuilder();
            content.append("Response:\n status code: ").append(responseContext.getStatus()).append(",\n status info: ")
                    .append(responseContext.getStatusInfo())
                    .append(",\n media type: ").append(responseContext.getMediaType()).append(",\n headers: \n");

            for (Entry<String, List<String>> header : responseContext.getHeaders().entrySet()) {
                content.append("\t").append(header.getKey()).append(":").append(header.getValue()).append("\n");
            }

            if (responseContext.hasEntity()) {
                responseContext.setEntityStream(readEntity(responseContext.getEntityStream(), content));
            }
            LOGGER.debug(content.toString());
        }
    }

    protected InputStream readEntity(InputStream stream, StringBuilder content) throws IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        content.append("Payload:\n");
        stream.mark(DEFAULT_MAX_ENTITY_SIZE + 1);
        final byte[] entity = new byte[DEFAULT_MAX_ENTITY_SIZE + 1];
        final int entitySize = stream.read(entity);
        content.append(new String(entity, 0, Math.min(entitySize, DEFAULT_MAX_ENTITY_SIZE), StandardCharsets.UTF_8));
        if (entitySize > DEFAULT_MAX_ENTITY_SIZE) {
            content.append("...more...");
        }
        content.append("\n");
        stream.reset();

        return stream;
    }

}
