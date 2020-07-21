
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;

import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.CompositeContextNode;
import io.automatik.engine.workflow.process.core.node.CompositeNode;
import io.automatik.engine.workflow.process.core.node.EventSubProcessNode;

public class CompositeContextNodeHandler extends AbstractCompositeNodeHandler {

	protected Node createNode(Attributes attrs) {
		throw new IllegalArgumentException("Reading in should be handled by end event handler");
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return CompositeContextNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
		CompositeContextNode compositeNode = (CompositeContextNode) node;
		String nodeType = "subProcess";
		if (node.getMetaData().get("Transaction") != null) {
			nodeType = "transaction";
		}
		writeNode(nodeType, compositeNode, xmlDump, metaDataType);
		if (compositeNode instanceof EventSubProcessNode) {
			xmlDump.append(" triggeredByEvent=\"true\" ");
		}
		Object isForCompensationObject = compositeNode.getMetaData("isForCompensation");
		if (isForCompensationObject != null && ((Boolean) isForCompensationObject)) {
			xmlDump.append("isForCompensation=\"true\" ");
		}
		xmlDump.append(">" + EOL);
		writeExtensionElements(compositeNode, xmlDump);
		// variables
		VariableScope variableScope = (VariableScope) compositeNode.getDefaultContext(VariableScope.VARIABLE_SCOPE);
		if (variableScope != null && !variableScope.getVariables().isEmpty()) {
			xmlDump.append("    <!-- variables -->" + EOL);
			for (Variable variable : variableScope.getVariables()) {
				xmlDump.append("    <property id=\""
						+ XmlBPMNProcessDumper.replaceIllegalCharsAttribute(variable.getName()) + "\" ");
				if (variable.getType() != null) {
					xmlDump.append("itemSubjectRef=\"" + XmlBPMNProcessDumper.getUniqueNodeId(compositeNode) + "-"
							+ XmlBPMNProcessDumper.replaceIllegalCharsAttribute(variable.getName()) + "Item\"");
				}
				// TODO: value
				xmlDump.append("/>" + EOL);
			}
		}
		// nodes
		List<Node> subNodes = getSubNodes(compositeNode);
		XmlBPMNProcessDumper.INSTANCE.visitNodes(subNodes, xmlDump, metaDataType);

		// connections
		visitConnectionsAndAssociations(compositeNode, xmlDump, metaDataType);

		endNode(nodeType, xmlDump);
	}

	protected List<Node> getSubNodes(CompositeNode compositeNode) {
		List<Node> subNodes = new ArrayList<Node>();
		for (io.automatik.engine.api.definition.process.Node subNode : compositeNode.getNodes()) {
			// filter out composite start and end nodes as they can be regenerated
			if ((!(subNode instanceof CompositeNode.CompositeNodeStart))
					&& (!(subNode instanceof CompositeNode.CompositeNodeEnd))) {
				subNodes.add((Node) subNode);
			}
		}
		return subNodes;
	}

}
