
package io.automatiko.engine.workflow.bpmn2.xml.di;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNPlaneHandler.ProcessInfo;
import io.automatiko.engine.workflow.bpmn2.xml.di.BPMNShapeHandler.NodeInfo;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;

public class BPMNEdgeHandler extends BaseAbstractHandler implements Handler {

    public BPMNEdgeHandler() {
        initValidParents();
        initValidPeers();
        this.allowNesting = false;
    }

    protected void initValidParents() {
        this.validParents = new HashSet<Class<?>>();
        this.validParents.add(ProcessInfo.class);
    }

    protected void initValidPeers() {
        this.validPeers = new HashSet<Class<?>>();
        this.validPeers.add(null);
        this.validPeers.add(NodeInfo.class);
        this.validPeers.add(ConnectionInfo.class);
    }

    public Object start(final String uri, final String localName, final Attributes attrs,
            final ExtensibleXmlParser parser) throws SAXException {
        parser.startElementBuilder(localName, attrs);

        final String elementRef = attrs.getValue("bpmnElement");
        ConnectionInfo info = new ConnectionInfo(elementRef);
        ProcessInfo processInfo = (ProcessInfo) parser.getParent();
        processInfo.addConnectionInfo(info);
        return info;
    }

    public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
        Element element = parser.endElementBuilder();
        // now get bendpoints
        String bendpoints = null;
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode instanceof Element) {
            String nodeName = xmlNode.getNodeName();
            if ("waypoint".equals(nodeName)) {
                // ignore first and last waypoint
                String x = ((Element) xmlNode).getAttribute("x");
                String y = ((Element) xmlNode).getAttribute("y");
                try {
                    int xValue = new Float(x).intValue();
                    int yValue = new Float(y).intValue();

                    xs.add(xValue);
                    ys.add(yValue);

                    if (bendpoints == null) {
                        bendpoints = "[";
                    } else if (xmlNode.getNextSibling() != null) {
                        bendpoints += xValue + "," + yValue;
                        bendpoints += ";";
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid bendpoint value", e);
                }
            }
            xmlNode = xmlNode.getNextSibling();
        }
        ConnectionInfo connectionInfo = (ConnectionInfo) parser.getCurrent();
        if (bendpoints != null && bendpoints.length() > 1) {
            connectionInfo.setBendpoints(bendpoints + "]");
        }
        connectionInfo.setXs(xs);
        connectionInfo.setYs(ys);

        return connectionInfo;
    }

    public Class<?> generateNodeFor() {
        return ConnectionInfo.class;
    }

    public static class ConnectionInfo {

        private String elementRef;
        private String bendpoints;

        private List<Integer> xs = new ArrayList<Integer>();
        private List<Integer> ys = new ArrayList<Integer>();

        public ConnectionInfo(String elementRef) {
            this.elementRef = elementRef;
        }

        public String getElementRef() {
            return elementRef;
        }

        public String getBendpoints() {
            return bendpoints;
        }

        public void setBendpoints(String bendpoints) {
            this.bendpoints = bendpoints;
        }

        public List<Integer> getXs() {
            return xs;
        }

        public void setXs(List<Integer> xs) {
            this.xs = xs;
        }

        public List<Integer> getYs() {
            return ys;
        }

        public void setYs(List<Integer> ys) {
            this.ys = ys;
        }

    }

}
