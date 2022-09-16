
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.LambdaParser;
import io.automatiko.engine.codegen.data.Person;
import io.automatiko.engine.services.execution.BaseFunctions;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.DataObjectType;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class CallActivityTaskAsCodeTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));

    @BeforeAll
    public static void prepare() {
        LambdaParser.parseLambdas(
                "src/test/java/" + CallActivityTaskAsCodeTest.class.getCanonicalName().replace(".", "/") + ".java",
                md -> true);
    }

    @AfterAll
    public static void clear() {

        BuilderContext.clear();
    }

    @Test
    public void testBasicCallActivityTask() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class);

        Integer x = builderSub.dataObject(Integer.class, "x");

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("myString", "simple string")
                .literalAsInput("myNumber", 123)
                .expressionAsInput("calculated", Integer.class, () -> x * 10)
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow")
                .dataObject("a", Integer.class)
                .dataObject("b", String.class);

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .literalAsInput("x", 100).expressionAsInput("y", String.class, () -> "from expression")
                .outputToDataObject("y", "b").outputToDataObject("x", "a")
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 10);
        parameters.put("b", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();
        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 100);

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("a", "b");
        assertThat(result.toMap().get("b")).isNotNull().isEqualTo("test");
        assertThat(result.toMap().get("a")).isNotNull().isEqualTo(100);
    }

    @Test
    public void testBasicCallActivityTaskOutputToField() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class);

        Integer x = builderSub.dataObject(Integer.class, "x");

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("myString", "simple string")
                .literalAsInput("myNumber", 123)
                .expressionAsInput("calculated", Integer.class, () -> x * 10)
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow")
                .dataObject("a", Integer.class)
                .dataObject("b", String.class)
                .dataObject("person", Person.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .literalAsInput("x", 100).expressionAsInput("y", String.class, () -> "from expression")
                .toDataObjectField("y", "person", "name").outputToDataObject("x", "a")
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 10);
        parameters.put("b", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();
        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 100);

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(3).containsKeys("a", "b", "person");
        assertThat(result.toMap().get("b")).isNotNull().isEqualTo("b");
        assertThat(result.toMap().get("a")).isNotNull().isEqualTo(100);
        assertThat(result.toMap().get("person")).isNotNull().extracting("name").isEqualTo("test");
    }

    @Test
    public void testBasicCallActivityTaskOutputAppendToList() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class);

        Integer x = builderSub.dataObject(Integer.class, "x");

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("myString", "simple string")
                .literalAsInput("myNumber", 123)
                .expressionAsInput("calculated", Integer.class, () -> x * 10)
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow")
                .dataObject("a", Integer.class)
                .dataObject("b", String.class)
                .dataObject("data", List.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .literalAsInput("x", 100).expressionAsInput("y", String.class, () -> "from expression")
                .appendToDataObjectField("y", "data").outputToDataObject("x", "a")
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 10);
        parameters.put("b", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();
        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 100);

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(3).containsKeys("a", "b", "data");
        assertThat(result.toMap().get("b")).isNotNull().isEqualTo("b");
        assertThat(result.toMap().get("a")).isNotNull().isEqualTo(100);
        assertThat(result.toMap().get("data")).isNotNull().asList().hasSize(2).contains("from expression", "test");
    }

    @Test
    public void testBasicCallActivityTaskOutputRemoveFromList() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class);

        Integer x = builderSub.dataObject(Integer.class, "x");

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("myString", "simple string")
                .literalAsInput("myNumber", 123)
                .expressionAsInput("calculated", Integer.class, () -> x * 10)
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow")
                .dataObject("a", Integer.class)
                .dataObject("b", String.class)
                .dataObject("data", List.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .literalAsInput("x", 100).expressionAsInput("y", String.class, () -> "from expression")
                .removeFromDataObjectField("y", "data").outputToDataObject("x", "a")
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 10);
        parameters.put("b", "b");
        parameters.put("data", new ArrayList<>(List.of("test")));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();
        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 100);

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(3).containsKeys("a", "b", "data");
        assertThat(result.toMap().get("b")).isNotNull().isEqualTo("b");
        assertThat(result.toMap().get("a")).isNotNull().isEqualTo(100);
        assertThat(result.toMap().get("data")).isNotNull().asList().hasSize(0);
    }

    @Test
    public void testBasicCallActivityTaskRepeat() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class);

        Integer x = builderSub.dataObject(Integer.class, "x");

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .dataObjectAsInput("y")
                .literalAsInput("myNumber", 123)
                .expressionAsInput("calculated", Integer.class, () -> x * 10)
                .users("john").outputToDataObject("value", "y")
                .then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow")
                .dataObject("a", Integer.class)
                .dataObject("b", String.class);

        String item = null;

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .literalAsInput("x", 100).expressionAsInput("y", String.class, () -> item)
                .outputToDataObject("y", "b").outputToDataObject("x", "a")
                .repeat(() -> java.util.List.of("one", "two"))
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("a", 10);
        parameters.put("b", "b");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        assertThat(processInstance.subprocesses()).hasSize(2);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(2, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();

        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsKey("y");

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsKey("y");

        subProcessInstance = processInstance.subprocesses().iterator().next();

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("a", "b");
        assertThat(result.toMap().get("b")).isNotNull().isEqualTo("two");
        assertThat(result.toMap().get("a")).isNotNull().isEqualTo(100);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBasicCallActivityTaskRepeatWithOutput() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class)
                .dataObject("x", Integer.class);

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .dataObjectAsInput("y")
                .dataObjectAsInput("x")
                .users("john").outputToDataObject("value", "y")
                .then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow");

        List<String> outputs = builder.dataObject(List.class, "outputs");

        String item = "";

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .expressionAsInput("x", Integer.class, () -> item.hashCode())
                .expressionAsInput("y", String.class, () -> item)
                .repeat(() -> java.util.List.of("one", "two"), () -> outputs)
                .outputToDataObject("y", "outItem")
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        assertThat(processInstance.subprocesses()).hasSize(2);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(2, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();

        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsKey("y");

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "one"), securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsKey("y");

        subProcessInstance = processInstance.subprocesses().iterator().next();

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "two"), securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("outputs");
        assertThat(result.toMap().get("outputs")).isNotNull().asList().contains("one", "two");
    }

    @Test
    public void testBasicCallActivityTaskRepeatWithOutputExpression() throws Exception {
        WorkflowBuilder builderSub = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("y", String.class)
                .dataObject("x", Integer.class);

        builderSub.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .dataObjectAsInput("y")
                .dataObjectAsInput("x")
                .users("john").outputToDataObject("value", "y")
                .then().end("done");

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("subworkflow", "test workflow with subworkflow")
                .listDataObject("inputs", Person.class)
                .dataObject("outputs", new DataObjectType<List<String>>() {
                });

        Person person = new Person();

        builder.start("start here").then().subWorkflow("call other workflow").id("UserTasksProcess")
                .expressionAsInput("x", Integer.class, () -> person.getAge())
                .expressionAsInput("y", String.class, () -> person.getName())
                .repeat("inputs", "person", "outputs", "updatedName")
                .outputToDataObject("y", "updatedName")
                .then().end("done");
        Application app = generateCode(List.of(builder.get(), builderSub.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("subworkflow");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("inputs", java.util.List.of(new Person("john", 10), new Person("mary", 20)));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        assertThat(processInstance.subprocesses()).hasSize(2);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(2, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());

        ProcessInstance<?> subProcessInstance = processInstance.subprocesses().iterator().next();

        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsKey("y");

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "Johnny"), securityPolicy);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsKey("y");

        subProcessInstance = processInstance.subprocesses().iterator().next();

        subProcessInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "Marrry"), securityPolicy);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("inputs", "outputs");
        assertThat(result.toMap().get("outputs")).isNotNull().asList().contains("Johnny", "Marrry");
    }
}
