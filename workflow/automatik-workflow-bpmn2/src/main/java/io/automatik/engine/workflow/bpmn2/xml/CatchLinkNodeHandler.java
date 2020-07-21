
package io.automatik.engine.workflow.bpmn2.xml;

import org.xml.sax.Attributes;

import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.CatchLinkNode;

public class CatchLinkNodeHandler extends AbstractNodeHandler implements Handler {

	public Class<?> generateNodeFor() {
		return CatchLinkNode.class;
	}

	@Override
	protected Node createNode(Attributes attrs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {

		CatchLinkNode linkNode = (CatchLinkNode) node;
		writeNode("intermediateCatchEvent", linkNode, xmlDump, metaDataType);
		xmlDump.append(">" + EOL);
		writeExtensionElements(linkNode, xmlDump);

		String name = (String) node.getMetaData().get(IntermediateCatchEventHandler.LINK_NAME);

		xmlDump.append("<linkEventDefinition name=\"" + name + "\" >" + EOL);

		Object target = linkNode.getMetaData("target");
		if (null != target) {
			xmlDump.append(String.format("<target>%s</target>", target) + EOL);
		}
		xmlDump.append("</linkEventDefinition>" + EOL);
		endNode("intermediateCatchEvent", xmlDump);

	}

}
