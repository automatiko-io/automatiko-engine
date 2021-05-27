package io.automatiko.engine.service.auth;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.workflow.http.HttpAuthorization;

public class HttpAuthSupport {

    private ObjectMapper mapper = new ObjectMapper();

    private Config config = ConfigProvider.getConfig();

    @SuppressWarnings("unchecked")
    public Map<String, String> produce(Map<String, String> incomingHeaders) {

        Map<String, String> map = new HashMap<>();

        String authType = config.getOptionalValue("quarkus.automatiko.async.callback.auth-type", String.class)
                .orElse("not-set");

        if (authType.equalsIgnoreCase("basic")) {
            String basicData = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-basic", String.class)
                    .orElse(null);

            String username = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-user", String.class).orElse(null);
            String password = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-password", String.class)
                    .orElse(null);
            if (username != null && password != null) {
                map.put("Authorization",
                        "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
            } else if (basicData != null) {
                map.put("Authorization", "Basic " + basicData);
            }
        } else if (authType.equalsIgnoreCase("oauth")) {
            String accessToken = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-access-token", String.class)
                    .orElse(null);

            if (accessToken == null) {
                String clientId = config
                        .getOptionalValue("quarkus.automatiko.async.callback.auth-client-id", String.class)
                        .orElse(null);
                String clientSecret = config
                        .getOptionalValue("quarkus.automatiko.async.callback.auth-client-secret", String.class)
                        .orElse(null);
                String refreshToken = config
                        .getOptionalValue("quarkus.automatiko.async.callback.auth-refresh-token", String.class)
                        .orElse(null);
                String refreshUrl = config
                        .getOptionalValue("quarkus.automatiko.async.callback.auth-refresh-url", String.class)
                        .orElse(null);
                String scope = config
                        .getOptionalValue("quarkus.automatiko.async.callback.auth-scope", String.class)
                        .orElse(null);

                accessToken = HttpAuthorization.get().token(clientId,
                        clientId,
                        clientSecret,
                        refreshToken,
                        refreshUrl,
                        scope,
                        is -> {
                            try {
                                return mapper.readValue(is, Map.class);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

            map.put("Authorization", "Bearer " + accessToken);
        } else if (authType.equalsIgnoreCase("custom")) {
            String name = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-custom-name", String.class)
                    .orElse(null);
            String value = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-custom-value", String.class)
                    .orElse(null);

            if (name != null && value != null) {
                map.put(name, value);
            }
        } else if (authType.equalsIgnoreCase("on-behalf")) {
            String name = config
                    .getOptionalValue("quarkus.automatiko.async.callback.auth-on-behalf-name", String.class)
                    .orElse("Authorization");

            if (incomingHeaders.containsKey(name)) {
                map.put(name, incomingHeaders.get(name));
            }
        }
        return map;
    }
}
