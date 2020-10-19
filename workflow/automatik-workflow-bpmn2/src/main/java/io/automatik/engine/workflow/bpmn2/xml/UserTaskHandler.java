
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.bpmn2.core.Resource;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.compiler.xml.XmlDumper;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.HumanTaskNode;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class UserTaskHandler extends TaskHandler {

    protected Node createNode(Attributes attrs) {
        return new HumanTaskNode();
    }

    @SuppressWarnings("unchecked")
    public Class generateNodeFor() {
        return HumanTaskNode.class;
    }

    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        HumanTaskNode humanTaskNode = (HumanTaskNode) node;
        Work work = humanTaskNode.getWork();
        work.setName("Human Task");

        ProcessBuildData buildData = (ProcessBuildData) parser.getData();
        Map<String, Resource> resources = (Map<String, Resource>) buildData.getMetaData("Resources");

        Map<String, String> dataInputs = new HashMap<String, String>();
        Map<String, String> dataOutputs = new HashMap<String, String>();
        List<String> owners = new ArrayList<String>();
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            // ioSpec and data{Input,Output}Spec handled in super.handleNode(...)
            if ("potentialOwner".equals(nodeName)) {
                String owner = readPotentialOwner(xmlNode, humanTaskNode);
                if (owner != null) {
                    owners.add(owner);
                }
            } else if ("performer".equals(nodeName)) {
                org.w3c.dom.Node resourceNode = xmlNode.getFirstChild();
                if (resourceNode != null) {
                    String resourceId = resourceNode.getTextContent();
                    if (resources.containsKey(resourceId)) {
                        owners.add(resources.get(resourceId).getName());
                    }
                }
            }
            xmlNode = xmlNode.getNextSibling();
        }
        if (owners.size() > 0) {
            String owner = owners.get(0);
            for (int i = 1; i < owners.size(); i++) {
                owner += "," + owners.get(i);
            }
            humanTaskNode.getWork().setParameter("ActorId", owner);
        }
        humanTaskNode.getWork().setParameter("NodeName", humanTaskNode.getName());
    }

    protected String readPotentialOwner(org.w3c.dom.Node xmlNode, HumanTaskNode humanTaskNode) {
        org.w3c.dom.Node node = xmlNode.getFirstChild();
        if (node != null) {
            node = node.getFirstChild();
            if (node != null) {
                node = node.getFirstChild();
                if (node != null) {
                    return node.getTextContent();
                }
            }
        }
        return null;
    }

    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        HumanTaskNode humanTaskNode = (HumanTaskNode) node;
        writeNode("userTask", humanTaskNode, xmlDump, metaDataType);
        xmlDump.append(">" + EOL);
        writeExtensionElements(humanTaskNode, xmlDump);
        writeIO(humanTaskNode, xmlDump);
        String ownerString = (String) humanTaskNode.getWork().getParameter("ActorId");
        if (ownerString != null) {
            String[] owners = ownerString.split(",");
            for (String owner : owners) {
                xmlDump.append("      <potentialOwner>" + EOL + "        <resourceAssignmentExpression>" + EOL
                        + "          <formalExpression>" + owner + "</formalExpression>" + EOL
                        + "        </resourceAssignmentExpression>" + EOL + "      </potentialOwner>" + EOL);
            }
        }
        endNode("userTask", xmlDump);
    }

    protected void writeIO(WorkItemNode workItemNode, StringBuilder xmlDump) {
        xmlDump.append("      <ioSpecification>" + EOL);
        for (Map.Entry<String, String> entry : workItemNode.getInMappings().entrySet()) {
            xmlDump.append("        <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Input\" name=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
        }
        for (Map.Entry<String, Object> entry : workItemNode.getWork().getParameters().entrySet()) {
            if (!"ActorId".equals(entry.getKey()) && entry.getValue() != null) {
                xmlDump.append("        <dataInput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                        + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Input\" name=\""
                        + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
            }
        }
        for (Map.Entry<String, String> entry : workItemNode.getOutMappings().entrySet()) {
            xmlDump.append("        <dataOutput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "Output\" name=\""
                    + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(entry.getKey()) + "\" />" + EOL);
        }
        xmlDump.append("        <inputSet>" + EOL);
        for (Map.Entry<String, String> entry : workItemNode.getInMappings().entrySet()) {
            xmlDump.append("          <dataInputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</dataInputRefs>" + EOL);
        }
        for (Map.Entry<String, Object> entry : workItemNode.getWork().getParameters().entrySet()) {
            if (!"ActorId".equals(entry.getKey()) && entry.getValue() != null) {
                xmlDump.append("          <dataInputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                        + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</dataInputRefs>" + EOL);
            }
        }
        xmlDump.append("        </inputSet>" + EOL);
        xmlDump.append("        <outputSet>" + EOL);
        for (Map.Entry<String, String> entry : workItemNode.getOutMappings().entrySet()) {
            xmlDump.append("          <dataOutputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Output</dataOutputRefs>" + EOL);
        }
        xmlDump.append("        </outputSet>" + EOL);
        xmlDump.append("      </ioSpecification>" + EOL);
        for (Map.Entry<String, String> entry : workItemNode.getInMappings().entrySet()) {
            xmlDump.append("      <dataInputAssociation>" + EOL);
            xmlDump.append("        <sourceRef>" + XmlDumper.replaceIllegalChars(entry.getValue()) + "</sourceRef>"
                    + EOL + "        <targetRef>" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</targetRef>" + EOL);
            xmlDump.append("      </dataInputAssociation>" + EOL);
        }
        for (Map.Entry<String, Object> entry : workItemNode.getWork().getParameters().entrySet()) {
            if (!"ActorId".equals(entry.getKey()) && entry.getValue() != null) {
                xmlDump.append("      <dataInputAssociation>" + EOL);
                xmlDump.append("        <targetRef>" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                        + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</targetRef>" + EOL
                        + "        <assignment>" + EOL + "          <from xsi:type=\"tFormalExpression\">"
                        + XmlDumper.replaceIllegalChars(entry.getValue().toString()) + "</from>" + EOL
                        + "          <to xsi:type=\"tFormalExpression\">"
                        + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                        + XmlDumper.replaceIllegalChars(entry.getKey()) + "Input</to>" + EOL + "        </assignment>"
                        + EOL);
                xmlDump.append("      </dataInputAssociation>" + EOL);
            }
        }
        for (Map.Entry<String, String> entry : workItemNode.getOutMappings().entrySet()) {
            xmlDump.append("      <dataOutputAssociation>" + EOL);
            xmlDump.append("        <sourceRef>" + XmlBPMNProcessDumper.getUniqueNodeId(workItemNode) + "_"
                    + XmlDumper.replaceIllegalChars(entry.getKey()) + "Output</sourceRef>" + EOL + "        <targetRef>"
                    + XmlDumper.replaceIllegalChars(entry.getValue()) + "</targetRef>" + EOL);
            xmlDump.append("      </dataOutputAssociation>" + EOL);
        }
    }

}
