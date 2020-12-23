
package io.automatiko.engine.workflow.bpmn2.xml;

import org.xml.sax.Attributes;

import io.automatiko.engine.workflow.compiler.xml.XmlDumper;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.FaultNode;

public class FaultNodeHandler extends AbstractNodeHandler {

	protected Node createNode(Attributes attrs) {
		throw new IllegalArgumentException("Reading in should be handled by end event handler");
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return FaultNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
		FaultNode faultNode = (FaultNode) node;
		writeNode("endEvent", faultNode, xmlDump, metaDataType);
		xmlDump.append(">" + EOL);
		writeExtensionElements(node, xmlDump);
		if (faultNode.getFaultVariable() != null) {
			xmlDump.append("      <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(faultNode)
					+ "_Input\" name=\"error\" />" + EOL);
			xmlDump.append("      <dataInputAssociation>" + EOL);
			xmlDump.append("        <sourceRef>" + XmlDumper.replaceIllegalChars(faultNode.getFaultVariable())
					+ "</sourceRef>" + EOL + "        <targetRef>" + XmlBPMNProcessDumper.getUniqueNodeId(faultNode)
					+ "_Input</targetRef>" + EOL);
			xmlDump.append("      </dataInputAssociation>" + EOL);
			xmlDump.append("      <inputSet>" + EOL);
			xmlDump.append("        <dataInputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(faultNode)
					+ "_Input</dataInputRefs>" + EOL);
			xmlDump.append("      </inputSet>" + EOL);
		}
		if (faultNode.isTerminateParent()) {
			String errorCode = faultNode.getFaultName();
			String errorId = getErrorIdForErrorCode(errorCode, faultNode);
			xmlDump.append("      <errorEventDefinition errorRef=\""
					+ XmlBPMNProcessDumper.replaceIllegalCharsAttribute(errorId) + "\" />" + EOL);
		} else {
			xmlDump.append("      <escalationEventDefinition escalationRef=\""
					+ XmlBPMNProcessDumper.replaceIllegalCharsAttribute(faultNode.getFaultName()) + "\" />" + EOL);
		}
		endNode("endEvent", xmlDump);
	}

}
