
package io.automatiko.engine.codegen.data;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;

public class HelloService {

    private int counter = 0;

    public String hello(String name) {
        System.out.println("Service invoked with " + name.toString() + " on service " + this.toString());
        return "Hello " + name + "!";
    }

    public JsonNode jsonHello(JsonNode person) {
        System.out.println("Service invoked with " + person + " on service " + this.toString());

        String retJsonStr = "{\"result\":\"Hello " + person.get("name").textValue() + "\"}";
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(retJsonStr);
        } catch (Exception e) {
            return null;
        }
    }

    public String goodbye(String name) {
        System.out.println("Service invoked with " + name.toString() + " on service " + this.toString());
        return "Goodbye " + name + "!";
    }

    public String helloMulti(String name, String lastName) {
        System.out.println("Service invoked with " + name + " and " + lastName + " on service " + this.toString());
        return "Hello (first and lastname) " + name.toString() + " " + lastName + "!";
    }

    public void helloNoOutput(String name, Integer age) {
        System.out.println("Service invoked with " + name.toString() + " " + age + " on service " + this.toString());

    }

    public String helloOutput(String name, Integer age) {
        System.out.println("Service invoked with " + name.toString() + " " + age + " on service " + this.toString());
        return "Hello " + name + " " + age + "!";
    }

    int count = 0;

    public void doSomething(String str1) {
        if (count > 0) {
            throw new RuntimeException("You should not be here");
        }
        count++;
    }

    public void doSomething(String str1, String str2) {

    }

    public String helloFailing(String name) {
        System.out.println("Calling failing service .... " + new Date());
        if (name == null) {
            throw new WorkItemExecutionError("404");
        } else if (name.equals("john")) {
            throw new WorkItemExecutionError("500");
        } else if (name.equals("mike")) {
            throw new WorkItemExecutionError("400");
        } else if (name.equals("shorty")) {
            throw new WorkItemExecutionError("410");
        }

        return hello(name);
    }

    public String helloEverySecondFailed(String name) {
        System.out.println("Service invoked with " + name.toString() + " on service " + this.toString() + " ---- " + counter++);
        if (counter % 2 != 0) {
            throw new RuntimeException("Test exception");
        }

        return "Hello " + name + "!";
    }
}
