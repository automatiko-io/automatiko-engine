
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import io.automatiko.engine.workflow.compiler.xml.XmlDumper;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;

public class ForEachNodeHandler extends AbstractCompositeNodeHandler {

	protected Node createNode(Attributes attrs) {
		throw new IllegalArgumentException("Reading in should be handled by end event handler");
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return ForEachNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
		ForEachNode forEachNode = (ForEachNode) node;
		writeNode("subProcess", forEachNode, xmlDump, metaDataType);
		xmlDump.append(" >" + EOL);
		writeExtensionElements(node, xmlDump);
		// ioSpecification and dataInputAssociation
		xmlDump.append("      <ioSpecification>" + EOL);
		String parameterName = forEachNode.getVariableName();
		if (parameterName != null) {
			xmlDump.append("        <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(forEachNode)
					+ "_input\" name=\"MultiInstanceInput\" />" + EOL);
		}
		xmlDump.append("        <inputSet/>" + EOL + "        <outputSet/>" + EOL + "      </ioSpecification>" + EOL);
		String collectionExpression = forEachNode.getCollectionExpression();
		if (collectionExpression != null) {
			xmlDump.append("      <dataInputAssociation>" + EOL + "        <sourceRef>"
					+ XmlDumper.replaceIllegalChars(collectionExpression) + "</sourceRef>" + EOL + "        <targetRef>"
					+ XmlBPMNProcessDumper.getUniqueNodeId(forEachNode) + "_input</targetRef>" + EOL
					+ "      </dataInputAssociation>" + EOL);
		}
		// multiInstanceLoopCharacteristics
		xmlDump.append("      <multiInstanceLoopCharacteristics>" + EOL + "        <loopDataInputRef>"
				+ XmlBPMNProcessDumper.getUniqueNodeId(forEachNode) + "_input</loopDataInputRef>" + EOL);
		if (parameterName != null) {
			xmlDump.append("        <inputDataItem id=\""
					+ XmlBPMNProcessDumper.replaceIllegalCharsAttribute(parameterName) + "\" itemSubjectRef=\""
					+ XmlBPMNProcessDumper.getUniqueNodeId(forEachNode) + "_multiInstanceItemType\"/>" + EOL);
		}
		xmlDump.append("      </multiInstanceLoopCharacteristics>" + EOL);
		// nodes
		List<Node> subNodes = getSubNodes(forEachNode);
		XmlBPMNProcessDumper.INSTANCE.visitNodes(subNodes, xmlDump, metaDataType);

		// connections
		visitConnectionsAndAssociations(forEachNode, xmlDump, metaDataType);

		endNode("subProcess", xmlDump);
	}

	protected List<Node> getSubNodes(ForEachNode forEachNode) {
		List<Node> subNodes = new ArrayList<Node>();
		for (io.automatiko.engine.api.definition.process.Node subNode : forEachNode.getNodes()) {
			// filter out composite start and end nodes as they can be regenerated
			if ((!(subNode instanceof CompositeNode.CompositeNodeStart))
					&& (!(subNode instanceof CompositeNode.CompositeNodeEnd))) {
				subNodes.add((Node) subNode);
			}
		}
		return subNodes;
	}

}
