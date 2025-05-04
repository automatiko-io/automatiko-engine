
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.UserNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;
import io.automatiko.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class UserTaskAsCodeTest extends AbstractCodegenTest {

    private Policy<?> securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));
    private Policy<?> securityPolicyMary = SecurityPolicy.of(new StaticIdentityProvider("mary"));

    @BeforeAll
    public static void prepare() {
        LambdaParser.parseLambdas("src/test/java/" + UserTaskAsCodeTest.class.getCanonicalName().replace(".", "/") + ".java",
                md -> true);
    }

    @AfterAll
    public static void clear() {

        BuilderContext.clear();
    }

    @Test
    public void testBasicUserTaskProcess() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessInputAsExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .expressionAsInput("greeting", String.class, () -> "Hello")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello", workItems.get(0).getParameters().get("greeting"));
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessInputAsLiteral() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("greeting", "Hello")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello", workItems.get(0).getParameters().get("greeting"));
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessOutputToField() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class)
                .dataObject("person", Person.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("greeting", "Hello")
                .users("john").toDataObjectField("value", "person", "name")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello", workItems.get(0).getParameters().get("greeting"));
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsKey("person");
        assertThat(((Person) ((Model) processInstance.variables()).toMap().get("person")).getName()).isEqualTo("test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessOutputAppendToList() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class)
                .dataObject("data", List.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("greeting", "Hello")
                .users("john").appendToDataObjectField("value", "data")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello", workItems.get(0).getParameters().get("greeting"));
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsKey("data");
        assertThat(((Model) processInstance.variables()).toMap().get("data")).asList().hasSize(1).contains("test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessOutputRemoveFromList() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class)
                .dataObject("data", List.class, Variable.AUTO_INITIALIZED_TAG);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .literalAsInput("greeting", "Hello")
                .users("john").removeFromDataObjectField("value", "data")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        parameters.put("data", new ArrayList<>(List.of("test")));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello", workItems.get(0).getParameters().get("greeting"));
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsKey("data");
        assertThat(((Model) processInstance.variables()).toMap().get("data")).asList().hasSize(0);

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testUserTaskProcesWithAdditionalOnTimer() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        builder.additionalPathOnTimer("on timeout").every("R3/PT1S").then().log("Log timeout", "on timeout").then()
                .end("completed");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("on timeout", 3);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        processInstance.abort();
    }

    @Test
    public void testUserTaskProcesWithAdditionalOnTimerExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x").then().end("done");

        builder.additionalPathOnTimer("on timeout").everyFromExpression(() -> "R3/PT1S").then().log("Log timeout", "on timeout")
                .then()
                .end("completed");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("on timeout", 3);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        processInstance.abort();
    }

    @Test
    public void testBasicUserTaskProcessUsersAsExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users(() -> java.util.List.of("john")).outputToDataObject("value", "y")
                .then().user("SecondTask").users(() -> java.util.List.of("john", "mary")).dataObjectAsInput("x").then()
                .end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessRepeat() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        String item = "";

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .outputToDataObject("value", "y")
                .repeat(() -> java.util.List.of("john", "mary"))
                .users(() -> java.util.List.of(item))
                .then()
                .end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicyMary);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicyMary);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessRepeatCollectOutput() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("outputs", List.class)
                .dataObject("inputs", List.class);

        String item = "";

        builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .repeat("inputs", "item", "outputs")
                .user(() -> item).outputToDataObject("value", "outItem")
                .then()
                .end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        parameters.put("inputs", List.of("john", "mary"));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicyMary);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test2"), securityPolicyMary);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        assertThat(((Model) processInstance.variables()).toMap().get("outputs")).asList().contains("test", "test2");

    }

    @Test
    public void testBasicUserTaskProcessWithOnTimeout() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class);

        UserNodeBuilder userNode = builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x");
        userNode.onTimeout().after(500, TimeUnit.MILLISECONDS).then().log("log", "timer called").then().end("end on timeout");
        userNode.then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("end on timeout", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        // another instance to be completed by timeout
        m = p.createModel();
        parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        processInstance = p.createInstance(m);
        processInstance.start();

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessWithOnTimeoutExpression() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class)
                .dataObject("timeout", String.class);

        UserNodeBuilder userNode = builder.start("start here").then().user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x");
        userNode.onTimeout().afterFromExpression(() -> java.time.Duration.ofSeconds(1).toString()).then()
                .log("log", "timer called")
                .then().end("end on timeout");
        userNode.then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("end on timeout", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        parameters.put("timeout", "PT1S");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 10);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        // another instance to be completed by timeout
        m = p.createModel();
        parameters = new HashMap<>();
        parameters.put("x", 10);
        parameters.put("timeout", "PT1S");
        m.fromMap(parameters);

        processInstance = p.createInstance(m);
        processInstance.start();

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }

    @Test
    public void testBasicUserTaskProcessWithOnTimeoutExpressionRepeat() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", String.class)
                .dataObject("timeout", String.class);

        UserNodeBuilder userNode = builder.start("start here").then()
                .set("set x", "x", () -> 20).then()
                .user("FirstTask").description("Hello #{todayDate()} task")
                .users("john").outputToDataObject("value", "y")
                .then().user("SecondTask").users("john").dataObjectAsInput("x");
        userNode.onTimeout(false).everyFromExpression("timeout").then()
                .log("log", "timer called")
                .then().end("end on timeout");
        userNode.then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("end on timeout", 3);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        parameters.put("timeout", "R3/PT1S");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());
        assertEquals("", workItems.get(0).getDescription());
        assertThat(workItems.get(0).getParameters()).containsEntry("x", 20);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        // another instance to be completed by timeout
        m = p.createModel();
        parameters = new HashMap<>();
        parameters.put("x", 10);
        parameters.put("timeout", "R3/PT1S");
        m.fromMap(parameters);

        processInstance = p.createInstance(m);
        processInstance.start();

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        assertThat(((Model) processInstance.variables()).toMap()).containsEntry("y", "test");

        workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("secondtask", workItems.get(0).getName());

        boolean completed = listener.waitTillCompleted(5000);
        assertThat(completed).isTrue();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.abort();
    }

    @Test
    public void testBasicUserTaskProcessSetValue() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("UserTasksProcess", "test workflow with user task")
                .dataObject("x", Integer.class)
                .dataObject("y", Person.class);

        builder.start("start here").then().set("set y", "y", () -> new Person("john", 12)).then()
                .user("FirstTask").description("Hello #{todayDate()} task")
                .users("john")
                .then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x", 10);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertEquals(1, workItems.size());
        assertEquals("firsttask", workItems.get(0).getName());
        assertEquals("Hello " + BaseFunctions.todayDate() + " task", workItems.get(0).getDescription());

        processInstance.completeWorkItem(workItems.get(0).getId(), Map.of("value", "test"), securityPolicy);

        assertThat(((Model) processInstance.variables()).toMap().get("y")).isEqualTo(new Person("john", 12));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

    }
}
