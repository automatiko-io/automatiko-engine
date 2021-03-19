
package io.automatiko.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.Transformation;

public class ScriptTaskHandler extends AbstractNodeHandler {

    private static Map<String, String> SUPPORTED_SCRIPT_FORMATS = new HashMap<>();

    static {
        SUPPORTED_SCRIPT_FORMATS.put(XmlBPMNProcessDumper.JAVA_LANGUAGE, "java");
        SUPPORTED_SCRIPT_FORMATS.put(XmlBPMNProcessDumper.JAVASCRIPT_LANGUAGE, "JavaScript");
        SUPPORTED_SCRIPT_FORMATS.put(XmlBPMNProcessDumper.FEEL_LANGUAGE, "FEEL");
        SUPPORTED_SCRIPT_FORMATS.put(XmlBPMNProcessDumper.FEEL_LANGUAGE_SHORT, "FEEL");
    }

    public static void registerSupportedScriptFormat(String language, String dialect) {
        SUPPORTED_SCRIPT_FORMATS.put(language, dialect);
    }

    private DataTransformerRegistry transformerRegistry = DataTransformerRegistry.get();

    protected Node createNode(Attributes attrs) {
        ActionNode result = new ActionNode();
        result.setAction(new ConsequenceAction());
        return result;
    }

    @SuppressWarnings("unchecked")
    public Class generateNodeFor() {
        return Node.class;
    }

    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        ActionNode actionNode = (ActionNode) node;
        node.setMetaData("NodeType", "ScriptTask");
        ConsequenceAction action = (ConsequenceAction) actionNode.getAction();
        if (action == null) {
            action = new ConsequenceAction();
            actionNode.setAction(action);
        }
        String language = element.getAttribute("scriptFormat");
        action.setDialect(SUPPORTED_SCRIPT_FORMATS.getOrDefault(language, "mvel"));
        action.setConsequence("");

