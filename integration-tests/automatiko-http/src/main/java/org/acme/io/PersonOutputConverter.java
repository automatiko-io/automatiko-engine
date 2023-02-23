package org.acme.io;

import java.io.StringWriter;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.acme.Person;

import io.automatiko.engine.api.io.OutputConverter;

@ApplicationScoped
public class PersonOutputConverter implements OutputConverter<Person, String> {

    JAXBContext context;

    @PostConstruct
    public void setup() {
        try {
            context = JAXBContext.newInstance(Person.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String convert(Person value) {
        StringWriter writer = new StringWriter();
        try {
            context.createMarshaller().marshal(value, writer);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

}
