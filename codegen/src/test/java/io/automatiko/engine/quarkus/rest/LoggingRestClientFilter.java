package io.automatiko.engine.quarkus.rest;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 * THIS IS JUST FOR THE TESTS, ACTUAL IMPL IS IN QUARKUS EXTENSION OF AUTOMATIKO
 *
 */
public class LoggingRestClientFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
    }

}
