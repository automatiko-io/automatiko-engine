package io.automatiko.engine.quarkus.operator.ops;

import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class ConfigMapOperations {

    @Inject
    KubernetesClient kube;

    public void createConfigMap(CustomResource<?, ?> resource, String name, Map<String, String> values) {

        ConfigMapBuilder builder = new ConfigMapBuilder().withNewMetadata().withName(name).endMetadata();

        for (Entry<String, String> entry : values.entrySet()) {
            builder.addToData(entry.getKey(), entry.getValue());
        }

        this.kube.configMaps().inNamespace(resource.getMetadata().getNamespace()).create(builder.build());
    }

    public void createConfigMap(CustomResource<?, ?> resource, String name, String key, String value) {

        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName(name).endMetadata().addToData(name, value)
                .build();

        this.kube.configMaps().inNamespace(resource.getMetadata().getNamespace()).create(configMap);
    }

    public void deleteConfigMap(CustomResource<?, ?> resource, String name) {

        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName(name).endMetadata()
                .build();

        this.kube.configMaps().inNamespace(resource.getMetadata().getNamespace()).delete(configMap);
    }
}
