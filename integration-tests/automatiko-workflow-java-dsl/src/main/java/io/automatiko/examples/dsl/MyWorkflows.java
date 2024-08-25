package io.automatiko.examples.dsl;

import static io.automatiko.engine.workflow.base.core.context.variable.Variable.INPUT_TAG;
import static io.automatiko.engine.workflow.base.core.context.variable.Variable.OUTPUT_TAG;

import io.automatiko.engine.api.Workflows;
import io.automatiko.engine.workflow.builder.JoinNodeBuilder;
import io.automatiko.engine.workflow.builder.RestServiceNodeBuilder;
import io.automatiko.engine.workflow.builder.ServiceNodeBuilder;
import io.automatiko.engine.workflow.builder.SplitNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

@Workflows
public class MyWorkflows {

    public WorkflowBuilder helloWorld() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("hello", "Sample Hello World workflow", "1")
                .withKeepEndOfInstanceStrategy()
                .customAttribute("endOfInstanceStrategy", "keep")
                .dataObject("name", String.class);

        builder
                .start("start here").then()
                .log("say hello", "Hello world").then()
                .end("done");

        return builder;
    }

    public WorkflowBuilder split() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("split", "Sample workflow with exclusive split");

        String x = builder.dataObject(String.class, "x");
        String y = builder.dataObject(String.class, "y");

        SplitNodeBuilder split = builder.start("start here").then()
                .log("log values", "X is {} and Y is {}", "x", "y")
                .thenSplit("split");

        split.when(() -> x != null).log("first branch", "first branch").then().end("done on first");

        split.when(() -> y != null).log("second branch", "second branch").then().end("done on second");

        return builder;
    }

    public WorkflowBuilder splitAndJoin() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("splitAndJoin", "Sample workflow with exclusive split and join");

        String x = builder.dataObject(String.class, "x");
        String y = builder.dataObject(String.class, "y");

        SplitNodeBuilder split = builder.start("start here").then()
                .log("log values", "X is {} and Y is {}", "x", "y")
                .thenSplit("split");

        JoinNodeBuilder join = split.when(() -> x != null).log("first branch", "first branch").thenJoin("join");

        split.when(() -> y != null).log("second branch", "second branch").thenJoin("join");

        join.then().log("after join", "joined").then().end("done");

        return builder;
    }

    public WorkflowBuilder serviceCall() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("service", "Sample workflow calling local service", "1")
                .dataObject("name", String.class, INPUT_TAG)
                .dataObject("greeting", String.class, OUTPUT_TAG)
                .dataObject("age", Integer.class);

        ServiceNodeBuilder service = builder.start("start here").then()
                .log("execute script", "Hello world").then()
                .service("greet");

        service.toDataObject("greeting",
                service.type(MyService.class).sayHello(service.fromDataObject("name"))).then()
                .end("that's it");

        return builder;
    }

    public WorkflowBuilder restServiceCall() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("restService", "Sample workflow calling REST service", "1")
                .dataObject("petId", Long.class, INPUT_TAG)
                .dataObject("pet", Object.class, OUTPUT_TAG);

        RestServiceNodeBuilder service = builder.start("start here").then()
                .log("execute script", "Hello world").then()
                .restService("get pet from the store");

        service.toDataObject("pet",
                service.openApi("/api/swagger.json").operation("getPetById").fromDataObject("petId")).then()
                .end("that's it");

        service.onError("404").then().log("log error", "Unable to find pet with id {}", "petId").then().end("not found");

        return builder;
    }

    public WorkflowBuilder userTasks() {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("userTasks", "Sample workflow with user tasks")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then()
                .user("First Task").description("A description of the task")
                .users("john").outputToDataObject("value", "y").then()
                .user("Second Task").users("john").dataObjectAsInput("x").then()
                .end("done");

        return builder;
    }

}
