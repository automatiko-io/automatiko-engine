package io.automatiko.addons.usertasks.index.fs;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.googlecode.cqengine.persistence.support.serialization.PersistenceConfig;
import com.googlecode.cqengine.persistence.support.serialization.PojoSerializer;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class JacksonSerializer implements PojoSerializer<CQEngineUserTaskInfo> {

    private ObjectMapper mapper;

    public JacksonSerializer(Class<CQEngineUserTaskInfo> objectType, PersistenceConfig persistenceConfig) {
        this();
    }

    public JacksonSerializer() {

        this.mapper = new ObjectMapper();

        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(), DefaultTyping.EVERYTHING,
                As.PROPERTY);

        mapper.registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }

    @Override
    public byte[] serialize(CQEngineUserTaskInfo object) {

        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CQEngineUserTaskInfo deserialize(byte[] bytes) {
        try {
            return mapper.readValue(bytes, CQEngineUserTaskInfo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
