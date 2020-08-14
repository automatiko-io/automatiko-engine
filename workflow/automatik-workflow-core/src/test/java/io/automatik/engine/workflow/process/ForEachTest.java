
package io.automatik.engine.workflow.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcessFactory;
import io.automatik.engine.workflow.test.util.AbstractBaseTest;

public class ForEachTest extends AbstractBaseTest {

	public void addLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Test
	public void test() {
		ExecutableProcessFactory factory = ExecutableProcessFactory.createProcess("ParentProcess");
		factory.variable("x", new ObjectDataType(java.lang.String.class));
		factory.variable("y", new ObjectDataType(java.lang.String.class));
		factory.variable("list", new ObjectDataType(java.util.List.class, "java.util.List<String>"));
		factory.variable("listOut", new ObjectDataType(java.util.List.class, "java.util.List<String>"));
		factory.name("Parent Process");
		factory.packageName("org.company.bpmn2");
		factory.dynamic(false);
		factory.version("1.0");
		factory.visibility("Private");
		factory.metaData("TargetNamespace", "http://www.example.org/MinimalExample");
		io.automatik.engine.workflow.process.executable.core.factory.StartNodeFactory startNode1 = factory.startNode(1);
		startNode1.name("StartProcess");
		startNode1.done();
		io.automatik.engine.workflow.process.executable.core.factory.ForEachNodeFactory forEachNode2 = factory
				.forEachNode(2);
		forEachNode2.metaData("UniqueId", "_2");
		forEachNode2.metaData("MICollectionOutput", "_2_listOutOutput");
		forEachNode2.metaData("x", 96);
		forEachNode2.metaData("width", 110);
		forEachNode2.metaData("y", 16);
		forEachNode2.metaData("MICollectionInput", "_2_input");
		forEachNode2.metaData("height", 48);
		forEachNode2.collectionExpression("list");
		forEachNode2.variable("x", new ObjectDataType(java.lang.String.class));
		forEachNode2.outputCollectionExpression("listOut");
		forEachNode2.outputVariable("y", new ObjectDataType(java.lang.String.class));

		forEachNode2.actionNode(5).action((kcontext) -> System.out.println(kcontext.getVariable("x"))).done();
		forEachNode2.linkIncomingConnections(5);
		forEachNode2.linkOutgoingConnections(5);

		forEachNode2.done();
		io.automatik.engine.workflow.process.executable.core.factory.EndNodeFactory endNode3 = factory.endNode(3);
		endNode3.name("EndProcess");
		endNode3.terminate(true);
		endNode3.done();
		factory.connection(1, 2, "_1-_2");
		factory.connection(2, 3, "_2-_3");
		factory.validate();

		List<String> list = new ArrayList<String>();
		list.add("first");
		list.add("second");
		List<String> listOut = new ArrayList<String>();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("x", "oldValue");
		parameters.put("list", list);
		parameters.put("listOut", listOut);

		InternalProcessRuntime ksession = createProcessRuntime(factory.getProcess());

		ksession.startProcess("ParentProcess", parameters);
	}

}
