
package io.automatik.engine.workflow.bpmn2.xml;

import static io.automatik.engine.workflow.compiler.xml.processes.DynamicNodeHandler.AUTOCOMPLETE_COMPLETION_CONDITION;
import static io.automatik.engine.workflow.process.executable.core.Metadata.COMPLETION_CONDITION;
import static io.automatik.engine.workflow.process.executable.core.Metadata.CUSTOM_ACTIVATION_CONDITION;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.DynamicNode;

public class AdHocSubProcessHandler extends CompositeContextNodeHandler {

    protected static final List<String> AUTOCOMPLETE_EXPRESSIONS = Arrays.asList(
            "getActivityInstanceAttribute(\"numberOfActiveInstances\") == 0", AUTOCOMPLETE_COMPLETION_CONDITION);

    @Override
    protected Node createNode(Attributes attrs) {
        DynamicNode result = new DynamicNode();
        VariableScope variableScope = new VariableScope();
        result.addContext(variableScope);
        result.setDefaultContext(variableScope);
        return result;
    }

    @Override
    public Class<?> generateNodeFor() {
        return DynamicNode.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);
        DynamicNode dynamicNode = (DynamicNode) node;
        String cancelRemainingInstances = element.getAttribute("cancelRemainingInstances");
        if ("false".equals(cancelRemainingInstances)) {
            dynamicNode.setCancelRemainingInstances(false);
        }
        // by default it should not autocomplete as it's adhoc
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        dynamicNode.setActivationCondition((String) node.getMetaData().get(CUSTOM_ACTIVATION_CONDITION));
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if (COMPLETION_CONDITION.equals(nodeName)) {
                String expression = xmlNode.getTextContent();
                if (AUTOCOMPLETE_EXPRESSIONS.contains(expression)) {
                    dynamicNode.setAutoComplete(true);
                } else {
                    dynamicNode.setCompletionCondition(expression);
                }
            }
            xmlNode = xmlNode.getNextSibling();
        }
        List<SequenceFlow> connections = (List<SequenceFlow>) dynamicNode.getMetaData(ProcessHandler.CONNECTIONS);
        ProcessHandler processHandler = new ProcessHandler();
        processHandler.linkConnections(dynamicNode, connections);
        processHandler.linkBoundaryEvents(dynamicNode);

        handleScript(dynamicNode, element, "onEntry");
        handleScript(dynamicNode, element, "onExit");
    }

    @Override
    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        DynamicNode dynamicNode = (DynamicNode) node;
        writeNode("adHocSubProcess", dynamicNode, xmlDump, metaDataType);
        if (!dynamicNode.isCancelRemainingInstances()) {
            xmlDump.append(" cancelRemainingInstances=\"false\"");
        }
        xmlDump.append(" ordering=\"Parallel\" >" + EOL);
        writeExtensionElements(dynamicNode, xmlDump);
        // nodes
        List<Node> subNodes = getSubNodes(dynamicNode);
        XmlBPMNProcessDumper.INSTANCE.visitNodes(subNodes, xmlDump, metaDataType);

        // connections
        visitConnectionsAndAssociations(dynamicNode, xmlDump, metaDataType);

        if (dynamicNode.isAutoComplete()) {
            xmlDump.append("    <completionCondition xsi:type=\"tFormalExpression\">"
                    + AUTOCOMPLETE_COMPLETION_CONDITION + "</completionCondition>" + EOL);
        }
        endNode("adHocSubProcess", xmlDump);
    }

}
