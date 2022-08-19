
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.data.HelloService;
import io.automatiko.engine.workflow.builder.ServiceNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class ServiceTaskAsCodeTest extends AbstractCodegenTest {

    @Test
    public void testBasicServiceProcessTask() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "name", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + name").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).hello(service.fromDataObject("name"))).then()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("name", "greeting");
        assertThat(result.toMap().get("greeting")).isNotNull().isEqualTo("Hello john!");
    }

}
