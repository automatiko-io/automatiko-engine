package io.automatiko.engine.quarkus;

import io.automatiko.engine.api.config.DatabasePersistenceConfig;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
@ConfigProperties(prefix = "quarkus.automatiko.persistence.db")
public class DatabasePersistenceRuntimeConfig extends DatabasePersistenceConfig {

}
