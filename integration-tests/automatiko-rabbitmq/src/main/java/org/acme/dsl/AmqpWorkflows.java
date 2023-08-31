package org.acme.dsl;

import org.acme.Person;

import io.automatiko.engine.api.Workflows;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

@Workflows
public class AmqpWorkflows {

    public WorkflowBuilder personWorkflow() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("amqp",
                "Sample workflow that uses AMQPfor integration with other systems");
        Person person = builder.dataObject(Person.class, "person", Variable.INTERNAL_TAG);

        builder.startOnMessage("dslperson").connector("amqp").ackMode("post")
                .toDataObject("person")
                .then().log("person processed", "Here is a person {}", "person")
                .then().expression("change values", () -> {
                    person.setAge(person.getAge() + 10);
                    person.setName(person.getName().toUpperCase());
                })
                .then().endWithMessage("done").connector("amqp").fromDataObject("person");

        return builder;
    }
}
