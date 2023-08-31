package org.acme.io;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.acme.Person;
import org.eclipse.microprofile.reactive.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.io.InputConverter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersonInputConverter implements InputConverter<Person> {

    @Inject
    ObjectMapper mapper;

    @Override
    public Person convert(Object input) {

        try {
            Message<?> msg = (Message<?>) input;

            Object payload = msg.getPayload();

            if (payload instanceof String) {
                return mapper.readValue((String) msg.getPayload(), Person.class);
            } else {
                return mapper.readValue((byte[]) msg.getPayload(), Person.class);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

}
