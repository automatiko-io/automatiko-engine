
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.LambdaParser;
import io.automatiko.engine.codegen.process.ProcessNodeLocator;
import io.automatiko.engine.workflow.AbstractProcess;
import io.automatiko.engine.workflow.builder.BuilderContext;
import io.automatiko.engine.workflow.builder.WorkflowBuilder;
import io.automatiko.engine.workflow.process.core.node.FaultNode;

public class ErrorEndAsCodeTest extends AbstractCodegenTest {

    @BeforeAll
    public static void prepare() {
        LambdaParser.parseLambdas(
                "src/test/java/" + ErrorEndAsCodeTest.class.getCanonicalName().replace(".", "/") + ".java",
                md -> true);
    }

    @AfterAll
    public static void clear() {

        BuilderContext.clear();
    }

    @Test
    public void testBasicErrorEndEvent() throws Exception {

        WorkflowBuilder builder = WorkflowBuilder.newWorkflow("samples", "test workflow with error end event")
                .dataObject("a", Integer.class)
                .dataObject("b", String.class);

        builder.start("start here").then().log("Log", "Running {} {}", "a", "b")
                .then().endWithError("error").error("error", "409").expressionAsInput(String.class, () -> "error");
        Application app = generateCode(List.of(builder.get()));
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("samples");

        Collection<FaultNode> errors = ProcessNodeLocator.findFaultNodes(((AbstractProcess<?>) p).process(),
                ((WorkflowProcess) ((AbstractProcess<?>) p).process()).getNodes()[0]);
        assertThat(errors).hasSize(1);

        Model m = p.createModel();
        m.fromMap(Map.of("a", 1, "b", "test"));
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
        assertThat(processInstance.abortCode()).isEqualTo("409");
        assertThat(processInstance.abortData()).isEqualTo("error");

    }

}
