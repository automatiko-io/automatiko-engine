package io.automatiko.engine.addons.persistence.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperCustomizer {

    void customize(ObjectMapper objectMapper);

}
