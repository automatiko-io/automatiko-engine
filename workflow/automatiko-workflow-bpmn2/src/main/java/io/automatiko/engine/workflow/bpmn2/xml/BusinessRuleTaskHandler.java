
package io.automatiko.engine.workflow.bpmn2.xml;

import static io.automatiko.engine.workflow.process.core.node.RuleSetNode.DMN_LANG;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.runtime.process.DataTransformer;
import io.automatiko.engine.workflow.base.core.impl.DataTransformerRegistry;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.compiler.xml.XmlDumper;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.impl.NodeImpl;
import io.automatiko.engine.workflow.process.core.node.Assignment;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.Transformation;

public class BusinessRuleTaskHandler extends AbstractNodeHandler {

    private static final String NAMESPACE_PROP = "namespace";
    private static final String MODEL_PROP = "model";
    private static final String DECISION_PROP = "decision";
    private static final String DECISION_SERVICE_PROP = "decisionService";
    private DataTransformerRegistry transformerRegistry = DataTransformerRegistry.get();

    protected Node createNode(Attributes attrs) {
        return new RuleSetNode();
    }

    @SuppressWarnings("unchecked")
    public Class generateNodeFor() {
        return RuleSetNode.class;
    }

    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        RuleSetNode ruleSetNode = (RuleSetNode) node;

