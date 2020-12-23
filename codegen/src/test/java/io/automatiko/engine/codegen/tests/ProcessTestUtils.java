
package io.automatiko.engine.codegen.tests;

import static org.assertj.core.api.Assertions.assertThat;

import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcessInstance;

class ProcessTestUtils {

	static void assertState(ProcessInstance<?> processInstance, int state) {
		assertThat(processInstance).isInstanceOf(AbstractProcessInstance.class);
		AbstractProcessInstance<?> abstractProcessInstance = (AbstractProcessInstance<?>) processInstance;
		assertThat(abstractProcessInstance.status())
				.withFailMessage("ProcessInstance [%s] Status - Expected: %s - Got: %s", processInstance.id(), state,
						processInstance.status())
				.isEqualTo(state);
		assertThat(abstractProcessInstance.processInstance().getState())
				.withFailMessage("LegacyProcessInstance [%s] Status - Expected: %s - Got: %s", processInstance.id(),
						state, ((AbstractProcessInstance<?>) processInstance).processInstance().getState())
				.isEqualTo(state);
	}

}
