package org.acme.io;

import org.acme.Person;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.io.OutputConverter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PersonOutputConverter implements OutputConverter<Person, String> {

    @Inject
    ObjectMapper mapper;

    @Override
    public String convert(Person value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
