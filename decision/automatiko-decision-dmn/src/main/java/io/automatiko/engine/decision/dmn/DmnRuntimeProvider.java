package io.automatiko.engine.decision.dmn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.io.impl.ByteArrayResource;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.io.impl.FileSystemResource;
import org.kie.api.io.ResourceConfiguration;
import org.kie.api.io.ResourceWithConfiguration;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.dmn.api.core.DMNCompiler;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.event.DMNRuntimeEventListener;
import org.kie.dmn.api.marshalling.DMNMarshaller;
import org.kie.dmn.backend.marshalling.v1x.DMNMarshallerFactory;
import org.kie.dmn.core.assembler.DMNAssemblerService;
import org.kie.dmn.core.assembler.DMNResource;
import org.kie.dmn.core.assembler.DMNResourceDependenciesSorter;
import org.kie.dmn.core.compiler.DMNCompilerConfigurationImpl;
import org.kie.dmn.core.compiler.DMNCompilerImpl;
import org.kie.dmn.core.compiler.DMNProfile;
import org.kie.dmn.core.compiler.profiles.ExtendedDMNProfile;
import org.kie.dmn.core.impl.DMNRuntimeImpl;
import org.kie.dmn.core.impl.DMNRuntimeKB;
import org.kie.dmn.feel.util.Either;
import org.kie.dmn.model.api.Definitions;

import io.automatiko.engine.api.io.Resource;

public class DmnRuntimeProvider {

    public static DMNRuntime fromClassPath(String... location) {

        return fromResources(Stream.of(location).map(l -> new org.drools.core.io.impl.ClassPathResource(l))
                .collect(Collectors.toList()))
                        .getOrElseThrow(e -> new RuntimeException("Error initalizing DMNRuntime", e));
    }

    public static DMNRuntime fromFiles(String... location) {

        return fromResources(Stream.of(location).map(l -> new org.drools.core.io.impl.FileSystemResource(l))
                .collect(Collectors.toList()))
                        .getOrElseThrow(e -> new RuntimeException("Error initalizing DMNRuntime", e));
    }

    public static DMNRuntime from(io.automatiko.engine.services.io.ByteArrayResource... resources) {

        return fromResources(Stream.of(resources).map(l -> new org.drools.core.io.impl.ByteArrayResource(l))
                .collect(Collectors.toList()))
                        .getOrElseThrow(e -> new RuntimeException("Error initalizing DMNRuntime", e));
    }

    public static DMNRuntime from(List<Resource> resources) {

        return fromResources(resources.stream().map(l -> convert(l)).collect(Collectors.toList()))
                .getOrElseThrow(e -> new RuntimeException("Error initalizing DMNRuntime", e));
    }

    private static Either<Exception, DMNRuntime> fromResources(Collection<org.kie.api.io.Resource> resources) {

        ExtendedDMNProfile profile = new ExtendedDMNProfile();
        DMNCompilerConfigurationImpl cc = new DMNCompilerConfigurationImpl();
        cc.addExtensions(profile.getExtensionRegisters());
        cc.addDRGElementCompilers(profile.getDRGElementCompilers());
        cc.addFEELProfile(profile);

        DMNCompiler dmnCompiler = new DMNCompilerImpl(cc);

        List<DMNResource> dmnResources = new ArrayList<>();
        for (org.kie.api.io.Resource r : resources) {
            Definitions definitions;
            try {
                definitions = getMarshaller(cc).unmarshal(r.getReader());
            } catch (IOException e) {
                return Either.ofLeft(e);
            }

            DMNResource dmnResource = new DMNResource(definitions, new ResourceWithConfiguration() {

                @Override
                public ResourceConfiguration getResourceConfiguration() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public org.kie.api.io.Resource getResource() {
                    return (org.kie.api.io.Resource) r;
                }

                @Override
                public Consumer<Object> getBeforeAdd() {
                    return null;
                }

                @Override
                public Consumer<Object> getAfterAdd() {
                    return null;
                }
            });
            dmnResources.add(dmnResource);
        }
        DMNAssemblerService.enrichDMNResourcesWithImportsDependencies(dmnResources, Collections.emptyList());
        List<DMNResource> sortedDmnResources = DMNResourceDependenciesSorter.sort(dmnResources);

        List<DMNModel> dmnModels = new ArrayList<>();
        for (DMNResource dmnRes : sortedDmnResources) {
            DMNModel dmnModel = dmnCompiler.compile(dmnRes.getDefinitions(), dmnRes.getResAndConfig().getResource(),
                    dmnModels);

            if (dmnModel != null) {
                dmnModels.add(dmnModel);
            } else {

                return Either.ofLeft(new IllegalStateException(
                        "Unable to compile DMN model for the resource " + dmnRes.getResAndConfig().getResource()));
            }
        }
        return Either.ofRight(new DMNRuntimeImpl(new DMNRuntimeKBStatic(dmnModels, Arrays.asList(profile))));
    }

    private static DMNMarshaller getMarshaller(DMNCompilerConfigurationImpl cc) {
        if (!cc.getRegisteredExtensions().isEmpty()) {
            return DMNMarshallerFactory.newMarshallerWithExtensions(cc.getRegisteredExtensions());
        } else {
            return DMNMarshallerFactory.newDefaultMarshaller();
        }
    }

    public static org.kie.api.io.Resource convert(Resource resource) {
        if (resource instanceof io.automatiko.engine.services.io.ClassPathResource) {
            return new ClassPathResource((io.automatiko.engine.services.io.ClassPathResource) resource);
        } else if (resource instanceof io.automatiko.engine.services.io.FileSystemResource) {
            return new FileSystemResource(
                    (io.automatiko.engine.services.io.FileSystemResource) resource);
        } else if (resource instanceof io.automatiko.engine.services.io.ByteArrayResource) {
            return new ByteArrayResource((io.automatiko.engine.services.io.ByteArrayResource) resource);
        }

        return null;
    }

    private static class DMNRuntimeKBStatic implements DMNRuntimeKB {

        private final List<DMNProfile> dmnProfiles;
        private final List<DMNModel> models;

        private DMNRuntimeKBStatic(Collection<DMNModel> models, Collection<DMNProfile> dmnProfiles) {
            this.models = Collections.unmodifiableList(new ArrayList<>(models));
            this.dmnProfiles = Collections.unmodifiableList(new ArrayList<>(dmnProfiles));
        }

        @Override
        public List<DMNModel> getModels() {
            return models;
        }

        @Override
        public DMNModel getModel(String namespace, String modelName) {
            return models.stream().filter(m -> m.getNamespace().equals(namespace) && m.getName().equals(modelName))
                    .findFirst().orElse(null);
        }

        @Override
        public DMNModel getModelById(String namespace, String modelId) {
            return models.stream()
                    .filter(m -> m.getNamespace().equals(namespace) && m.getDefinitions().getId().equals(modelId))
                    .findFirst().orElse(null);
        }

        @Override
        public List<DMNProfile> getProfiles() {
            return dmnProfiles;
        }

        @Override
        public List<DMNRuntimeEventListener> getListeners() {
            return Collections.emptyList();
        }

        @Override
        public ClassLoader getRootClassLoader() {
            return null;
        }

        @Override
        public InternalKnowledgeBase getInternalKnowledgeBase() {
            throw new UnsupportedOperationException();
        }

        @Override
        public KieRuntimeFactory getKieRuntimeFactory(String kieBaseName) {
            throw new UnsupportedOperationException();
        }
    }
}
