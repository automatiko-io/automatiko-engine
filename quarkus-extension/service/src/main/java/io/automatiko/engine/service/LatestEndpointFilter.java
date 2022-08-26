package io.automatiko.engine.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.workflow.MutableProcessInstances;
import io.automatiko.engine.api.workflow.Process;

public class LatestEndpointFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatestEndpointFilter.class);

    boolean enabled;

    String rootPath;

    Instance<Process<?>> availableProcesses;

    @Inject
    public LatestEndpointFilter(Instance<Process<?>> availableProcesses,
            @ConfigProperty(name = "quarkus.http.root-path") Optional<String> rootPathProp,
            @ConfigProperty(name = "quarkus.automatiko.service-route-to-latest") Optional<Boolean> enabled) {
        this.rootPath = rootPathProp.orElse("/");
        this.enabled = enabled.orElse(false);
        this.availableProcesses = availableProcesses;

    }

    @ServerRequestFilter(preMatching = true)
    public void routeToLatest(ContainerRequestContext requestContext) {
        if (!enabled) {
            return;
        }
        String path = requestContext.getUriInfo().getPath();

        LOGGER.debug("Request is going to path {}", path);

        if (!rootPath.equals("/")) {
            path = path.replace(rootPath, "");
        }

        if (!path.matches("^/v.*/.*")) {
            String[] pathElements = path.split("/");
            String processId = pathElements[1];

            LOGGER.debug("not versioned endpoint for process '{}'", processId);

            Map<String, Process<?>> processData = availableProcesses == null ? Collections.emptyMap()
                    : availableProcesses.stream().collect(Collectors.toMap(p -> p.id(), p -> p));

            if (processData.containsKey(processId)) {
                LOGGER.debug("endpoint for process '{}' exists without version, using it", processId);
                return;
            }

            String processInstanceId = null;

            if (pathElements.length > 2) {
                processInstanceId = pathElements[2];
            }

            List<Number> versions = new ArrayList<>();
            for (Process<?> process : processData.values()) {
                if (process.id().startsWith(processId + "_")) {

                    if (processInstanceId != null
                            && ((MutableProcessInstances<?>) process.instances()).exists(processInstanceId)) {
                        String latestVersionPrefix = "/v" + process.version().replace(".", "_");
                        if (!rootPath.equals("/")) {
                            latestVersionPrefix = rootPath + latestVersionPrefix;
                        }
                        String newPath = latestVersionPrefix + path;
                        LOGGER.debug("Rerouting to selected version for process '{}' on relative path '{}'", processId,
                                newPath);

                        requestContext.setRequestUri(URI.create(newPath));

                        return;
                    }

                    try {
                        if (process.version().contains(".")) {
                            versions.add(Double.parseDouble(process.version()));
                        } else {
                            versions.add(Integer.parseInt(process.version()));
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.debug("Unable to get number out of version '{}'", process.version());
                    }
                }
            }
            LOGGER.debug("Found versions for process '{}' are {}", processId, versions);
            if (!versions.isEmpty()) {
                Collections.sort(versions, Collections.reverseOrder());

                String latestVersionPrefix = "/v" + String.valueOf(versions.get(0)).replace(".", "_");
                if (!rootPath.equals("/")) {
                    latestVersionPrefix = rootPath + latestVersionPrefix;
                }
                String newPath = latestVersionPrefix + path;
                LOGGER.debug("Rerouting to latest version for process '{}' on relative path '{}'", processId, newPath);

                requestContext.setRequestUri(URI.create(newPath));
            }
        }
    }
}
