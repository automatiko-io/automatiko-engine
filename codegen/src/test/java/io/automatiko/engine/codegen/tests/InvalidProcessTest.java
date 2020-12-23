
package io.automatiko.engine.codegen.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.codegen.AbstractCodegenTest;

public class InvalidProcessTest extends AbstractCodegenTest {

	@Test
	public void testBasicUserTaskProcess() throws Exception {
		assertThrows(IllegalArgumentException.class,
				() -> generateCodeProcessesOnly("invalid/invalid-process-id.bpmn2"),
				"Process id '_7063C749-BCA8-4B6D-BC31-ACEE6FDF5512' is not valid");

	}

}
