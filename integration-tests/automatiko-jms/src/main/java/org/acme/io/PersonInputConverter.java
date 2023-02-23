package org.acme.io;

import java.io.StringReader;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.acme.Person;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.automatiko.engine.api.io.InputConverter;

@ApplicationScoped
public class PersonInputConverter implements InputConverter<Person> {

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
    public Person convert(Object input) {
        StringReader reader = new StringReader(((Message<String>) input).getPayload());
        try {
            return (Person) context.createUnmarshaller().unmarshal(reader);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

    }

}
