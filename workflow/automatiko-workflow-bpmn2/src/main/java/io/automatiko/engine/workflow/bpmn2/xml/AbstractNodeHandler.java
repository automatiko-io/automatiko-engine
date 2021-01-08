
package io.automatiko.engine.workflow.bpmn2.xml;

import static io.automatiko.engine.workflow.compiler.util.ClassUtils.constructClass;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.COMPLETION_CONDITION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.BooleanDataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.FloatDataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.IntegerDataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.StringDataType;
import io.automatiko.engine.workflow.bpmn2.core.Association;
import io.automatiko.engine.workflow.bpmn2.core.Definitions;
import io.automatiko.engine.workflow.bpmn2.core.Error;
import io.automatiko.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatiko.engine.workflow.bpmn2.core.Lane;
import io.automatiko.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatiko.engine.workflow.bpmn2.core.Signal;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.compiler.xml.XmlDumper;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.ProcessAction;
import io.automatiko.engine.workflow.process.core.impl.ConsequenceAction;
import io.automatiko.engine.workflow.process.core.impl.ExtendedNodeImpl;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public abstract class AbstractNodeHandler extends BaseAbstractHandler implements Handler {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractNodeHandler.class);

    static final String PROCESS_INSTANCE_SIGNAL_EVENT = "kcontext.getProcessInstance().signalEvent(";
    static final String RUNTIME_SIGNAL_EVENT = "kcontext.getKnowledgeRuntime().signalEvent(";
    static final String RUNTIME_MANAGER_SIGNAL_EVENT = "((org.kie.api.runtime.manager.RuntimeManager)kcontext.getKnowledgeRuntime().getEnvironment().get(\"RuntimeManager\")).signalEvent(";

    protected static final String EVENT_TYPE = "EventType";

    public static final String INPUT_TYPES = "BPMN.InputTypes";
    public static final String OUTPUT_TYPES = "BPMN.OutputTypes";

    protected final static String EOL = System.getProperty("line.separator");
    protected Map<String, String> dataInputs = new HashMap<String, String>();
    protected Map<String, String> dataInputTypes = new HashMap<String, String>();
    protected Map<String, String> dataOutputs = new HashMap<String, String>();
    protected Map<String, String> dataOutputTypes = new HashMap<String, String>();
    protected Map<String, String> inputAssociation = new HashMap<String, String>();
    protected Map<String, String> outputAssociation = new HashMap<String, String>();

    public AbstractNodeHandler() {
        initValidParents();
        initValidPeers();
        this.allowNesting = true;
    }

    protected void initValidParents() {
        this.validParents = new HashSet<Class<?>>();
        this.validParents.add(NodeContainer.class);
    }

    protected void initValidPeers() {
        this.validPeers = new HashSet<Class<?>>();
        this.validPeers.add(null);
        this.validPeers.add(Lane.class);
        this.validPeers.add(Variable.class);
        this.validPeers.add(Node.class);
        this.validPeers.add(SequenceFlow.class);
        this.validPeers.add(Lane.class);
        this.validPeers.add(Association.class);
    }

    public Object start(final String uri, final String localName, final Attributes attrs,
            final ExtensibleXmlParser parser) throws SAXException {
        dataInputs = new HashMap<String, String>();
        dataOutputs = new HashMap<String, String>();
        dataInputTypes = new HashMap<String, String>();
        dataOutputTypes = new HashMap<String, String>();

        parser.startElementBuilder(localName, attrs);
        final Node node = createNode(attrs);
        String id = attrs.getValue("id");
        node.setMetaData("UniqueId", id);
        final String name = attrs.getValue("name");
        node.setName(name);
        node.setMetaData(INPUT_TYPES, dataInputTypes);
        node.setMetaData(OUTPUT_TYPES, dataOutputTypes);

        AtomicInteger idGen = (AtomicInteger) parser.getMetaData().get("idGen");
        node.setId(idGen.getAndIncrement());

        return node;
    }

    protected abstract Node createNode(Attributes attrs);

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        final Element element = parser.endElementBuilder();
        Node node = (Node) parser.getCurrent();
        handleNode(node, element, uri, localName, parser);
        NodeContainer nodeContainer = (NodeContainer) parser.getParent();
        nodeContainer.addNode(node);
        ((ProcessBuildData) parser.getData()).addNode(node);
        return node;
    }

    protected void handleNode(final Node node, final Element element, final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        final String x = element.getAttribute("x");
        if (x != null && x.length() != 0) {
            try {
                node.setMetaData("x", Integer.parseInt(x));
            } catch (NumberFormatException exc) {
                throw new SAXParseException("<" + localName + "> requires an Integer 'x' attribute",
                        parser.getLocator());
            }
        }
        final String y = element.getAttribute("y");
        if (y != null && y.length() != 0) {
            try {
                node.setMetaData("y", new Integer(y));
            } catch (NumberFormatException exc) {
                throw new SAXParseException("<" + localName + "> requires an Integer 'y' attribute",
                        parser.getLocator());
            }
        }
        final String width = element.getAttribute("width");
        if (width != null && width.length() != 0) {
            try {
                node.setMetaData("width", new Integer(width));
            } catch (NumberFormatException exc) {
                throw new SAXParseException("<" + localName + "> requires an Integer 'width' attribute",
                        parser.getLocator());
            }
        }
        final String height = element.getAttribute("height");
        if (height != null && height.length() != 0) {
            try {
                node.setMetaData("height", new Integer(height));
            } catch (NumberFormatException exc) {
                throw new SAXParseException("<" + localName + "> requires an Integer 'height' attribute",
                        parser.getLocator());
            }
        }
    }

    public abstract void writeNode(final Node node, final StringBuilder xmlDump, final int metaDataType);

    protected void writeNode(final String name, final Node node, final StringBuilder xmlDump, int metaDataType) {
        xmlDump.append("    <" + name + " ");
        xmlDump.append("id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(node) + "\" ");
        if (node.getName() != null) {
            xmlDump.append("name=\"" + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(node.getName()) + "\" ");
        }
        if (metaDataType == XmlBPMNProcessDumper.META_DATA_AS_NODE_PROPERTY) {
            Integer x = (Integer) node.getMetaData().get("x");
            Integer y = (Integer) node.getMetaData().get("y");
            Integer width = (Integer) node.getMetaData().get("width");
            Integer height = (Integer) node.getMetaData().get("height");
            if (x != null && x != 0) {
                xmlDump.append("g:x=\"" + x + "\" ");
            }
            if (y != null && y != 0) {
                xmlDump.append("g:y=\"" + y + "\" ");
            }
            if (width != null && width != -1) {
                xmlDump.append("g:width=\"" + width + "\" ");
            }
            if (height != null && height != -1) {
                xmlDump.append("g:height=\"" + height + "\" ");
            }
        }
    }

    protected void endNode(final StringBuilder xmlDump) {
        xmlDump.append("/>" + EOL);
    }

    protected void endNode(final String name, final StringBuilder xmlDump) {
        xmlDump.append("    </" + name + ">" + EOL);
    }

    protected void handleScript(final ExtendedNodeImpl node, final Element element, String type) {
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i) instanceof Element) {
                Element xmlNode = (Element) nodeList.item(i);
                String nodeName = xmlNode.getNodeName();
                if (nodeName.equals("extensionElements")) {
                    NodeList subNodeList = xmlNode.getChildNodes();
                    for (int j = 0; j < subNodeList.getLength(); j++) {
                        org.w3c.dom.Node subXmlNode = subNodeList.item(j);
                        if (subXmlNode.getNodeName().contains(type + "-script")) {
                            List<ProcessAction> actions = node.getActions(type);
                            if (actions == null) {
                                actions = new ArrayList<ProcessAction>();
                                node.setActions(type, actions);
                            }
                            ProcessAction action = extractScript((Element) subXmlNode);
                            actions.add(action);
                        }
                    }
                }
            }
        }
    }

    public static ProcessAction extractScript(Element xmlNode) {
        String dialect = "mvel";
        if ("http://www.java.com/java".equals(xmlNode.getAttribute("scriptFormat"))) {
            dialect = "java";
        } else if ("http://www.javascript.com/javascript".equals(xmlNode.getAttribute("scriptFormat"))) {
            dialect = "JavaScript";
        }
        NodeList subNodeList = xmlNode.getChildNodes();
        for (int j = 0; j < subNodeList.getLength(); j++) {
            if (subNodeList.item(j) instanceof Element) {
                Element subXmlNode = (Element) subNodeList.item(j);
                if ("script".equals(subXmlNode.getNodeName())) {
                    String consequence = subXmlNode.getTextContent();
                    ConsequenceAction action = new ConsequenceAction(dialect, consequence);
                    return action;
                }
            }
        }
        return new ConsequenceAction("mvel", "");
    }

    protected void writeMetaData(final Node node, final StringBuilder xmlDump) {
        XmlBPMNProcessDumper.writeMetaData(getMetaData(node), xmlDump);
    }

    protected Map<String, Object> getMetaData(Node node) {
        return XmlBPMNProcessDumper.getMetaData(node.getMetaData());
    }

    protected void writeExtensionElements(Node node, final StringBuilder xmlDump) {
        if (containsExtensionElements(node)) {
            xmlDump.append("      <extensionElements>" + EOL);
            if (node instanceof ExtendedNodeImpl) {
                writeScripts("onEntry", ((ExtendedNodeImpl) node).getActions("onEntry"), xmlDump);
                writeScripts("onExit", ((ExtendedNodeImpl) node).getActions("onExit"), xmlDump);
            }
            writeMetaData(node, xmlDump);
            xmlDump.append("      </extensionElements>" + EOL);
        }
    }

    protected boolean containsExtensionElements(Node node) {
        if (!getMetaData(node).isEmpty()) {
            return true;
        }
        if (node instanceof ExtendedNodeImpl && ((ExtendedNodeImpl) node).containsActions()) {
            return true;
        }
        return false;
    }

    protected void writeScripts(final String type, List<ProcessAction> actions, final StringBuilder xmlDump) {
        if (actions != null && actions.size() > 0) {
            for (ProcessAction action : actions) {
                writeScript(action, type, xmlDump);
            }
        }
    }

    public static void writeScript(final ProcessAction action, String type, final StringBuilder xmlDump) {
        if (action instanceof ConsequenceAction) {
            ConsequenceAction consequenceAction = (ConsequenceAction) action;
            xmlDump.append("        <tns:" + type + "-script");
            String name = consequenceAction.getName();
            if (name != null) {
                xmlDump.append(" name=\"" + name + "\"");
            }
            String dialect = consequenceAction.getDialect();
            if ("java".equals(dialect)) {
                xmlDump.append(" scriptFormat=\"" + XmlBPMNProcessDumper.JAVA_LANGUAGE + "\"");
            } else if ("JavaScript".equals(dialect)) {
                xmlDump.append(" scriptFormat=\"" + XmlBPMNProcessDumper.JAVASCRIPT_LANGUAGE + "\"");
            }

        } else {
            throw new IllegalArgumentException("Unknown action " + action);
        }
    }

    protected void readIoSpecification(org.w3c.dom.Node xmlNode, Map<String, String> dataInputs,
            Map<String, String> dataOutputs, Map<String, String> dataInputTypes, Map<String, String> dataOutputTypes) {
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        while (subNode instanceof Element) {
            String subNodeName = subNode.getNodeName();
            if ("dataInput".equals(subNodeName)) {
                String id = ((Element) subNode).getAttribute("id");
                String inputName = ((Element) subNode).getAttribute("name");
                String type = ((Element) subNode).getAttribute("dtype");
                dataInputs.put(id, inputName);
                dataInputTypes.put(inputName, type);
            }
            if ("dataOutput".equals(subNodeName)) {
                String id = ((Element) subNode).getAttribute("id");
                String outputName = ((Element) subNode).getAttribute("name");
                String type = ((Element) subNode).getAttribute("dtype");
                dataOutputs.put(id, outputName);
                dataOutputTypes.put(outputName, type);
            }
            subNode = subNode.getNextSibling();
        }
    }

    protected void readDataInputAssociation(org.w3c.dom.Node xmlNode, Map<String, String> forEachNodeInputAssociation) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        if ("sourceRef".equals(subNode.getNodeName())) {
            String source = subNode.getTextContent();
            // targetRef
            subNode = subNode.getNextSibling();
            String target = subNode.getTextContent();
            forEachNodeInputAssociation.put(target, source);
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
                    forEachNodeInputAssociation.put(to, subSubNode.getTextContent());
                    return;
                } else if (nl.getLength() == 0) {
                    return;
                }
                Object result = null;
                Object from = nl.item(0);
                if (from instanceof Text) {
                    result = ((Text) from).getTextContent();
                } else {
                    result = nl.item(0);
                }
                forEachNodeInputAssociation.put(to, result.toString());

            }
        }
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode,
            Map<String, String> forEachNodeOutputAssociation) {
        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        if ("sourceRef".equals(subNode.getNodeName())) {
            String source = subNode.getTextContent();
            // targetRef
            subNode = subNode.getNextSibling();
            String target = subNode.getTextContent();
            forEachNodeOutputAssociation.put(source, target);
        }
    }

    @SuppressWarnings("unchecked")
    protected void readMultiInstanceLoopCharacteristics(org.w3c.dom.Node xmlNode, ForEachNode forEachNode,
            ExtensibleXmlParser parser) {

        // sourceRef
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        while (subNode != null) {
            String nodeName = subNode.getNodeName();
            if ("inputDataItem".equals(nodeName)) {
                String variableName = ((Element) subNode).getAttribute("id");
                String itemSubjectRef = ((Element) subNode).getAttribute("itemSubjectRef");
                DataType dataType = null;
                Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
                        .getData()).getMetaData("ItemDefinitions");
                dataType = getDataType(itemSubjectRef, itemDefinitions, parser.getClassLoader());

                if (variableName != null && variableName.trim().length() > 0) {
                    forEachNode.setMetaData("MIInput", ((Element) subNode).getAttribute("id"));

                    forEachNode.setVariable(variableName, dataType);
                }
            } else if ("outputDataItem".equals(nodeName)) {
                String variableName = ((Element) subNode).getAttribute("id");
                String itemSubjectRef = ((Element) subNode).getAttribute("itemSubjectRef");
                DataType dataType = null;
                Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
                        .getData()).getMetaData("ItemDefinitions");
                dataType = getDataType(itemSubjectRef, itemDefinitions, parser.getClassLoader());

                if (variableName != null && variableName.trim().length() > 0) {
                    forEachNode.setMetaData("MIOutput", ((Element) subNode).getAttribute("id"));

                    forEachNode.setOutputVariable(variableName, dataType);
                }
            } else if ("loopDataOutputRef".equals(nodeName)) {

                String outputDataRef = ((Element) subNode).getTextContent();

                if (outputDataRef != null && outputDataRef.trim().length() > 0) {
                    String collectionName = outputAssociation.get(outputDataRef);
                    if (collectionName == null) {
                        collectionName = dataOutputs.get(outputDataRef);
                    }
                    forEachNode.setOutputCollectionExpression(collectionName);

                }
                forEachNode.setMetaData("MICollectionOutput", outputDataRef);

            } else if ("loopDataInputRef".equals(nodeName)) {

                String inputDataRef = ((Element) subNode).getTextContent();

                if (inputDataRef != null && inputDataRef.trim().length() > 0) {
                    String collectionName = inputAssociation.get(inputDataRef);
                    if (collectionName == null) {
                        collectionName = dataInputs.get(inputDataRef);
                    }
                    forEachNode.setCollectionExpression(collectionName);

                }
                forEachNode.setMetaData("MICollectionInput", inputDataRef);

            } else if (COMPLETION_CONDITION.equals(nodeName)) {
                String expression = subNode.getTextContent();
                forEachNode.setCompletionConditionExpression(expression);

                String language = ((Element) subNode).getAttribute("language");
                forEachNode.setExpressionLang(language);
            }
            subNode = subNode.getNextSibling();
        }
    }

    protected DataType getDataType(String itemSubjectRef, Map<String, ItemDefinition> itemDefinitions, ClassLoader cl) {
        DataType dataType = new ObjectDataType();
        if (itemDefinitions == null) {
            return dataType;
        }
        ItemDefinition itemDefinition = itemDefinitions.get(itemSubjectRef);
        if (itemDefinition != null) {
            String structureRef = itemDefinition.getStructureRef();

            if ("java.lang.Boolean".equals(structureRef) || "Boolean".equals(structureRef)) {
                dataType = new BooleanDataType();

            } else if ("java.lang.Integer".equals(structureRef) || "Integer".equals(structureRef)) {
                dataType = new IntegerDataType();

            } else if ("java.lang.Float".equals(structureRef) || "Float".equals(structureRef)) {
                dataType = new FloatDataType();

            } else if ("java.lang.String".equals(structureRef) || "String".equals(structureRef)) {
                dataType = new StringDataType();

            } else if ("java.lang.Object".equals(structureRef) || "Object".equals(structureRef)) {
                dataType = new ObjectDataType(constructClass(structureRef), structureRef);

            } else {
                dataType = new ObjectDataType(constructClass(structureRef, cl), structureRef);
            }

        }
        return dataType;
    }

    protected String getErrorIdForErrorCode(String errorCode, Node node) {
        io.automatiko.engine.api.definition.process.NodeContainer parent = node.getParentContainer();
        while (!(parent instanceof ExecutableProcess) && parent instanceof Node) {
            parent = ((Node) parent).getParentContainer();
        }
        if (!(parent instanceof ExecutableProcess)) {
            throw new RuntimeException("This should never happen: !(parent instanceof RuleFlowProcess): parent is "
                    + parent.getClass().getSimpleName());
        }
        List<Error> errors = ((Definitions) ((ExecutableProcess) parent).getMetaData("Definitions")).getErrors();
        Error error = null;
        for (Error listError : errors) {
            if (errorCode.equals(listError.getErrorCode())) {
                error = listError;
                break;
            } else if (errorCode.equals(listError.getId())) {
                error = listError;
                break;
            }
        }
        if (error == null) {
            throw new IllegalArgumentException("Could not find error with errorCode " + errorCode);
        }
        return error.getId();
    }

    protected void handleThrowCompensationEventNode(final Node node, final Element element, final String uri,
            final String localName, final ExtensibleXmlParser parser) {
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        assert node instanceof ActionNode
                || node instanceof EndNode : "Node is neither an ActionNode nor an EndNode but a "
                        + node.getClass().getSimpleName();
        while (xmlNode != null) {
            if ("compensateEventDefinition".equals(xmlNode.getNodeName())) {
                String activityRef = ((Element) xmlNode).getAttribute("activityRef");
                if (activityRef == null) {
                    activityRef = "";
                }
                node.setMetaData("compensation-activityRef", activityRef);

                /**
                 * waitForCompletion: BPMN 2.0 Spec, p. 304: "By default, compensation is
                 * triggered synchronously, that is the compensation throw event waits for the
                 * completion of the triggered compensation handler. Alternatively, compensation
                 * can be triggered without waiting for its completion, by setting the throw
                 * compensation event's waitForCompletion attribute to false."
                 */
                String nodeId = (String) node.getMetaData().get("UniqueId");
                String waitForCompletionString = ((Element) xmlNode).getAttribute("waitForCompletion");
                boolean waitForCompletion = true;
                if (waitForCompletionString != null && waitForCompletionString.length() > 0) {
                    waitForCompletion = Boolean.parseBoolean(waitForCompletionString);
                }
                if (!waitForCompletion) {
                    throw new IllegalArgumentException(
                            "Asynchronous compensation [" + nodeId + ", " + node.getName() + "] is not yet supported!");
                }

            }
            xmlNode = xmlNode.getNextSibling();
        }
    }

    protected void writeVariableName(EventNode eventNode, StringBuilder xmlDump) {
        if (eventNode.getVariableName() != null) {
            xmlDump.append("      <dataOutput id=\"" + XmlBPMNProcessDumper.getUniqueNodeId(eventNode)
                    + "_Output\" name=\"event\" />" + EOL);
            xmlDump.append("      <dataOutputAssociation>" + EOL);
            xmlDump.append("      <sourceRef>" + XmlBPMNProcessDumper.getUniqueNodeId(eventNode) + "_Output</sourceRef>"
                    + EOL + "      <targetRef>" + XmlDumper.replaceIllegalChars(eventNode.getVariableName())
                    + "</targetRef>" + EOL);
            xmlDump.append("      </dataOutputAssociation>" + EOL);
            xmlDump.append("      <outputSet>" + EOL);
            xmlDump.append("        <dataOutputRefs>" + XmlBPMNProcessDumper.getUniqueNodeId(eventNode)
                    + "_Output</dataOutputRefs>" + EOL);
            xmlDump.append("      </outputSet>" + EOL);
        }
    }

    private static final String SIGNAL_NAMES = "signalNames";

    protected String checkSignalAndConvertToRealSignalNam(ExtensibleXmlParser parser, String signalName) {

        Signal signal = findSignalByName(parser, signalName);
        if (signal != null) {
            signalName = signal.getName();
            if (signalName == null) {
                throw new IllegalArgumentException("Signal definition must have a name attribute");
            }
        }

        return signalName;
    }

    protected Signal findSignalByName(ExtensibleXmlParser parser, String signalName) {
        ProcessBuildData buildData = ((ProcessBuildData) parser.getData());

        Set<String> signalNames = (Set<String>) buildData.getMetaData(SIGNAL_NAMES);
        if (signalNames == null) {
            signalNames = new HashSet<>();
            buildData.setMetaData(SIGNAL_NAMES, signalNames);
        }
        signalNames.add(signalName);

        Map<String, Signal> signals = (Map<String, Signal>) buildData.getMetaData("Signals");
        if (signals != null) {
            return signals.get(signalName);
        }

        return null;
    }

    protected String retrieveDataType(String itemSubjectRef, String dtype, ExtensibleXmlParser parser) {
        if (dtype != null && !dtype.isEmpty()) {
            return dtype;
        }

        if (itemSubjectRef != null && !itemSubjectRef.isEmpty()) {
            Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
                    .getData()).getMetaData("ItemDefinitions");

            return itemDefinitions.get(itemSubjectRef).getStructureRef();
        }

        return null;
    }

    /**
     * Finds the right variable by its name to make sure that when given as id it
     * will be also matched
     *
     * @param variableName name or id of the variable
     * @param parser parser instance
     * @return returns found variable name or given 'variableName' otherwise
     */
    protected String findVariable(String variableName, final ExtensibleXmlParser parser) {
        if (variableName == null) {
            return null;
        }
        List<?> parents = parser.getParents();

        for (Object parent : parents) {
            if (parent instanceof ContextContainer) {
                ContextContainer contextContainer = (ContextContainer) parent;
                VariableScope variableScope = (VariableScope) contextContainer
                        .getDefaultContext(VariableScope.VARIABLE_SCOPE);
                return variableScope.getVariables().stream().filter(v -> v.matchByIdOrName(variableName))
                        .map(v -> v.getName()).findFirst().orElse(variableName);
            }
        }

        return variableName;
    }

}
