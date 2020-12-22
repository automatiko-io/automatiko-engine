package io.automatik.engine.quarkus.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.automatik.engine.api.codegen.AutomatikConfigProperties;

public class AutomatikConfigSource implements ConfigSource {

    public static final String NAME = "AutomatikConfigSource";

    private List<AutomatikConfigProperties> found = null;

    public AutomatikConfigSource() {

    }

    @Override
    public Map<String, String> getProperties() {
        load();
        Map<String, String> properties = new LinkedHashMap<String, String>();
        if (found == null) {
            return properties;
        }
        for (AutomatikConfigProperties p : found) {
            if (p.getProperties() != null) {
                properties.putAll(p.getProperties());
            }
        }

        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        load();
        if (found == null) {
            return null;
        }

        return found.stream().map(p -> p.getProperty(propertyName)).filter(v -> v != null).findFirst().orElse(null);

    }

    @Override
    public String getName() {
        return NAME;
    }

    private void load() {
        if (found == null) {
            try {
                ServiceLoader<AutomatikConfigProperties> loader = ServiceLoader.load(AutomatikConfigProperties.class);
                found = StreamSupport.stream(loader.spliterator(), false).collect(Collectors.toList());

                found.add(new AutomatikConfigProperties() {
                    private Map<String, String> values = new LinkedHashMap<>();

                    @Override
                    public String getProperty(String name) {
                        if ("mp.openapi.extensions.smallrye.operationIdStrategy".equals(name)) {
                            return "METHOD";
                        }
                        return null;
                    }

                    @Override
                    public Map<String, String> getProperties() {
                        if (values.isEmpty()) {
                            values.put("mp.openapi.extensions.smallrye.operationIdStrategy", "METHOD");
                        }

                        return values;
                    }
                });
            } catch (Throwable e) {
            }
        }
    }
}
