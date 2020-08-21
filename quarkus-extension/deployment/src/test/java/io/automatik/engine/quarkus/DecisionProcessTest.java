package io.automatik.engine.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.quarkus.test.QuarkusUnitTest;

public class DecisionProcessTest {

	@RegisterExtension
	static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
			.create(JavaArchive.class).addAsResource("dmnprocess.bpmn", "src/main/resources/dmnprocess.bpmn")
			.addAsResource("vacationDays.dmn", "src/main/resources/vacationDays.dmn")
			.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

	@Inject
	@Named("DmnProcess_1_0")
	Process<? extends Model> process;

	@Test
	public void testProcess() throws Exception {

		Model m = process.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("age", 16);
		parameters.put("yearsOfService", 1);
		m.fromMap(parameters);

		ProcessInstance<? extends Model> processInstance = process.createInstance(m);
		processInstance.start();
		assertEquals(io.automatik.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED, processInstance.status());
		Model result = processInstance.variables();

		assertEquals(BigDecimal.valueOf(27), result.toMap().get("vacationDays"));
	}
}
