
package io.automatik.engine.workflow.bpmn2.canonical;

import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ABORTED;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static io.automatik.engine.api.runtime.process.ProcessInstance.STATE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.automatik.engine.api.auth.SecurityPolicy;
import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.api.definition.process.WorkflowProcess;
import io.automatik.engine.api.runtime.process.WorkItem;
import io.automatik.engine.api.runtime.process.WorkItemHandler;
import io.automatik.engine.api.workflow.ProcessConfig;
import io.automatik.engine.api.workflow.ProcessError;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.services.identity.StaticIdentityProvider;
import io.automatik.engine.services.uow.CollectingUnitOfWorkFactory;
import io.automatik.engine.services.uow.DefaultUnitOfWorkManager;
import io.automatik.engine.workflow.CachedWorkItemHandlerConfig;
import io.automatik.engine.workflow.DefaultProcessEventListenerConfig;
import io.automatik.engine.workflow.StaticProcessConfig;
import io.automatik.engine.workflow.base.core.resources.ClassPathResource;
import io.automatik.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.automatik.engine.workflow.base.instance.impl.demo.SystemOutWorkItemHandler;
import io.automatik.engine.workflow.bpmn2.BpmnProcess;
import io.automatik.engine.workflow.bpmn2.BpmnVariables;
import io.automatik.engine.workflow.bpmn2.JbpmBpmn2TestCase;
import io.automatik.engine.workflow.bpmn2.objects.TestWorkItemHandler;
import io.automatik.engine.workflow.compiler.canonical.ProcessMetaData;
import io.automatik.engine.workflow.compiler.canonical.ProcessToExecModelGenerator;
import io.automatik.engine.workflow.compiler.canonical.UserTaskModelMetaData;
import io.automatik.engine.workflow.marshalling.ProcessInstanceMarshaller;

public class ActivityGenerationModelTest extends JbpmBpmn2TestCase {

	private Path compilationOutcome;

