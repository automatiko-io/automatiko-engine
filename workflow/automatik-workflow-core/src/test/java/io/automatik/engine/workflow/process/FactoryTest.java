
package io.automatik.engine.workflow.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class FactoryTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void test() {
		ExecutableProcessFactory factory = ExecutableProcessFactory.createProcess("ExampleProcess");
		factory.variable("x", new ObjectDataType("java.lang.String"));
		factory.variable("y", new ObjectDataType("java.lang.String"));
		factory.variable("list", new ObjectDataType("java.util.List"));
		factory.variable("listOut", new ObjectDataType("java.util.List"));
		factory.name("Example Process");
		factory.packageName("org.company.bpmn2");
		factory.dynamic(false);
		factory.version("1.0");
		factory.visibility("Private");
		factory.metaData("TargetNamespace", "http://www.example.org/MinimalExample");
		factory.startNode(1).name("StartProcess").done();

		factory.dynamicNode(2).metaData("UniqueId", "_2").metaData("MICollectionOutput", "_2_listOutOutput")
				.metaData("x", 96).metaData("y", 16)
				.activationExpression(
						kcontext -> Objects.equals(kcontext.getVariable("x"), kcontext.getVariable("oldValue")))
				.variable("x", new ObjectDataType("java.lang.String"))
				.exceptionHandler(RuntimeException.class.getName(), "java", "System.out.println(\"Error\");")
				.autoComplete(true).language("java").done();

		factory.humanTaskNode(3).name("Task").taskName("Task Name").actorId("Actor").comment("Hey")
				.content("Some content").workParameter("x", "Parameter").inMapping("x", "y").outMapping("y", "x")
				.waitForCompletion(true).timer("1s", null, "java", "").onEntryAction("java", "")
				.onExitAction("java", "").done();

		factory.faultNode(4).name("Fault").faultName("Fault Name").faultVariable("x").done();

		factory.connection(1, 2, "_1-_2").connection(2, 3, "_2-_3").connection(3, 4, "_3-_4");

		factory.validate();

		List<String> list = new ArrayList<String>();
		list.add("first");
		list.add("second");
		List<String> listOut = new ArrayList<String>();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("x", "oldValue");
		parameters.put("list", list);

		InternalProcessRuntime ksession = createProcessRuntime(factory.getProcess());

		ksession.startProcess("ExampleProcess", parameters);
	}

}
