
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.bpmn2.core.Interface;
import io.automatik.engine.workflow.bpmn2.core.Interface.Operation;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class ServiceTaskHandler extends TaskHandler {

	protected Node createNode(Attributes attrs) {
		return new WorkItemNode();
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return Node.class;
	}

	@SuppressWarnings("unchecked")
	protected void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		WorkItemNode workItemNode = (WorkItemNode) node;
		String operationRef = element.getAttribute("operationRef");
		String implementation = element.getAttribute("implementation");
		List<Interface> interfaces = (List<Interface>) ((ProcessBuildData) parser.getData()).getMetaData("Interfaces");

		workItemNode.setMetaData("OperationRef", operationRef);
		workItemNode.setMetaData("Implementation", implementation);
		workItemNode.setMetaData("Type", "Service Task");
		if (interfaces != null) {
//            throw new IllegalArgumentException("No interfaces found");

			Operation operation = null;
			for (Interface i : interfaces) {
				operation = i.getOperation(operationRef);
				if (operation != null) {
					break;
				}
			}
			if (operation == null) {
				throw new IllegalArgumentException("Could not find operation " + operationRef);
			}
			// avoid overriding parameters set by data input associations
			if (workItemNode.getWork().getParameter("Interface") == null) {
				String interfaceRef = operation.getInterface().getImplementationRef();
				workItemNode.getWork().setParameter("Interface",
						interfaceRef != null && !interfaceRef.isEmpty() ? interfaceRef
								: operation.getInterface().getName());
			}
			if (workItemNode.getWork().getParameter("Operation") == null) {
				workItemNode.getWork().setParameter("Operation", operation.getName());
			}
			if (workItemNode.getWork().getParameter("ParameterType") == null && operation.getMessage() != null) {
				workItemNode.getWork().setParameter("ParameterType", operation.getMessage().getType());
			}
			// parameters to support web service invocation
			if (implementation != null) {
				workItemNode.getWork().setParameter("interfaceImplementationRef",
						operation.getInterface().getImplementationRef());
				workItemNode.getWork().setParameter("operationImplementationRef", operation.getImplementationRef());
				workItemNode.getWork().setParameter("implementation", implementation);
			}
		}
	}

	protected String getTaskName(final Element element) {
		return "Service Task";
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		throw new IllegalArgumentException("Writing out should be handled by TaskHandler");
	}

}