	@AfterEach
	public void cleanup() throws IOException {
		if (compilationOutcome != null) {
			try (Stream<Path> walk = Files.walk(compilationOutcome)) {
				walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		}
	}

	@Test
	public void testMinimalProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-MinimalProcess.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("com.sample.MinimalProcess", content);

		Map<String, BpmnProcess> processes = createProcesses(classData, Collections.emptyMap());
		ProcessInstance<BpmnVariables> processInstance = processes.get("Minimal").createInstance();

		processInstance.start();

		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testProcessEmptyScript() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-ProcessEmptyScript.bpmn2")).get(0);

		assertThrows(IllegalStateException.class,
				() -> ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process()));

	}

	@Test
	public void testUserTaskProcessWithTaskModels() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);

		List<UserTaskModelMetaData> models = ProcessToExecModelGenerator.INSTANCE
				.generateUserTaskModel((WorkflowProcess) process.process());

		for (UserTaskModelMetaData metaData : models) {
			String content = metaData.generateInput();
			assertThat(content).isNotNull();
			log(content);

			content = metaData.generateOutput();
			assertThat(content).isNotNull();
			log(content);
		}
	}

	@Test
	public void testUserTaskProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.UserTaskProcess", content);
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();

		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Human Task", workItemHandler));
		ProcessInstance<BpmnVariables> processInstance = processes.get("UserTask").createInstance();

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("john", workItem.getParameter("ActorId"));
		processInstance.completeWorkItem(workItem.getId(), null, SecurityPolicy.of(new StaticIdentityProvider("john")));
		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testUserTaskWithParamProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTaskWithParametrizedInput.bpmn2"))
				.get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.UserTaskProcess", content);

		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();

		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Human Task", workItemHandler));
		ProcessInstance<BpmnVariables> processInstance = processes.get("UserTask").createInstance();

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("Executing task of process instance " + processInstance.id() + " as work item with Hello",
				workItem.getParameter("Description").toString().trim());
		processInstance.completeWorkItem(workItem.getId(), null, SecurityPolicy.of(new StaticIdentityProvider("john")));
		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testScriptMultilineExprProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-CallActivitySubProcess.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.SubProcessProcess", content);

		Map<String, BpmnProcess> processes = createProcesses(classData, Collections.emptyMap());
		ProcessInstance<BpmnVariables> processInstance = processes.get("SubProcess").createInstance();

		processInstance.start();

		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testExclusiveSplit() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-ExclusiveSplit.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.TestProcess", content);

		SystemOutWorkItemHandler workItemHandler = new SystemOutWorkItemHandler();

		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Email", workItemHandler));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "First");
		params.put("y", "Second");
		ProcessInstance<BpmnVariables> processInstance = processes.get("com.sample.test")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();

		assertEquals(STATE_COMPLETED, processInstance.status());

	}

	@Test
	public void testExclusiveSplitRetriggerAfterError() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-ExclusiveSplit.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.TestProcess", content);

		SystemOutWorkItemHandler workItemHandler = new SystemOutWorkItemHandler();

		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Email", workItemHandler));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "First1");
		params.put("y", "Second1");
		ProcessInstance<BpmnVariables> processInstance = processes.get("com.sample.test")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();

		assertEquals(STATE_ERROR, processInstance.status());

		Optional<ProcessError> errorOptional = processInstance.error();
		assertThat(errorOptional).isPresent();

		ProcessError error = errorOptional.get();
		assertThat(error.failedNodeId()).isEqualTo("_2");
		assertThat(error.errorMessage())
				.contains("XOR split could not find at least one valid outgoing connection for split Split");

		params.put("x", "First");
		processInstance.updateVariables(BpmnVariables.create(params));

		error.retrigger();
		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testInclusiveSplit() throws Exception {

		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-InclusiveSplit.bpmn2")).get(0);
		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.TestProcess", content);

		Map<String, BpmnProcess> processes = createProcesses(classData, Collections.emptyMap());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", 15);
		ProcessInstance<BpmnVariables> processInstance = processes.get("com.sample.test")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();

		assertEquals(STATE_COMPLETED, processInstance.status());

	}

	@Test
	public void testInclusiveSplitDefaultConnection() throws Exception {

		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-InclusiveGatewayWithDefault.bpmn2")).get(0);
		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.InclusiveGatewayWithDefaultProcess", content);

		Map<String, BpmnProcess> processes = createProcesses(classData, Collections.emptyMap());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("test", "c");
		ProcessInstance<BpmnVariables> processInstance = processes.get("InclusiveGatewayWithDefault")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();

		assertEquals(STATE_COMPLETED, processInstance.status());

	}

	@Test
	public void testParallelGateway() throws Exception {

		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-ParallelSplit.bpmn2")).get(0);
		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.TestProcess", content);

		Map<String, BpmnProcess> processes = createProcesses(classData, Collections.emptyMap());
		Map<String, Object> params = new HashMap<String, Object>();
		ProcessInstance<BpmnVariables> processInstance = processes.get("com.sample.test")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();

		assertEquals(STATE_COMPLETED, processInstance.status());

	}

	@Test
	public void testInclusiveSplitAndJoinNested() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-InclusiveSplitAndJoinNested.bpmn2")).get(0);
		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.TestProcess", content);

		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", 15);
		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Human Task", workItemHandler));
		ProcessInstance<BpmnVariables> processInstance = processes.get("com.sample.test")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		List<WorkItem> activeWorkItems = workItemHandler.getWorkItems();

		assertEquals(2, activeWorkItems.size());

		for (WorkItem wi : activeWorkItems) {
			processInstance.completeWorkItem(wi.getId(), null);
		}

		activeWorkItems = workItemHandler.getWorkItems();
		assertEquals(2, activeWorkItems.size());

		for (WorkItem wi : activeWorkItems) {
			processInstance.completeWorkItem(wi.getId(), null);
		}
		assertEquals(STATE_COMPLETED, processInstance.status());

	}

	@Test
	public void testInclusiveSplitAndJoinNestedWithBusinessKey() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-InclusiveSplitAndJoinNested.bpmn2")).get(0);
		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.TestProcess", content);

		String customId = "custom";

		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", 15);
		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Human Task", workItemHandler));
		ProcessInstance<BpmnVariables> processInstance = processes.get("com.sample.test").createInstance(customId,
				BpmnVariables.create(params));

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		ProcessInstance<BpmnVariables> loadedProcessInstance = processes.get("com.sample.test").instances()
				.findById(customId).orElse(null);
		assertThat(loadedProcessInstance).isNotNull();

		loadedProcessInstance.abort();

		assertEquals(STATE_ABORTED, processInstance.status());

	}

	@Test
	public void testWorkItemProcessWithVariableMapping() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-ServiceProcess.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.ServiceProcessProcess", content);

		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("s", "john");
		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("org.jbpm.bpmn2.objects.HelloService_hello_2_Handler", workItemHandler));
		ProcessInstance<BpmnVariables> processInstance = processes.get("ServiceProcess")
				.createInstance(BpmnVariables.create(params));

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);

		assertEquals("john", workItem.getParameter("Parameter"));

		processInstance.completeWorkItem(workItem.getId(), Collections.singletonMap("Result", "john doe"));

		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testBusinessRuleTaskProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-BusinessRuleTask.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);
	}

	@Test
	public void testServiceTaskProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-ServiceProcess.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		assertThat(metaData.getWorkItems()).hasSize(1).contains("org.jbpm.bpmn2.objects.HelloService_hello_2_Handler");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUserTaskProcessWithMarshalling() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		Map<String, String> classData = new HashMap<>();
		classData.put("org.drools.bpmn2.UserTaskProcess", content);
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();

		Map<String, BpmnProcess> processes = createProcesses(classData,
				Collections.singletonMap("Human Task", workItemHandler));
		ProcessInstance<BpmnVariables> processInstance = processes.get("UserTask")
				.createInstance(BpmnVariables.create(Collections.singletonMap("s", "test")));

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		ProcessInstanceMarshaller marshaller = new ProcessInstanceMarshaller();

		byte[] data = marshaller.marhsallProcessInstance(processInstance);
		assertNotNull(data);

		processInstance = (ProcessInstance<BpmnVariables>) marshaller.unmarshallProcessInstance(data, process);

		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("john", workItem.getParameter("ActorId"));
		processInstance.completeWorkItem(workItem.getId(), null, SecurityPolicy.of(new StaticIdentityProvider("john")));
		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	@Test
	public void testCallActivityProcess() throws Exception {
		BpmnProcess process = BpmnProcess.from(new ClassPathResource("PrefixesProcessIdCallActivity.bpmn2")).get(0);

		ProcessMetaData metaData = ProcessToExecModelGenerator.INSTANCE.generate((WorkflowProcess) process.process());
		String content = metaData.getGeneratedClassModel().toString();
		assertThat(content).isNotNull();
		log(content);

		assertThat(metaData.getSubProcesses()).hasSize(1).containsKey("SubProcess").containsValue("test.SubProcess");
	}

	/*
	 * Helper methods
	 */

	protected void log(String content) {
		logger.debug(content);
	}

	protected Map<String, BpmnProcess> createProcesses(Map<String, String> classData,
			Map<String, WorkItemHandler> handlers) throws Exception {
		List<JavaFileObject> sources = new ArrayList<JavaFileObject>();

		for (Entry<String, String> entry : classData.entrySet()) {
			String fileName = entry.getKey().replaceAll("\\.", "/") + ".java";
			sources.add(new SourceCode(fileName, entry.getValue()));
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
		compilationOutcome = Files.createTempDirectory("compile-test-");
		fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(compilationOutcome.toFile()));

		CompilationTask task = compiler.getTask(null, fileManager, diagnosticsCollector, null, null, sources);
		boolean result = task.call();

		if (result) {

			CachedWorkItemHandlerConfig wiConfig = new CachedWorkItemHandlerConfig();
			for (Entry<String, WorkItemHandler> entry : handlers.entrySet()) {
				wiConfig.register(entry.getKey(), entry.getValue());
			}

			ProcessConfig config = new StaticProcessConfig(wiConfig, new DefaultProcessEventListenerConfig(),
					new DefaultUnitOfWorkManager(new CollectingUnitOfWorkFactory()), null,
					new DefaultVariableInitializer());

			URLClassLoader cl = new URLClassLoader(new URL[] { compilationOutcome.toUri().toURL() });
			Map<String, BpmnProcess> processes = new HashMap<>();
			for (String className : classData.keySet()) {
				Class<?> processClass = Class.forName(className, true, cl);

				Method processMethod = processClass.getMethod("process");
				Process process = (Process) processMethod.invoke(null);
				assertThat(process).isNotNull();

				processes.put(process.getId(), new BpmnProcess(process, config));
			}

			return processes;
		} else {
			List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
			for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
				System.err.println(d);
			}

			throw new RuntimeException("Compilation failed");
		}
	}
}
