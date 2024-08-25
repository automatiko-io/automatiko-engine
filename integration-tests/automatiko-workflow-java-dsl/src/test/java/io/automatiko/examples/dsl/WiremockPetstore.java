package io.automatiko.examples.dsl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WiremockPetstore implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        stubFor(get(urlEqualTo("/v2/pet/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"id\": 0,\n" +
                                "  \"category\": {\n" +
                                "    \"id\": 0,\n" +
                                "    \"name\": \"dogs\"\n" +
                                "  },\n" +
                                "  \"name\": \"doggie\",\n" +
                                "  \"photoUrls\": [\n" +
                                "    \"string\"\n" +
                                "  ],\n" +
                                "  \"tags\": [\n" +
                                "    {\n" +
                                "      \"id\": 0,\n" +
                                "      \"name\": \"string\"\n" +
                                "    }\n" +
                                "  ],\n" +
                                "  \"status\": \"available\"\n" +
                                "}")));

        stubFor(get(urlEqualTo("/v2/pet/3000"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(404)
                        .withBody("{\n" +
                                "}")));
        stubFor(get(urlMatching(".*")).atPriority(10).willReturn(aResponse().proxiedFrom("https://petstore.swagger.io/v2")));
        Map<String, String> urls = new HashMap<>();

        urls.put("swaggerpetstore/mp-rest/url", wireMockServer.baseUrl() + "/v2");

        urls.put("swaggerasyncpetstore/mp-rest/url", wireMockServer.baseUrl() + "/v2");
        return urls;
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }
}
