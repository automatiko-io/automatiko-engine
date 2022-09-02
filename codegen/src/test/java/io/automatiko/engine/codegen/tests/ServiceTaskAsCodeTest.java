
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.LambdaParser;
import io.automatiko.engine.codegen.data.HelloService;
import io.automatiko.engine.codegen.data.Person;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.ServiceNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class ServiceTaskAsCodeTest extends AbstractCodegenTest {

    @BeforeAll
    public static void prepare() {
        LambdaParser.parseLambdas("src/test/java/" + ServiceTaskAsCodeTest.class.getCanonicalName().replace(".", "/") + ".java",
                md -> true);
    }

    @AfterAll
    public static void clear() {

        BuilderContext.clear();
    }

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

    @Test
    public void testBasicServiceProcessTaskInputAsExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class);

        builder.start("start here").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).hello(service.expressionAsInput(() -> "Mary"))).then()
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
        assertThat(result.toMap().get("greeting")).isNotNull().isEqualTo("Hello Mary!");
    }

    @Test
    public void testBasicServiceProcessTaskInputAsLiteral() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class);

        builder.start("start here").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).hello(service.literalAsInput("Mary"))).then()
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
        assertThat(result.toMap().get("greeting")).isNotNull().isEqualTo("Hello Mary!");
    }

    @Test
    public void testBasicServiceProcessTaskOutputExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("person", Person.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObjectField("person",
                service.type(HelloService.class).hello(service.literalAsInput("Mary")), "name").then()
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
        assertThat(result.toMap()).hasSize(2).containsKeys("name", "person");
        assertThat(result.toMap().get("person")).isNotNull().extracting("name").isEqualTo("Hello Mary!");
    }

    @Test
    public void testBasicServiceProcessTaskOutputAppendToList() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greetings", List.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.appendToDataObjectField("greetings",
                service.type(HelloService.class).hello(service.literalAsInput("Mary"))).then()
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
        assertThat(result.toMap()).hasSize(2).containsKeys("name", "greetings");
        assertThat(result.toMap().get("greetings")).isNotNull().asList().hasSize(1).contains("Hello Mary!");
    }

    @Test
    public void testBasicServiceProcessTaskOutputRemoveToList() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greetings", List.class);

        builder.start("start here").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.removeFromDataObjectField("greetings",
                service.type(HelloService.class).hello(service.literalAsInput("Mary"))).then()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();

        List<String> greetings = new ArrayList<>();
        greetings.add("Hello Mary!");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        parameters.put("greetings", greetings);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("name", "greetings");
        assertThat(result.toMap().get("greetings")).isNotNull().asList().hasSize(0);
    }
}
