package io.automatiko.engine.quarkus.operator.ops;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Gettable;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.fabric8.kubernetes.client.dsl.Resource;

@ApplicationScoped
public class CustomResourceOperations {

    @Inject
    KubernetesClient kube;

    @SuppressWarnings({ "rawtypes" })
    public void updateCustomResource(CustomResource<?, ?> resource) {

        Replaceable r = kube.resource(resource).inNamespace(resource.getMetadata().getNamespace())
                .lockResourceVersion(resource.getMetadata().getResourceVersion());
        r.replace();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void updateCustomResourceStatus(CustomResource<?, ?> resource) {

        Resource r = kube.resources(resource.getClass()).inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getMetadata().getName());

        CustomResource fromServer = (CustomResource) ((Gettable) r.fromServer()).get();

        fromServer.setStatus(resource.getStatus());

        r.patchStatus();

    }

    @SuppressWarnings({ "rawtypes" })
    public void deleteCustomResource(CustomResource<?, ?> resource) {

        Resource r = kube.resources(resource.getClass()).inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getMetadata().getName());
        r.delete();

    }
}
