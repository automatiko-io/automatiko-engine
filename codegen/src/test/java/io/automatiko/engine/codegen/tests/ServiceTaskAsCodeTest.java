
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
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
import io.automatiko.engine.codegen.data.PersonWithList;
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
        assertThat(result.toMap().get("greetings")).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(1)
                .contains("Hello Mary!");
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
        assertThat(result.toMap().get("greetings")).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(0);
    }

    @Test
    public void testBasicServiceProcessTaskRepeated() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class)
                .dataObject("output", List.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("outItem",
                service.type(HelloService.class).hello(service.fromDataObject("item"))).repeat("input", "item", "output")
                .endRepeatAndThen()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", List.of("one", "two", "three"));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("input", "output");

        assertThat(result.toMap().get("output")).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).contains("Hello one!",
                "Hello two!", "Hello three!");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedWithNames() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class)
                .dataObject("output", List.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).hello(service.fromDataObject("name")))
                .repeat("input", "name", "output", "greeting").endRepeatAndThen()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", List.of("one", "two", "three"));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("input", "output");

        assertThat(result.toMap().get("output")).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).contains("Hello one!",
                "Hello two!", "Hello three!");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedInputOnly() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.repeat("input").type(HelloService.class).hello(service.fromDataObject("item"));
        service.endRepeatAndThen().end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", List.of("one", "two", "three"));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("input");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedInputOnlyExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.repeat(() -> java.util.List.of("one", "two", "three"), "name").type(HelloService.class)
                .hello(service.fromDataObject("name"));
        service.endRepeatAndThen().end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("input");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class);

        List<?> output = builder.dataObject(List.class, "output");

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("outItem",
                service.type(HelloService.class).hello(service.fromDataObject("item")))
                .repeat(() -> java.util.List.of("one", "two", "three"), () -> output).endRepeatAndThen()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("input", "output");

        assertThat(result.toMap().get("output")).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).contains("Hello one!",
                "Hello two!", "Hello three!");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedNestedExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class);

        PersonWithList person = builder.dataObject(PersonWithList.class, "person");

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("outItem",
                service.type(HelloService.class).hello(service.fromDataObject("item")))
                .repeat(() -> java.util.List.of("one", "two", "three"), () -> person.getStringList())
                .onError(RuntimeException.class).retry(5, TimeUnit.SECONDS, 3)
                .then()
                .end("error handled");
        service.endRepeatAndThen().end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new PersonWithList("test", 0, false, new ArrayList<>(), null, null, null));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("input", "person");

        assertThat(result.toMap().get("person")).isNotNull().extracting("stringList")
                .asInstanceOf(InstanceOfAssertFactories.LIST).contains("Hello one!",
                        "Hello two!", "Hello three!");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedErrorHandling() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class);

        PersonWithList person = builder.dataObject(PersonWithList.class, "person");

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("outItem",
                service.type(HelloService.class).helloEverySecondFailed(service.fromDataObject("item")))
                .repeat(() -> java.util.List.of("one", "two", "three"), () -> person.getStringList())
                .onError(RuntimeException.class).retry(2, TimeUnit.SECONDS, 3)
                .then()
                .end("error handled");
        service.endRepeatAndThen()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();
        //        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("that's it", 1);
        //        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);
        //
        //        Process<? extends Model> p = app.processes().processById("ServiceProcess");
        //
        //        Model m = p.createModel();
        //        Map<String, Object> parameters = new HashMap<>();
        //        parameters.put("person", new PersonWithList("test", 0, false, new ArrayList<>(), null, null, null));
        //        m.fromMap(parameters);
        //
        //        ProcessInstance<?> processInstance = p.createInstance(m);
        //        processInstance.start();
        //
        //        boolean completed = listener.waitTillCompleted(5000);
        //        assertThat(completed).isTrue();
        //
        //        assertThat(processInstance.startDate()).isNotNull();
        //        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        //        Model result = (Model) processInstance.variables();
        //        assertThat(result.toMap()).hasSize(2).containsKeys("input", "person");
        //
        //        assertThat(result.toMap().get("person")).isNotNull().extracting("stringList").asList().contains("Hello one!",
        //                "Hello two!", "Hello three!");
    }

    @Test
    public void testBasicServiceProcessTaskRepeatedMultipleArgs() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("input", List.class)
                .dataObject("output", List.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "input", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + input").then();

        ServiceNodeBuilder service = builder.service("greet");

        Person p = new Person();

        service.toDataObject("outItem",
                service.type(HelloService.class).helloOutput(service.expressionAsInput(() -> p.getName()),
                        service.expressionAsInput(() -> p.getAge())))
                .repeat("input", "p", "output").endRepeatAndThen()
                .end("that's it");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> process = app.processes().processById("ServiceProcess");

        Model m = process.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("input", List.of(new Person("one", 10), new Person("two", 20), new Person("three", 30)));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = process.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("input", "output");

        assertThat(result.toMap().get("output")).isNotNull().asInstanceOf(InstanceOfAssertFactories.LIST).contains(
                "Hello one 10!", "Hello two 20!",
                "Hello three 30!");
    }

    @Test
    public void testBasicServiceProcessTaskWithVarargsMissing() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "name", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + name").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).helloWithVarargs(service.fromDataObject("name")))
                .then()
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
        assertThat(result.toMap().get("greeting")).isNotNull().isEqualTo("Hello john []!");
    }

    @Test
    public void testBasicServiceProcessTaskWithVarargsSingle() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "name", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + name").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).helloWithVarargs(service.fromDataObject("name"),
                        service.literalAsInput("aaaa")))
                .then()
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
        assertThat(result.toMap().get("greeting")).isNotNull().isEqualTo("Hello john [aaaa]!");
    }

    @Test
    public void testBasicServiceProcessTaskWithVarargsMany() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ServiceProcess", "test workflow as code")
                .dataObject("name", String.class)
                .dataObject("greeting", String.class);

        builder.start("start here").then()
                .log("execute script", "Hello world {}{}", "name", "\"!\"").then()
                .printout("execute script", "\"Hello world \" + name").then();

        ServiceNodeBuilder service = builder.service("greet");

        service.toDataObject("greeting",
                service.type(HelloService.class).helloWithVarargs(service.fromDataObject("name"),
                        service.literalAsInput("aaaa"),
                        service.literalAsInput("vvv")))
                .then()
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
        assertThat(result.toMap().get("greeting")).isNotNull().isEqualTo("Hello john [aaaa, vvv]!");
    }
}
