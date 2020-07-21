package $Package$;

import io.automatik.engine.api.Config;

public class Module {

    private static final Config config =
            new io.automatik.engine.api.StaticConfig(
                    new io.automatik.engine.workflow.StaticProcessConfig(
                            new $WorkItemHandlerConfig$(),
                            new $ProcessEventListenerConfig$()));

    public Config config() {
        return config;
    }
}
