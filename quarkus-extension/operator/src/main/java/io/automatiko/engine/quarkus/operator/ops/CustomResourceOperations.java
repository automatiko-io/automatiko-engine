package io.automatiko.engine.quarkus.operator.ops;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.fabric8.kubernetes.client.dsl.StatusUpdatable;

@ApplicationScoped
public class CustomResourceOperations {

    @Inject
    KubernetesClient kube;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void updateCustomResource(CustomResource<?, ?> resource) {

        Replaceable r = kube.customResources(resource.getClass()).inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getMetadata().getName())
                .lockResourceVersion(resource.getMetadata().getResourceVersion());
        r.replace(resource);

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void updateCustomResourceStatus(CustomResource<?, ?> resource) {

        StatusUpdatable r = kube.customResources(resource.getClass()).inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getMetadata().getName());
        r.updateStatus(resource);

    }
}