        dataInputs.clear();
        dataOutputs.clear();

        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {

            String nodeName = xmlNode.getNodeName();
            if (xmlNode instanceof Element && "script".equals(nodeName)) {
                action.setConsequence(xmlNode.getTextContent());
            } else if ("ioSpecification".equals(nodeName)) {
                readIoSpecification(xmlNode, dataInputs, dataOutputs, dataInputTypes, dataOutputTypes);
            } else if ("dataInputAssociation".equals(nodeName)) {
                readDataInputAssociation(xmlNode, actionNode, dataInputs);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, actionNode, dataOutputs);
            }
            xmlNode = xmlNode.getNextSibling();
        }

        actionNode.setMetaData("DataInputs", new LinkedHashMap<String, String>(dataInputs));
        actionNode.setMetaData("DataOutputs", new LinkedHashMap<String, String>(dataOutputs));

        String compensation = element.getAttribute("isForCompensation");
        if (compensation != null) {
            boolean isForCompensation = Boolean.parseBoolean(compensation);
            if (isForCompensation) {
                actionNode.setMetaData("isForCompensation", isForCompensation);
            }
        }
    }

    public Object end(final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        final Element element = parser.endElementBuilder();
        Node node = (Node) parser.getCurrent();
        // determine type of event definition, so the correct type of node can be generated
        handleNode(node, element, uri, localName, parser);

        org.w3c.dom.Node xmlNode = element.getFirstChild();
        int uniqueIdGen = 1;
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("multiInstanceLoopCharacteristics".equals(nodeName)) {
                // create new timerNode
                ForEachNode forEachNode = new ForEachNode();
                forEachNode.setId(node.getId());
                String uniqueId = (String) node.getMetaData().get("UniqueId");
                forEachNode.setMetaData("UniqueId", uniqueId);
                node.setMetaData("UniqueId", uniqueId + ":" + uniqueIdGen++);
                forEachNode.addNode(node);
                forEachNode.linkIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE, node.getId(),
                        NodeImpl.CONNECTION_DEFAULT_TYPE);
                forEachNode.linkOutgoingConnections(node.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE,
                        NodeImpl.CONNECTION_DEFAULT_TYPE);
                forEachNode.setSequential(Boolean.parseBoolean(((Element) xmlNode).getAttribute("isSequential")));

                Node orignalNode = node;
                node = forEachNode;
                handleForEachNode(node, element, uri, localName, parser);
                // remove input/output collection data input/output of for each to avoid problems when running in variable strict mode
                if (orignalNode instanceof ActionNode) {
                    adjustNodeConfiguration(orignalNode, forEachNode);
                }

                break;
            }
            xmlNode = xmlNode.getNextSibling();
        }

        NodeContainer nodeContainer = (NodeContainer) parser.getParent();
        nodeContainer.addNode(node);
        ((ProcessBuildData) parser.getData()).addNode(node);

        return node;
    }

    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        throw new IllegalArgumentException("Writing out should be handled by action node handler");
    }

    protected void readDataInputAssociation(org.w3c.dom.Node xmlNode, ActionNode actionNode,
            Map<String, String> dataInputs) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        if ("sourceRef".equals(subNode.getNodeName())) {
            List<String> sources = new ArrayList<>();
            sources.add(subNode.getTextContent());

            subNode = subNode.getNextSibling();

            while ("sourceRef".equals(subNode.getNodeName())) {
                sources.add(subNode.getTextContent());
                subNode = subNode.getNextSibling();
            }
            // targetRef
            String target = subNode.getTextContent();
            // transformation
            Transformation transformation = null;
            subNode = subNode.getNextSibling();
            if (subNode != null && "transformation".equals(subNode.getNodeName())) {
                String lang = subNode.getAttributes().getNamedItem("language").getNodeValue();
                String expression = subNode.getTextContent();

                DataTransformer transformer = transformerRegistry.find(lang);
                if (transformer == null) {
                    throw new IllegalArgumentException("No transformer registered for language " + lang);
                }
                transformation = new Transformation(lang, expression);

                subNode = subNode.getNextSibling();
            }
            // assignments
            List<Assignment> assignments = new LinkedList<Assignment>();
            while (subNode != null) {
                String expressionLang = ((Element) subNode).getAttribute("expressionLanguage");
                if (expressionLang == null || expressionLang.trim().isEmpty()) {
                    expressionLang = "XPath";
                }
                org.w3c.dom.Node ssubNode = subNode.getFirstChild();
                String from = ssubNode.getTextContent();
                String to = ssubNode.getNextSibling().getTextContent();
                assignments.add(new Assignment(expressionLang, from, to));
                subNode = subNode.getNextSibling();
            }
            actionNode.addInAssociation(
                    new DataAssociation(sources, dataInputs.get(target), assignments, transformation));
        }
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, ActionNode actionNode,
            Map<String, String> dataOutputs) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        List<String> sources = new ArrayList<>();
        sources.add(subNode.getTextContent());

        subNode = subNode.getNextSibling();

        while ("sourceRef".equals(subNode.getNodeName())) {
            sources.add(subNode.getTextContent());
            subNode = subNode.getNextSibling();
        }
        // targetRef
        String target = subNode.getTextContent();
        // transformation
        Transformation transformation = null;
        subNode = subNode.getNextSibling();
        if (subNode != null && "transformation".equals(subNode.getNodeName())) {
            String lang = subNode.getAttributes().getNamedItem("language").getNodeValue();
            String expression = subNode.getTextContent();
            DataTransformer transformer = transformerRegistry.find(lang);
            if (transformer == null) {
                throw new IllegalArgumentException("No transformer registered for language " + lang);
            }
            transformation = new Transformation(lang, expression);
            subNode = subNode.getNextSibling();
        }
        // assignments
        List<Assignment> assignments = new LinkedList<Assignment>();
        while (subNode != null) {
            String expressionLang = ((Element) subNode).getAttribute("expressionLanguage");
            if (expressionLang == null || expressionLang.trim().isEmpty()) {
                expressionLang = "XPath";
            }
            org.w3c.dom.Node ssubNode = subNode.getFirstChild();
            String from = ssubNode.getTextContent();
            String to = ssubNode.getNextSibling().getTextContent();
            assignments.add(new Assignment(expressionLang, from, to));
            subNode = subNode.getNextSibling();
        }
        actionNode.addOutAssociation(new DataAssociation(
                sources.stream().map(source -> dataOutputs.get(source)).collect(Collectors.toList()), target,
                assignments, transformation));
    }

    protected void adjustNodeConfiguration(Node orignalNode, ForEachNode forEachNode) {

        List<DataAssociation> inputs = ((ActionNode) orignalNode).adjustInMapping(forEachNode.getCollectionExpression());
        List<DataAssociation> outputs = ((ActionNode) orignalNode)
                .adjustOutMapping(forEachNode.getOutputCollectionExpression());

        if (inputs != null) {
            forEachNode.addInAssociation(inputs);
        }
        if (outputs != null) {
            forEachNode.addOutAssociation(outputs);
        }
    }

    protected void handleForEachNode(final Node node, final Element element, final String uri,
            final String localName, final ExtensibleXmlParser parser) {
        ForEachNode forEachNode = (ForEachNode) node;
        org.w3c.dom.Node xmlNode = element.getFirstChild();

        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("dataInputAssociation".equals(nodeName)) {
                readDataInputAssociation(xmlNode, inputAssociation);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, outputAssociation);
            } else if ("multiInstanceLoopCharacteristics".equals(nodeName)) {
                readMultiInstanceLoopCharacteristics(xmlNode, forEachNode, parser);
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

}