        String language = element.getAttribute("implementation");
        if (language == null || language.equalsIgnoreCase("##unspecified") || language.isEmpty()) {
            language = RuleSetNode.DMN_LANG;
        }
        ruleSetNode.setLanguage(language);

        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("ioSpecification".equals(nodeName)) {
                readIoSpecification(xmlNode, dataInputs, dataOutputs, dataInputTypes, dataOutputTypes);
            } else if ("dataInputAssociation".equals(nodeName)) {
                readDataInputAssociation(xmlNode, ruleSetNode, dataInputs);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, ruleSetNode, dataOutputs);
            }
            xmlNode = xmlNode.getNextSibling();
        }

        if (language.equals(DMN_LANG)) {
            String namespace = (String) ruleSetNode.removeParameter(NAMESPACE_PROP);
            String model = (String) ruleSetNode.removeParameter(MODEL_PROP);
            String decision = (String) ruleSetNode.removeParameter(DECISION_PROP);
            String decisionService = (String) ruleSetNode.removeParameter(DECISION_SERVICE_PROP);
            ruleSetNode.setRuleType(RuleSetNode.RuleType.decision(namespace, model,
                    decisionService != null ? decisionService : decision, decisionService != null));

        }

        handleScript(ruleSetNode, element, "onEntry");
        handleScript(ruleSetNode, element, "onExit");
    }

    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        RuleSetNode ruleSetNode = (RuleSetNode) node;
        writeNode("businessRuleTask", ruleSetNode, xmlDump, metaDataType);
        RuleSetNode.RuleType ruleType = ruleSetNode.getRuleType();
        if (ruleType != null) {
            xmlDump.append("g:ruleFlowGroup=\"" + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(ruleType.getName())
                    + "\" " + EOL);
            // else DMN
        }

        xmlDump.append(" implementation=\""
                + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(ruleSetNode.getLanguage()) + "\" >" + EOL);

        writeExtensionElements(ruleSetNode, xmlDump);
        writeIO(ruleSetNode, xmlDump);
        endNode("businessRuleTask", xmlDump);
    }

    protected void readDataInputAssociation(org.w3c.dom.Node xmlNode, RuleSetNode ruleSetNode,
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
            ruleSetNode.addInAssociation(
                    new DataAssociation(sources, dataInputs.get(target), assignments, transformation));
        } else {
            // targetRef
            String to = subNode.getTextContent();
            // assignment
            subNode = subNode.getNextSibling();
            if (subNode != null) {
                org.w3c.dom.Node subSubNode = subNode.getFirstChild();
                NodeList nl = subSubNode.getChildNodes();
                if (nl.getLength() > 1) {
                    // not supported ?
                    ruleSetNode.setParameter(dataInputs.get(to), subSubNode.getTextContent());
                    return;
                } else if (nl.getLength() == 0) {
                    return;
                }
                Object result = null;
                Object from = nl.item(0);
                if (from instanceof Text) {
                    String text = ((Text) from).getTextContent();
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        result = text.substring(1, text.length() - 1);
                    } else {
                        result = text;
                    }
                } else {
                    result = nl.item(0);
                }
                ruleSetNode.setParameter(dataInputs.get(to), result);
            }
        }
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, RuleSetNode ruleSetNode,
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
        ruleSetNode.addOutAssociation(new DataAssociation(
                sources.stream().map(source -> dataOutputs.get(source)).collect(Collectors.toList()), target,
                assignments, transformation));
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
                if (orignalNode instanceof RuleSetNode) {
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

    protected void adjustNodeConfiguration(Node orignalNode, ForEachNode forEachNode) {

        List<DataAssociation> inputs = ((RuleSetNode) orignalNode).adjustInMapping(forEachNode.getCollectionExpression());
        List<DataAssociation> outputs = ((RuleSetNode) orignalNode)
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

    protected void writeIO(RuleSetNode ruleSetNode, StringBuilder xmlDump) {
        xmlDump.append("      <ioSpecification>" + EOL);
        for (Map.Entry<String, String> entry : ruleSetNode.getInMappings().entrySet()) {
            xmlDump.append("        <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Input\" name=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
        }
        for (Map.Entry<String, Object> entry : ruleSetNode.getParameters().entrySet()) {
            if (!"ActorId".equals(entry.getKey()) && entry.getValue() != null) {
                xmlDump.append("        <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                        + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Input\" name=\""
                        + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
            }
        }
        for (Map.Entry<String, String> entry : ruleSetNode.getOutMappings().entrySet()) {
            xmlDump.append("        <dataOutput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Output\" name=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
        }
        xmlDump.append("        <inputSet>" + EOL);
        for (Map.Entry<String, String> entry : ruleSetNode.getInMappings().entrySet()) {
            xmlDump.append("          <dataInputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</dataInputRefs>" + EOL);
        }
        for (Map.Entry<String, Object> entry : ruleSetNode.getParameters().entrySet()) {
            if (!"ActorId".equals(entry.getKey()) && entry.getValue() != null) {
                xmlDump.append("          <dataInputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                        + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</dataInputRefs>" + EOL);
            }
        }
        xmlDump.append("        </inputSet>" + EOL);
        xmlDump.append("        <outputSet>" + EOL);
        for (Map.Entry<String, String> entry : ruleSetNode.getOutMappings().entrySet()) {
            xmlDump.append("          <dataOutputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Output</dataOutputRefs>" + EOL);
        }
        xmlDump.append("        </outputSet>" + EOL);
        xmlDump.append("      </ioSpecification>" + EOL);
        for (Map.Entry<String, String> entry : ruleSetNode.getInMappings().entrySet()) {
            xmlDump.append("      <dataInputAssociation>" + EOL);
            xmlDump.append("        <sourceRef>" + XmlDumper.replaceIllegalChars(entry.getValue()) + "</sourceRef>"
                    + EOL + "        <targetRef>" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</targetRef>" + EOL);
            xmlDump.append("      </dataInputAssociation>" + EOL);
        }
        for (Map.Entry<String, Object> entry : ruleSetNode.getParameters().entrySet()) {
            if (!"ActorId".equals(entry.getKey()) && entry.getValue() != null) {
                xmlDump.append("      <dataInputAssociation>" + EOL);
                xmlDump.append("        <targetRef>" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                        + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</targetRef>" + EOL
                        + "        <assignment>" + EOL + "          <from xsi:type=\"tFormalExpression\">"
                        + XmlDumper.replaceIllegalChars(entry.getValue().toString()) + "</from>" + EOL
                        + "          <to xsi:type=\"tFormalExpression\">"
                        + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                        + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</to>" + EOL + "        </assignment>"
                        + EOL);
                xmlDump.append("      </dataInputAssociation>" + EOL);
            }
        }
        for (Map.Entry<String, String> entry : ruleSetNode.getOutMappings().entrySet()) {
            xmlDump.append("      <dataOutputAssociation>" + EOL);
            xmlDump.append("        <sourceRef>" + XmlBPMNProcessDumper.getUniqueNodeId(ruleSetNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Output</sourceRef>" + EOL + "        <targetRef>"
                    + XmlDumper.replaceIllegalChars(entry.getValue()) + "</targetRef>" + EOL);
            xmlDump.append("      </dataOutputAssociation>" + EOL);
        }
    }

}
