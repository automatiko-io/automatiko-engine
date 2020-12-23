package $Package$;

import io.automatiko.engine.api.Config;

public class Module {

    private static final Config config =
            new io.automatiko.engine.api.StaticConfig(
                    new io.automatiko.engine.workflow.StaticProcessConfig(
                            new $WorkItemHandlerConfig$(),
                            new $ProcessEventListenerConfig$()));

    public Config config() {
        return config;
    }
}
