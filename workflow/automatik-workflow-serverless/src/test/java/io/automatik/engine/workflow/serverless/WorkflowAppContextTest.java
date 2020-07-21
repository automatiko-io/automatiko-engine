package io.automatik.engine.workflow.serverless;

import org.junit.jupiter.api.Test;

import io.automatik.engine.workflow.serverless.parser.util.WorkflowAppContext;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkflowAppContextTest extends BaseServerlessTest {

	@Test
	public void testOfAppResources() throws Exception {
		WorkflowAppContext workflowAppContext = WorkflowAppContext.ofAppResources();
		assertThat(workflowAppContext).isNotNull();
		assertThat(workflowAppContext.getApplicationProperties()).isNotNull();
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction1.testprop1")).isEqualTo("testprop1val");
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction1.testprop2")).isEqualTo("testprop2val");
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction2.testprop1")).isEqualTo("testprop1val");
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction2.testprop2")).isEqualTo("testprop2val");
	}

	@Test
	public void testOfProperties() throws Exception {
		WorkflowAppContext workflowAppContext = WorkflowAppContext.ofProperties(testWorkflowProperties());
		assertThat(workflowAppContext).isNotNull();
		assertThat(workflowAppContext.getApplicationProperties()).isNotNull();
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction1.testprop1")).isEqualTo("testprop1val");
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction1.testprop2")).isEqualTo("testprop2val");
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction2.testprop1")).isEqualTo("testprop1val");
		assertThat(workflowAppContext.getApplicationProperties()
				.getProperty("kogito.sw.functions.testfunction2.testprop2")).isEqualTo("testprop2val");
	}

}
