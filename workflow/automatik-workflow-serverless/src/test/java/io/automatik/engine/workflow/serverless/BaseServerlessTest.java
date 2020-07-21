
package io.automatik.engine.workflow.serverless;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import io.automatik.engine.workflow.serverless.api.Workflow;
import io.automatik.engine.workflow.serverless.api.end.End;
import io.automatik.engine.workflow.serverless.api.events.EventDefinition;
import io.automatik.engine.workflow.serverless.api.start.Start;
import io.automatik.engine.workflow.serverless.api.states.DefaultState;
import io.automatik.engine.workflow.serverless.api.states.InjectState;
import io.automatik.engine.workflow.serverless.parser.ServerlessWorkflowParser;
import io.automatik.engine.workflow.serverless.parser.core.ServerlessWorkflowFactory;
import io.automatik.engine.workflow.serverless.parser.util.WorkflowAppContext;

public abstract class BaseServerlessTest {

	protected static ServerlessWorkflowFactory testFactory = new ServerlessWorkflowFactory(
			WorkflowAppContext.ofProperties(testWorkflowProperties()));

	protected static final Workflow singleInjectStateWorkflow = new Workflow()
			.withStates(singletonList(new InjectState().withName("relayState").withType(DefaultState.Type.INJECT)
					.withStart(new Start().withKind(Start.Kind.DEFAULT)).withEnd(new End(End.Kind.DEFAULT))));

	protected static final Workflow multiInjectStateWorkflow = new Workflow().withStates(asList(
			new InjectState().withName("relayState").withType(DefaultState.Type.INJECT)
					.withStart(new Start().withKind(Start.Kind.DEFAULT)).withEnd(new End(End.Kind.DEFAULT)),
			new InjectState().withName("relayState2").withType(DefaultState.Type.INJECT)
					.withEnd(new End(End.Kind.DEFAULT))));

	protected static final Workflow eventDefOnlyWorkflow = new Workflow().withEvents(singletonList(
			new EventDefinition().withName("sampleEvent").withSource("sampleSource").withType("sampleType")));

	protected ServerlessWorkflowParser getWorkflowParser(String workflowLocation) {
		ServerlessWorkflowParser parser;
		if (workflowLocation.endsWith(".sw.json")) {
			parser = new ServerlessWorkflowParser("json");
		} else {
			parser = new ServerlessWorkflowParser("yml");
		}
		return parser;
	}

	protected Reader classpathResourceReader(String location) {
		return new InputStreamReader(this.getClass().getResourceAsStream(location));
	}

	protected static Properties testWorkflowProperties() {
		Properties properties = new Properties();
		properties.put("kogito.sw.functions.testfunction1.testprop1", "testprop1val");
		properties.put("kogito.sw.functions.testfunction1.testprop2", "testprop2val");
		properties.put("kogito.sw.functions.testfunction2.testprop1", "testprop1val");
		properties.put("kogito.sw.functions.testfunction2.testprop2", "testprop2val");
		properties.put("kogito.sw.functions.testfunction3.ruleflowgroup", "testruleflowgroup");

		return properties;
	}
}