package io.automatiko.engine.quarkus.operator.ops;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class SecretOperations {

    @Inject
    KubernetesClient kube;

    public void createSecret(CustomResource<?, ?> resource, String name, Map<String, String> values) {

        SecretBuilder builder = new SecretBuilder().withNewMetadata().withName(name).endMetadata();

        for (Entry<String, String> entry : values.entrySet()) {
            builder.addToData(entry.getKey(),
                    Base64.getEncoder().encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }

        this.kube.secrets().inNamespace(resource.getMetadata().getNamespace()).create(builder.build());
    }

    public void createSecret(CustomResource<?, ?> resource, String name, String key, String value) {

        SecretBuilder builder = new SecretBuilder().withNewMetadata().withName(name).endMetadata();

        builder.addToData(key, Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));

        this.kube.secrets().inNamespace(resource.getMetadata().getNamespace()).create(builder.build());
    }

    public void deleteSecret(CustomResource<?, ?> resource, String name) {

        Secret secret = new SecretBuilder().withNewMetadata().withName(name).endMetadata()
                .build();

        this.kube.secrets().inNamespace(resource.getMetadata().getNamespace()).delete(secret);
    }
}
