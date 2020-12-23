
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.bpmn2.core.Association;
import io.automatiko.engine.workflow.bpmn2.core.IntermediateLink;
import io.automatiko.engine.workflow.bpmn2.core.Lane;
import io.automatiko.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class SequenceFlowHandler extends BaseAbstractHandler implements Handler {

	public SequenceFlowHandler() {
		initValidParents();
		initValidPeers();
		this.allowNesting = false;
	}

	protected void initValidParents() {
		this.validParents = new HashSet<>();
		this.validParents.add(NodeContainer.class);
	}

	protected void initValidPeers() {
		this.validPeers = new HashSet<>();
		this.validPeers.add(null);
		this.validPeers.add(Lane.class);
		this.validPeers.add(Variable.class);
		this.validPeers.add(Node.class);
		this.validPeers.add(SequenceFlow.class);
		this.validPeers.add(Lane.class);
		this.validPeers.add(Association.class);
		// TODO: this is right?
		this.validPeers.add(IntermediateLink.class);
	}

	@SuppressWarnings("unchecked")
	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		final String id = attrs.getValue("id");
		final String sourceRef = attrs.getValue("sourceRef");
		final String targetRef = attrs.getValue("targetRef");
		final String bendpoints = attrs.getValue("g:bendpoints");
		final String name = attrs.getValue("name");
		final String priority = attrs.getValue("https://automatiko.io", "priority");

		NodeContainer nodeContainer = (NodeContainer) parser.getParent();

		List<SequenceFlow> connections = null;
		if (nodeContainer instanceof ExecutableProcess) {
			ExecutableProcess process = (ExecutableProcess) nodeContainer;
			connections = (List<SequenceFlow>) process.getMetaData(ProcessHandler.CONNECTIONS);
			if (connections == null) {
				connections = new ArrayList<>();
				process.setMetaData(ProcessHandler.CONNECTIONS, connections);
			}
		} else if (nodeContainer instanceof CompositeNode) {

			CompositeNode compositeNode = (CompositeNode) nodeContainer;
			connections = (List<SequenceFlow>) compositeNode.getMetaData(ProcessHandler.CONNECTIONS);
			if (connections == null) {
				connections = new ArrayList<>();
				compositeNode.setMetaData(ProcessHandler.CONNECTIONS, connections);
			}
		}

		SequenceFlow connection = new SequenceFlow(id, sourceRef, targetRef);
		connection.setBendpoints(bendpoints);
		connection.setName(name);
		if (priority != null) {
			connection.setPriority(Integer.parseInt(priority));
		}

		if (connections != null) {
			connections.add(connection);
		}

		return connection;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		SequenceFlow sequenceFlow = (SequenceFlow) parser.getCurrent();

		org.w3c.dom.Node xmlNode = element.getFirstChild();
		while (xmlNode != null) {
			String nodeName = xmlNode.getNodeName();
			if ("conditionExpression".equals(nodeName)) {
				String expression = xmlNode.getTextContent();
				org.w3c.dom.Node languageNode = xmlNode.getAttributes().getNamedItem("language");
				if (languageNode != null) {
					String language = languageNode.getNodeValue();
					if (XmlBPMNProcessDumper.JAVA_LANGUAGE.equals(language)) {
						sequenceFlow.setLanguage("java");
					} else if (XmlBPMNProcessDumper.MVEL_LANGUAGE.equals(language)) {
						sequenceFlow.setLanguage("mvel");
					} else if (XmlBPMNProcessDumper.RULE_LANGUAGE.equals(language)) {
						sequenceFlow.setType("rule");
					} else if (XmlBPMNProcessDumper.XPATH_LANGUAGE.equals(language)) {
						sequenceFlow.setLanguage("XPath");
					} else if (XmlBPMNProcessDumper.JAVASCRIPT_LANGUAGE.equals(language)) {
						sequenceFlow.setLanguage("JavaScript");
					} else if (XmlBPMNProcessDumper.FEEL_LANGUAGE.equals(language)
							|| XmlBPMNProcessDumper.DMN_FEEL_LANGUAGE.equals(language)) {
						sequenceFlow.setLanguage("FEEL");
					} else {
						throw new IllegalArgumentException("Unknown language " + language);
					}
				}
				sequenceFlow.setExpression(expression);
			}
			xmlNode = xmlNode.getNextSibling();
		}
		return sequenceFlow;
	}

	public Class<?> generateNodeFor() {
		return SequenceFlow.class;
	}
}
