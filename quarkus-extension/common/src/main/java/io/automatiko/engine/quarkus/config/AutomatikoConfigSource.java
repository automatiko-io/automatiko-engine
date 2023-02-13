package io.automatiko.engine.quarkus.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.automatiko.engine.api.codegen.AutomatikoConfigProperties;

public class AutomatikoConfigSource implements ConfigSource {

    public static final String NAME = "AutomatikoConfigSource";

    private List<AutomatikoConfigProperties> found = null;

    public AutomatikoConfigSource() {

    }

    @Override
    public Map<String, String> getProperties() {
        load();
        Map<String, String> properties = new LinkedHashMap<String, String>();
        if (found == null) {
            return properties;
        }
        for (AutomatikoConfigProperties p : found) {
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

    private synchronized void load() {
        if (found == null) {
            try {
                ServiceLoader<AutomatikoConfigProperties> loader = ServiceLoader.load(AutomatikoConfigProperties.class);
                List<AutomatikoConfigProperties> tmp = StreamSupport.stream(loader.spliterator(), false)
                        .collect(Collectors.toList());

                tmp.add(new AutomatikoConfigProperties() {
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
                found = tmp;
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public Set<String> getPropertyNames() {
        load();
        if (found == null) {
            return Collections.emptySet();
        }

        return found.stream().flatMap(c -> c.getProperties().keySet().stream()).collect(Collectors.toSet());
    }
}
