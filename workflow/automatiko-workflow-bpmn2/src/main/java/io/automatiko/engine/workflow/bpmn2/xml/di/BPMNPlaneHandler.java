
package io.automatiko.engine.workflow.bpmn2.xml.di;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.definition.process.Process;
import io.automatiko.engine.workflow.bpmn2.core.Definitions;
import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNEdgeHandler.ConnectionInfo;
import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNShapeHandler.NodeInfo;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcess;

public class BPMNPlaneHandler extends BaseAbstractHandler implements Handler {

    public BPMNPlaneHandler() {
        initValidParents();
        initValidPeers();
        this.allowNesting = false;
    }

    protected void initValidParents() {
        this.validParents = new HashSet<Class<?>>();
        this.validParents.add(Definitions.class);
    }

    protected void initValidPeers() {
        this.validPeers = new HashSet<Class<?>>();
        this.validPeers.add(null);
        this.validPeers.add(Process.class);
    }

    public Object start(final String uri, final String localName, final Attributes attrs,
            final ExtensibleXmlParser parser) throws SAXException {
        parser.startElementBuilder(localName, attrs);

        final String processRef = attrs.getValue("bpmnElement");
        ProcessInfo info = new ProcessInfo(processRef);
        return info;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        parser.endElementBuilder();
        ProcessInfo processInfo = (ProcessInfo) parser.getCurrent();
        List<Process> processes = ((ProcessBuildData) parser.getData()).getProcesses();
        ExecutableProcess process = null;
        for (Process p : processes) {
            if (p.getId() != null && p.getId().equals(processInfo.getProcessRef())) {
                process = (ExecutableProcess) p;
                break;
            }
        }
        if (process != null) {
            for (NodeInfo nodeInfo : processInfo.getNodeInfos()) {
                processNodeInfo(nodeInfo, process.getNodes());
            }
            postProcessNodeOffset(process.getNodes(), 0, 0);
            for (ConnectionInfo connectionInfo : processInfo.getConnectionInfos()) {
                if (connectionInfo.getBendpoints() != null) {
                    processConnectionInfo(connectionInfo, process.getNodes());
                }
            }
        }
        return processInfo;
    }

    private boolean processNodeInfo(NodeInfo nodeInfo, Node[] nodes) {
        if (nodeInfo == null || nodeInfo.getNodeRef() == null) {
            return false;
        }
        for (Node node : nodes) {
            String id = (String) node.getMetaData().get("UniqueId");
            if (nodeInfo.getNodeRef().equals(id)) {
                ((io.automatiko.engine.workflow.process.core.Node) node).setMetaData("x", nodeInfo.getX());
                ((io.automatiko.engine.workflow.process.core.Node) node).setMetaData("y", nodeInfo.getY());
                ((io.automatiko.engine.workflow.process.core.Node) node).setMetaData("width", nodeInfo.getWidth());
                ((io.automatiko.engine.workflow.process.core.Node) node).setMetaData("height", nodeInfo.getHeight());
                return true;
            }
            if (node instanceof NodeContainer) {
                boolean found = processNodeInfo(nodeInfo, ((NodeContainer) node).getNodes());
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    private void postProcessNodeOffset(Node[] nodes, int xOffset, int yOffset) {
        for (Node node : nodes) {
            Integer x = (Integer) node.getMetaData().get("x");
            if (x != null) {
                ((io.automatiko.engine.workflow.process.core.Node) node).setMetaData("x", x - xOffset);
            }
            Integer y = (Integer) node.getMetaData().get("y");
            if (y != null) {
                ((io.automatiko.engine.workflow.process.core.Node) node).setMetaData("y", y - yOffset);
            }
            if (node instanceof NodeContainer) {
                postProcessNodeOffset(((NodeContainer) node).getNodes(), xOffset + (x == null ? 0 : x),
                        yOffset + (y == null ? 0 : y));
            }
        }
    }

    private boolean processConnectionInfo(ConnectionInfo connectionInfo, Node[] nodes) {
        for (Node node : nodes) {
            for (List<Connection> connections : node.getOutgoingConnections().values()) {
                for (Connection connection : connections) {
                    String id = (String) connection.getMetaData().get("UniqueId");
                    if (id != null && id.equals(connectionInfo.getElementRef())) {
                        ((ConnectionImpl) connection).setMetaData("bendpoints", connectionInfo.getBendpoints());
                        ((ConnectionImpl) connection).setMetaData("x", connectionInfo.getXs());
                        ((ConnectionImpl) connection).setMetaData("y", connectionInfo.getYs());
                        return true;
                    }
                }
            }
            if (node instanceof NodeContainer) {
                boolean found = processConnectionInfo(connectionInfo, ((NodeContainer) node).getNodes());
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    public Class<?> generateNodeFor() {
        return ProcessInfo.class;
    }

    public static class ProcessInfo {

        private String processRef;
        private List<NodeInfo> nodeInfos = new ArrayList<NodeInfo>();
        private List<ConnectionInfo> connectionInfos = new ArrayList<ConnectionInfo>();

        public ProcessInfo(String processRef) {
            this.processRef = processRef;
        }

        public String getProcessRef() {
            return processRef;
        }

        public void addNodeInfo(NodeInfo nodeInfo) {
            this.nodeInfos.add(nodeInfo);
        }

        public List<NodeInfo> getNodeInfos() {
            return nodeInfos;
        }

        public void addConnectionInfo(ConnectionInfo connectionInfo) {
            connectionInfos.add(connectionInfo);
        }

        public List<ConnectionInfo> getConnectionInfos() {
            return connectionInfos;
        }

    }

}
