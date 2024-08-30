package io.automatiko.examples.dsl;

import io.automatiko.engine.api.Workflows;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

@Workflows(category = "User oriented workflow example", categoryDescription = "Sample workflows that utilize user tasks to assign work to human actors", resourcePathPrefix = "/samples")
public class ExtraWorkflows {

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
