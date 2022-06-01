package org.acme;

import io.automatiko.engine.api.Functions;

public class PersonFunctions implements Functions {

    public static Person personRewrite(Person person) {
        Person rewritten = new Person();

        rewritten.setAge(person.getAge() + 10);
        rewritten.setName(person.getName().toUpperCase());
        return rewritten;
    }
}
