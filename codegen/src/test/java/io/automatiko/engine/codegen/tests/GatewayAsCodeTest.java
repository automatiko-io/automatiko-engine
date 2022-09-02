
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

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
import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.JoinNodeBuilder;
import io.automatiko.engine.workflow.builder.SplitNodeBuilder;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;

public class GatewayAsCodeTest extends AbstractCodegenTest {

    @BeforeAll
    public static void prepare() {
        LambdaParser.parseLambdas("src/test/java/" + GatewayAsCodeTest.class.getCanonicalName().replace(".", "/") + ".java",
                md -> true);
    }

    @AfterAll
    public static void clear() {

        BuilderContext.clear();
    }

    @Test
    public void testExclusiveGatewayStartToEnd() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ExclusiveSplit", "test workflow with xor gateway")
                .dataObject("x", String.class)
                .dataObject("y", String.class);

        SplitNodeBuilder split = builder.start("start here").then()
                .log("execute script", "Hello world").thenSplit("gateway");

        split.when("x != null").log("first branch", "first branch").then().end("end");

        split.when("y != null").log("second branch", "second branch").then().end("error");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ExclusiveSplit");

        Map<String, Object> params = new HashMap<>();
        params.put("x", "First");
        params.put("y", "None");
        Model m = p.createModel();
        m.fromMap(params);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
    }

    @Test
    public void testExclusiveGatewayStartToEndLambda() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ExclusiveSplit", "test workflow with xor gateway");
        String x = builder.dataObject(String.class, "x");
        String y = builder.dataObject(String.class, "y");

        SplitNodeBuilder split = builder.start("start here").then()
                .log("execute script", "Hello world").thenSplit("gateway");

        split.when(() -> x != null).log("first branch", "first branch").then().end("end");

        split.when(() -> y != null).log("second branch", "second branch").then().end("error");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ExclusiveSplit");

        Map<String, Object> params = new HashMap<>();
        params.put("x", "First");
        params.put("y", "None");
        Model m = p.createModel();
        m.fromMap(params);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
    }

    @Test
    public void testExclusiveGatewayWithJoin() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("ExclusiveSplit", "test workflow with xor gateway")
                .dataObject("x", String.class)
                .dataObject("y", String.class);

        SplitNodeBuilder split = builder.start("start here").then()
                .log("execute script", "Hello world").thenSplit("gateway");

        JoinNodeBuilder join = split.when("x != null").log("first branch", "first branch").thenJoin("join");

        split.when("y != null").log("second branch", "second branch").thenJoin("join");

        join.then().log("after join", "joined").then().end("done");

        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ExclusiveSplit");

        Map<String, Object> params = new HashMap<>();
        params.put("x", "First");
        params.put("y", "None");
        Model m = p.createModel();
        m.fromMap(params);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("x", "y");
    }

}
