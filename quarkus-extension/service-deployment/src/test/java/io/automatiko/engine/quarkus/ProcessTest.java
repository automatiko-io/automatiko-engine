package io.automatiko.engine.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.quarkus.test.QuarkusUnitTest;

public class ProcessTest {

	@RegisterExtension
	static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
			.create(JavaArchive.class).addAsResource("test-process.bpmn", "src/main/resources/test-process.bpmn")
			.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

	@Inject
	@Named("tests")
	Process<? extends Model> process;

	@Test
	public void testProcess() throws Exception {

		Model m = process.createModel();
		Map<String, Object> parameters = new HashMap<>();
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = process.createInstance(m);
		processInstance.start();
		assertEquals(io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());
	}
}
