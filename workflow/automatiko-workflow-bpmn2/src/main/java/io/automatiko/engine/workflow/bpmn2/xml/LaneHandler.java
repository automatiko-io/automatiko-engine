
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.bpmn2.core.Association;
import io.automatiko.engine.workflow.bpmn2.core.Lane;
import io.automatiko.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class LaneHandler extends BaseAbstractHandler implements Handler {

	public static final String LANES = "BPMN.Lanes";

	@SuppressWarnings("unchecked")
	public LaneHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(ExecutableProcess.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);
			this.validPeers.add(Lane.class);
			this.validPeers.add(Variable.class);
			this.validPeers.add(Node.class);
			this.validPeers.add(SequenceFlow.class);
			this.validPeers.add(Lane.class);
			this.validPeers.add(Association.class);

			this.allowNesting = false;
		}
	}

	@SuppressWarnings("unchecked")
	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		String id = attrs.getValue("id");
		String name = attrs.getValue("name");

		WorkflowProcess process = (WorkflowProcess) parser.getParent();

		List<Lane> lanes = (List<Lane>) ((ExecutableProcess) process).getMetaData(LaneHandler.LANES);
		if (lanes == null) {
			lanes = new ArrayList<Lane>();
			((ExecutableProcess) process).setMetaData(LaneHandler.LANES, lanes);
		}
		Lane lane = new Lane(id);
		lane.setName(name);
		lanes.add(lane);
		return lane;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		Lane lane = (Lane) parser.getCurrent();

		org.w3c.dom.Node xmlNode = element.getFirstChild();
		while (xmlNode != null) {
			String nodeName = xmlNode.getNodeName();
			if ("flowNodeRef".equals(nodeName)) {
				String flowElementRef = xmlNode.getTextContent();
				lane.addFlowElement(flowElementRef);
			}
			xmlNode = xmlNode.getNextSibling();
		}
		return lane;
	}

	public Class<?> generateNodeFor() {
		return Lane.class;
	}

}
