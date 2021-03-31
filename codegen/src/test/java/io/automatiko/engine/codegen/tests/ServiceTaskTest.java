
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.automatiko.engine.api.Application;
import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessError;
import io.automatiko.engine.api.workflow.ProcessErrors;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.codegen.AbstractCodegenTest;
import io.automatiko.engine.codegen.process.ProcessCodegenException;
import io.automatiko.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatiko.engine.workflow.compiler.util.NodeLeftCountDownProcessEventListener;

public class ServiceTaskTest extends AbstractCodegenTest {

    @Test
    public void testBasicServiceProcessTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Hello john!");
    }

    @Test
    @Timeout(unit = TimeUnit.SECONDS, value = 10)
    public void testBasicServiceProcessTaskWithRetry() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessRetry.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("Print error", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);
        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        listener.waitTillCompleted();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("john");
    }

    @Test
    @Timeout(unit = TimeUnit.SECONDS, value = 10)
    public void testBasicServiceProcessTaskWithRetryDefaultLimit() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessRetryDefLimit.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("Print error", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);
        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        listener.waitTillCompleted();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("john");
    }

    @Test
    @Timeout(unit = TimeUnit.SECONDS, value = 10000)
    public void testBasicServiceProcessTaskWithRetrySuccessful() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessRetry.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("EndProcess", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);
        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();
        m.fromMap(Collections.singletonMap("s", "mary"));
        processInstance.updateVariables(m);

        listener.waitTillCompleted();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Hello mary!");
    }

    @Test
    @Timeout(unit = TimeUnit.SECONDS, value = 10000)
    public void testBasicServiceProcessTaskWithRetryAbort() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessRetry.bpmn2");
        assertThat(app).isNotNull();

        NodeLeftCountDownProcessEventListener listener = new NodeLeftCountDownProcessEventListener("EndProcess", 1);
        ((DefaultProcessEventListenerConfig) app.config().process().processEventListeners()).register(listener);
        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        listener.waitTillCompleted(1500);

        processInstance.abort();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testServiceProcessDifferentOperationsTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperations.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Goodbye Hello john!!");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testServiceProcessDifferentOperationsParallelTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperationsParallel.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        //parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ERROR);

        assertThat(processInstance.errors()).isPresent();
        assertThat(((ProcessErrors) processInstance.errors().get()).errors()).hasSize(2);

        parameters.put("s", "john");
        m.fromMap(parameters);
        processInstance.updateVariables(m);

        ((ProcessErrors) processInstance.errors().get()).retrigger();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().asString().contains("Goodbye").contains("Hello");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testServiceProcessDifferentOperationsParallelTaskRetriggerIndividually() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperationsParallel.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        //parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ERROR);

        assertThat(processInstance.errors()).isPresent();
        assertThat(((ProcessErrors) processInstance.errors().get()).errors()).hasSize(2);

        parameters.put("s", "john");
        m.fromMap(parameters);
        processInstance.updateVariables(m);

        ProcessErrors errors = (ProcessErrors) processInstance.errors().get();

        for (ProcessError error : errors.errors()) {

            errors.retrigger(error.failedNodeId());

        }
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().asString().contains("Goodbye").contains("Hello");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testServiceProcessDifferentOperationsParallelTaskSkip() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperationsParallel.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ERROR);

        assertThat(processInstance.errors()).isPresent();
        assertThat(((ProcessErrors) processInstance.errors().get()).errors()).hasSize(2);

        parameters.put("s", "john");
        m.fromMap(parameters);
        processInstance.updateVariables(m);

        ((ProcessErrors) processInstance.errors().get()).skip();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("john");
    }

    @Test
    public void testServiceProcessDifferentOperationsTaskFromAnotherNode() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessDifferentOperations.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessDifferentOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.startFrom("_A1EE8114-BF7B-4DAF-ABD7-62EEDCFAEFD4");

        assertThat(processInstance.startDate()).isNotNull();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Goodbye john!");
    }

    @Test
    public void testServiceProcessSameOperationsTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessSameOperations.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessSameOperations_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(1).containsKeys("s");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Hello Hello john!!");
    }

    @Test
    public void testBasicServiceProcessTaskMultiinstance() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessMI.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        List<String> list = new ArrayList<String>();
        list.add("first");
        list.add("second");
        List<String> listOut = new ArrayList<String>();

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("list", list);
        parameters.put("listOut", listOut);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(3).containsKeys("list", "s", "listOut");
        assertThat((List<String>) result.toMap().get("listOut")).isNotNull().hasSize(2).contains("Hello first!",
                "Hello second!");
    }

    @Test
    public void malformedShouldThrowException() throws Exception {
        assertThrows(ProcessCodegenException.class, () -> {
            generateCodeProcessesOnly("servicetask/ServiceProcessMalformed.bpmn2");
        });
    }

    @Test
    public void shouldInferMethodSignatureFromClass() throws Exception {
        // should no throw
        generateCodeProcessesOnly("servicetask/ServiceProcessInferMethod.bpmn2");
    }

    @Test
    public void testMultiParamServiceProcessTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/MultiParamServiceProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        parameters.put("x", "doe");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("s", "x");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Hello (first and lastname) john doe!");
    }

    @Test
    public void testMultiParamConstantServiceProcessTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/MultiParamServiceProcessConstant.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("s", "john");
        parameters.put("x", "doe");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("s", "x");
        assertThat(result.toMap().get("s")).isNotNull().isEqualTo("Hello (first and lastname) john Test!");
    }

    @Test
    public void testMultiParamServiceProcessTaskNoOutput() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/MultiParamServiceProcessNoOutput.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("MultiParamServiceProcessNoOutput_1_0");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        parameters.put("age", 35);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(2).containsKeys("name", "age");

    }

    @Test
    public void testMultiParamServiceCustomResultProcessTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/MultiParamCustomResultServiceTask.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("services");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "john");
        parameters.put("age", 35);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(3).containsKeys("name", "age");

        assertThat(result.toMap().get("result")).isNotNull().isEqualTo("Hello john 35!");

    }

    @Test
    public void testOverloadedService() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessOverloaded.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcessOverloaded_1_0");
        ProcessInstance<?> processInstance = p.createInstance(p.createModel());
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicServiceProcessTaskMultiinstanceSequential() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/ServiceProcessMI-sequential.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");

        List<String> list = new ArrayList<String>();
        list.add("first");
        list.add("second");
        List<String> listOut = new ArrayList<String>();

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("list", list);
        parameters.put("listOut", listOut);
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
        Model result = (Model) processInstance.variables();
        assertThat(result.toMap()).hasSize(3).containsKeys("list", "s", "listOut");
        assertThat((List<String>) result.toMap().get("listOut")).isNotNull().hasSize(2).contains("Hello first!",
                "Hello second!");
    }

    @Test
    public void testBasicRestServiceProcessTask() throws Exception {

        Application app = generateCodeProcessesOnly("servicetask/RESTServiceProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.processes().processById("ServiceProcess");
    }

}
