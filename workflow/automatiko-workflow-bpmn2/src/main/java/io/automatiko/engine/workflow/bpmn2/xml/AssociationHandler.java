package io.automatiko.engine.workflow.bpmn2.xml;

import static io.automatiko.engine.workflow.bpmn2.xml.ProcessHandler.ASSOCIATIONS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.bpmn2.core.Association;
import io.automatiko.engine.workflow.bpmn2.core.Lane;
import io.automatiko.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class AssociationHandler extends BaseAbstractHandler implements Handler {

	public AssociationHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<>();
			this.validParents.add(Process.class);
			this.validParents.add(CompositeContextNode.class); // for SubProcesses

			this.validPeers = new HashSet<>();
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

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		Association association = new Association();
		association.setId(attrs.getValue("id"));
		association.setSourceRef(attrs.getValue("sourceRef"));
		association.setTargetRef(attrs.getValue("targetRef"));
		String direction = attrs.getValue("associationDirection");
		if (direction != null) {
			boolean acceptableDirection = false;
			direction = direction.toLowerCase();
			String[] possibleDirections = { "none", "one", "both" };
			for (String acceptable : possibleDirections) {
				if (acceptable.equals(direction)) {
					acceptableDirection = true;
					break;
				}
			}
			if (!acceptableDirection) {
				throw new IllegalArgumentException(
						"Unknown direction '" + direction + "' used in Association " + association.getId());
			}
		}
		association.setDirection(direction);

		/**
		 * BPMN2 spec, p. 66: "At this point, BPMN provides three standard Artifacts:
		 * Associations, Groups, and Text Annotations. ... When an Artifact is defined
		 * it is contained within a Collaboration or a FlowElementsContainer (a Process
		 * or Choreography)."
		 *
		 * (In other words: associations must be defined within a process, not outside)
		 */
		List<Association> associations = null;
		NodeContainer nodeContainer = (NodeContainer) parser.getParent();
		if (nodeContainer instanceof Process) {
			ExecutableProcess process = (ExecutableProcess) nodeContainer;
			associations = (List<Association>) process.getMetaData(ASSOCIATIONS);
			if (associations == null) {
				associations = new ArrayList<>();
				process.setMetaData(ASSOCIATIONS, associations);
			}
		} else if (nodeContainer instanceof CompositeNode) {
			CompositeContextNode compositeNode = (CompositeContextNode) nodeContainer;
			associations = (List<Association>) compositeNode.getMetaData(ASSOCIATIONS);
			if (associations == null) {
				associations = new ArrayList<>();
				compositeNode.setMetaData(ProcessHandler.ASSOCIATIONS, associations);
			}
		} else {
			associations = new ArrayList<>();
		}
		associations.add(association);

		return association;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return Association.class;
	}
}
