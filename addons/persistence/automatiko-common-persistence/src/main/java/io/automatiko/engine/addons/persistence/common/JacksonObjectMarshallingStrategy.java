package io.automatiko.engine.addons.persistence.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.automatiko.engine.api.marshalling.ObjectMarshallingStrategy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.process.executable.core.ServerlessExecutableProcess;

public class JacksonObjectMarshallingStrategy implements ObjectMarshallingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(JacksonObjectMarshallingStrategy.class);

    protected ObjectMapper mapper;

    private boolean usePolomorfic = true;

    public JacksonObjectMarshallingStrategy(Process<?> process) {
        if (((AbstractProcess<?>) process).process() instanceof ServerlessExecutableProcess) {
            this.usePolomorfic = false;
        }
        this.mapper = new ObjectMapper();

        if (usePolomorfic) {
            mapper.activateDefaultTyping(
                    BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(), DefaultTyping.EVERYTHING,
                    As.PROPERTY);
        }
        mapper.registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }

    @Override
    public boolean accept(Object object) {

        return true;
    }

    @Override
    public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {
        return log(mapper.writeValueAsBytes(object));
    }

    @Override
    public Object unmarshal(String dataType, Context context, ObjectInputStream is, byte[] object,
            ClassLoader classloader) throws IOException, ClassNotFoundException {
        if (object.length == 0) {
            return null;
        }
        if (usePolomorfic) {
            return mapper.readValue(log(object), Object.class);
        } else {
            return mapper.readTree(log(object));
        }
    }

    @Override
    public Context createContext() {
        return null;
    }

    protected byte[] log(byte[] data) {
        logger.debug("Variable content:: {}", new String(data, StandardCharsets.UTF_8));

        return data;
    }

    public ObjectMapper mapper() {
        return mapper;
    }
}
