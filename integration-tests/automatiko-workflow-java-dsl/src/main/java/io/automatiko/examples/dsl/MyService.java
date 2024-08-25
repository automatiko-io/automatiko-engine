package io.automatiko.examples.dsl;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyService {

    public String greet() {
        System.out.println("Greet method called");
        return "hi";
    }

    public String sayHello(String name) {
        System.out.println("sayHello method called");
        return "Hello " + name;
    }

    public Map<String, String> collectData(String key, int min) {
        return Map.of();
    }
}
