package io.automatiko.engine.codegen.process.image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGHints;
import org.jfree.svg.ViewBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

public class SvgBpmnProcessImageGenerator implements SvgProcessImageGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvgBpmnProcessImageGenerator.class);

    private WorkflowProcess workFlowProcess;

    private int maxWidth = 0;
    private int maxHeight = 0;

    private BasicStroke defaultStroke = new BasicStroke(2);
    private BasicStroke dotted = new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, new float[] { 2.0f }, 0.0f);
    private BasicStroke dashed = new BasicStroke(2.0f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, new float[] { 2.0f }, 1.0f);

    public SvgBpmnProcessImageGenerator(WorkflowProcess workFlowProcess) {
        this.workFlowProcess = workFlowProcess;
    }

    @Override
    public String generate() {
        try {
            SVGGraphics2D g2 = new SVGGraphics2D(2000, 1000);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(new Font(g2.getFont().getFontName(), Font.BOLD, g2.getFont().getSize()));
            g2.setStroke(defaultStroke);

            g2.drawString(workFlowProcess.getName(), 5, 10);

            buildNodeContainer(0, 0, workFlowProcess, g2);

            StringWriter writer = new StringWriter();
            writer.write(
                    "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
            writer.write(g2.getSVGElement(workFlowProcess.getId(), false, new ViewBox(0, 0, maxWidth + 20, maxHeight + 20),
                    null, null)
                    + "\n");

            return writer.toString();
        } catch (Throwable e) {
            LOGGER.warn("Unable to generate process image due to " + e.getMessage());
            return null;
        }
    }

    /*
     * Build methods
     */

    protected void buildNodeContainer(int x, int y, NodeContainer nodeContainer, SVGGraphics2D g2) {
        try {
            for (Node node : nodeContainer.getNodes()) {

                if (node instanceof StartNode) {
                    buildStartEvent(x, y, (StartNode) node, g2);
                } else if (node instanceof EndNode) {
                    buildEndEvent(x, y, (EndNode) node, g2);
                } else if (node instanceof FaultNode) {
                    buildErrorEndEvent(x, y, (FaultNode) node, g2);
                } else if (node instanceof BoundaryEventNode) {
                    buildBoundaryEvent(x, y, node, g2);
                } else if (node instanceof EventNode || node instanceof StateNode) {
                    buildIntermediateEvent(x, y, node, g2);
                } else if (node instanceof HumanTaskNode) {
                    buildHumanTaskNode(x, y, (HumanTaskNode) node, g2);
                } else if (node instanceof ActionNode) {
                    buildScriptTaskNode(x, y, (ActionNode) node, g2);
                } else if (node instanceof WorkItemNode) {
                    buildServiceTaskNode(x, y, (WorkItemNode) node, g2);
                } else if (node instanceof Split || node instanceof Join) {
                    buildGateway(x, y, node, g2);
                } else if (node instanceof ForEachNode) {

                    buildNodeContainer(x(node), y(node), ((ForEachNode) node).getCompositeNode(), g2);
                } else if (node instanceof CompositeNode) {
                    buildSubprocessNode(x, y, (CompositeNode) node, g2);
                    int sx = x(node);
                    int sy = y(node);
                    buildNodeContainer(sx, sy, (CompositeNode) node, g2);
                } else if (node instanceof RuleSetNode) {
                    buildBusinessRuleTaskNode(x, y, (RuleSetNode) node, g2);
                } else if (node instanceof TimerNode) {
                    buildTimerEvent(x, y, (TimerNode) node, g2);
                } else if (node instanceof SubProcessNode) {
                    buildCallActivity(x, y, (SubProcessNode) node, g2);
                }

                buildSequenceFlow(x, y, node, g2);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void buildStartEvent(int x, int y, StartNode node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        Ellipse2D.Double start = new Ellipse2D.Double(x, y, width, height);

        g2.draw(start);
        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), start.getBounds(), g2.getFont(), (height / 2) + 10);

        if ("ConsumeMessage".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, start.getBounds(), "MessageEventDefinition.png");
        } else if ("Signal".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, start.getBounds(), "SignalEventDefinition.png");
        } else if ("Timer".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, start.getBounds(), "TimerEventDefinition.png");
        } else if ("Error".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, start.getBounds(), "ErrorEventDefinition.png");
        } else if ("Escalation".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, start.getBounds(), "EscalationEventDefinition.png");
        } else if ("Condition".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, start.getBounds(), "ConditionalEventDefinition.png");
        }
    }

    protected void buildEndEvent(int x, int y, EndNode node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        Ellipse2D.Double end = new Ellipse2D.Double(x, y, width, height);

        g2.setStroke(new BasicStroke(4));
        g2.draw(end);
        if (node.isTerminate()) {
            Ellipse2D.Double innerend = new Ellipse2D.Double(x + 8, y + 8, width - 15, height - 15);
            g2.draw(innerend);
            g2.fill(innerend);
        } else if ("ProduceMessage".equals(node.getMetaData("TriggerType"))) {
            drawCenteredIcon(g2, end.getBounds(), "ThrowMessageEventDefinition.png");
        } else if ("signal".equals(node.getMetaData("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "ThrowSignalEventDefinition.png");
        } else if ("error".equals(node.getMetaData("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "ThrowErrorEventDefinition.png");
        } else if ("escalation".equals(node.getMetaData("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "ThrowEscalationEventDefinition.png");
        } else if ("Compensation".equals(node.getMetaData().get("TriggerType"))) {
            drawCenteredIcon(g2, end.getBounds(), "CompensateEventDefinition.png");
        }

        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), end.getBounds(), g2.getFont(), (height(node) / 2) + 10);
        g2.setStroke(defaultStroke);
    }

    protected void buildErrorEndEvent(int x, int y, FaultNode node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        Ellipse2D.Double end = new Ellipse2D.Double(x, y, width, height);

        g2.setStroke(new BasicStroke(4));
        g2.draw(end);
        drawCenteredIcon(g2, end.getBounds(), "ThrowErrorEventDefinition.png");

        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), end.getBounds(), g2.getFont(), (height(node) / 2) + 10);
        g2.setStroke(defaultStroke);
    }

    protected void buildIntermediateEvent(int x, int y, Node node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        Ellipse2D.Double end = new Ellipse2D.Double(x, y, width, height);
        g2.draw(end);

        Ellipse2D.Double innerend = new Ellipse2D.Double(x + 5, y + 5, width - 10, height - 10);
        g2.draw(innerend);

        if ("message".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "MessageEventDefinition.png");
        } else if ("signal".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "SignalEventDefinition.png");
        } else if ("timer".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "TimerEventDefinition.png");
        } else if ("error".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "ErrorEventDefinition.png");
        } else if ("escalation".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "EscalationEventDefinition.png");
        } else if ("condition".equals(node.getMetaData().get("EventType")) || node instanceof StateNode) {
            drawCenteredIcon(g2, end.getBounds(), "ConditionalEventDefinition.png");
        } else if ("compensation".equals(node.getMetaData().get("EventType")) || node instanceof StateNode) {
            drawCenteredIcon(g2, end.getBounds(), "CompensateEventDefinition.png");
        }

        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), end.getBounds(), g2.getFont(), (height(node) / 2) + 10);
        g2.setStroke(defaultStroke);
    }

    protected void buildBoundaryEvent(int x, int y, Node node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);

        if (Boolean.FALSE.equals(node.getMetaData().get("CancelActivity"))) {
            g2.setStroke(dashed);
        }

        Ellipse2D.Double end = new Ellipse2D.Double(x, y, width, height);
        g2.draw(end);
        g2.setColor(new Color(255, 255, 255));
        g2.fill(end);

        Ellipse2D.Double innerend = new Ellipse2D.Double(x + 5, y + 5, width - 10, height - 10);
        g2.setColor(new Color(0, 0, 0));
        g2.draw(innerend);
        g2.setColor(new Color(255, 255, 255));
        g2.fill(innerend);
        g2.setColor(new Color(0, 0, 0));

        if ("message".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "MessageEventDefinition.png");
        } else if ("signal".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "SignalEventDefinition.png");
        } else if ("timer".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "TimerEventDefinition.png");
        } else if ("error".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "ErrorEventDefinition.png");
        } else if ("escalation".equals(node.getMetaData().get("EventType"))) {
            drawCenteredIcon(g2, end.getBounds(), "EscalationEventDefinition.png");
        } else if ("condition".equals(node.getMetaData().get("EventType")) || node instanceof StateNode) {
            drawCenteredIcon(g2, end.getBounds(), "ConditionalEventDefinition.png");
        } else if ("compensation".equals(node.getMetaData().get("EventType")) || node instanceof StateNode) {
            drawCenteredIcon(g2, end.getBounds(), "CompensateEventDefinition.png");
        }

        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), end.getBounds(), g2.getFont(), (height(node) / 2) + 10);
        g2.setStroke(defaultStroke);
    }

    protected void buildTimerEvent(int x, int y, TimerNode node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        Ellipse2D.Double end = new Ellipse2D.Double(x, y, width, height);
        g2.draw(end);

        Ellipse2D.Double innerend = new Ellipse2D.Double(x + 5, y + 5, width - 10, height - 10);
        g2.draw(innerend);
        drawCenteredIcon(g2, end.getBounds(), "TimerEventDefinition.png");

        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), end.getBounds(), g2.getFont(), (height(node) / 2) + 10);
        g2.setStroke(defaultStroke);
    }

    protected void buildCallActivity(int x, int y, SubProcessNode node, SVGGraphics2D g2)
            throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);

        g2.setStroke(new BasicStroke(5));
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
        g2.setPaint(new Color(0, 0, 0));
        g2.draw(roundedRectangle);
        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), roundedRectangle.getBounds(), g2.getFont(), 0);

        Rectangle rect = roundedRectangle.getBounds();
        BufferedImage image = ImageIO.read(getClass().getResource("/icons/CallActivityPlus.png"));
        int ix = (int) (rect.getX() + (rect.getWidth() - image.getWidth()) / 2);
        int iy = (int) (rect.getY() + ((rect.getHeight() - image.getHeight()) - 3));

        setImageNodeId(node, g2);
        g2.drawImage(image, ix, iy, null);

        g2.setStroke(defaultStroke);

        drawWarningPlaceholderIcon(node, g2, roundedRectangle.getBounds());
    }

    protected void buildHumanTaskNode(int x, int y, HumanTaskNode node, SVGGraphics2D g2)
            throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
        g2.setPaint(new Color(0, 0, 0));
        g2.draw(roundedRectangle);
        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), roundedRectangle.getBounds(), g2.getFont(), 0);

        g2.drawImage(ImageIO.read(getClass().getResource("/icons/UserTask.png")), x + 2, y + 2, null);

        drawWarningPlaceholderIcon(node, g2, roundedRectangle.getBounds());
    }

    protected void buildScriptTaskNode(int x, int y, ActionNode node, SVGGraphics2D g2)
            throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);

        if (node.getMetaData("TriggerType") != null) {
            Ellipse2D.Double end = new Ellipse2D.Double(x, y, width, height);
            g2.draw(end);

            Ellipse2D.Double innerend = new Ellipse2D.Double(x + 5, y + 5, width - 10, height - 10);
            g2.draw(innerend);

            if ("ProduceMessage".equals(node.getMetaData("TriggerType"))) {
                drawCenteredIcon(g2, end.getBounds(), "ThrowMessageEventDefinition.png");
            } else if ("signal".equals(node.getMetaData("EventType"))) {
                drawCenteredIcon(g2, end.getBounds(), "ThrowSignalEventDefinition.png");
            } else if ("error".equals(node.getMetaData("EventType"))) {
                drawCenteredIcon(g2, end.getBounds(), "ThrowErrorEventDefinition.png");
            } else if ("escalation".equals(node.getMetaData("EventType"))) {
                drawCenteredIcon(g2, end.getBounds(), "ThrowEscalationEventDefinition.png");
            }

            setTextNodeId(node, g2);
            drawCenteredString(g2, node.getName(), end.getBounds(), g2.getFont(), (height(node) / 2) + 10);
            g2.setStroke(defaultStroke);
        } else {

            RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
            g2.setPaint(new Color(0, 0, 0));
            g2.draw(roundedRectangle);
            setTextNodeId(node, g2);
            drawCenteredString(g2, node.getName(), roundedRectangle.getBounds(), g2.getFont(), 0);

            g2.drawImage(ImageIO.read(getClass().getResource("/icons/ScriptTask.png")), x + 2, y + 2, null);

            drawWarningPlaceholderIcon(node, g2, roundedRectangle.getBounds());
        }
    }

    protected void buildServiceTaskNode(int x, int y, WorkItemNode node, SVGGraphics2D g2)
            throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
        g2.setPaint(new Color(0, 0, 0));
        g2.draw(roundedRectangle);
        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), roundedRectangle.getBounds(), g2.getFont(), 0);

        g2.drawImage(ImageIO.read(getClass().getResource("/icons/ServiceTask.png")), x + 2, y + 2, null);

        drawWarningPlaceholderIcon(node, g2, roundedRectangle.getBounds());
    }

    protected void buildBusinessRuleTaskNode(int x, int y, RuleSetNode node, SVGGraphics2D g2)
            throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
        g2.setPaint(new Color(0, 0, 0));
        g2.draw(roundedRectangle);
        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), roundedRectangle.getBounds(), g2.getFont(), 0);

        g2.drawImage(ImageIO.read(getClass().getResource("/icons/BusinessRuleTask.png")), x + 2, y + 2, null);

        drawWarningPlaceholderIcon(node, g2, roundedRectangle.getBounds());
    }

    protected void buildGateway(int x, int y, Node node, SVGGraphics2D g2) throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);
        Polygon p = new Polygon();

        p.addPoint(x, y + (height / 2));
        p.addPoint(x + (width / 2), y);
        p.addPoint(x + width, y + (height / 2));
        p.addPoint(x + (width / 2), y + height);

        g2.drawPolygon(p);
        setTextNodeId(node, g2);
        drawCenteredString(g2, node.getName(), p.getBounds(), g2.getFont(), (height(node) / 2) + 10);

        Font current = g2.getFont();
        g2.setFont(new Font("Montserrat", Font.BOLD, 25));

        String gatewayMarker = "";

        if (node instanceof Split) {
            switch (((Split) node).getType()) {
                case Split.TYPE_XOR:
                    gatewayMarker = "X";
                    break;
                case Split.TYPE_AND:
                    gatewayMarker = "+";
                    break;

                case Split.TYPE_OR:
                    gatewayMarker = "o";
                    break;
                default:
                    break;
            }
        } else if (node instanceof Join) {
            switch (((Join) node).getType()) {
                case Join.TYPE_XOR:
                    gatewayMarker = "X";
                    break;
                case Join.TYPE_AND:
                    gatewayMarker = "+";
                    break;

                case Join.TYPE_OR:
                    gatewayMarker = "o";
                    break;
                default:
                    break;
            }
        }

        drawCenteredString(g2, gatewayMarker, p.getBounds(), g2.getFont(), 0);
        g2.setFont(current);

    }

    protected void buildSubprocessNode(int x, int y, CompositeNode node, SVGGraphics2D g2)
            throws IOException {
        setNodeId(node, g2);
        x += x(node);
        y += y(node);
        int width = width(node);
        int height = height(node);

        if (node instanceof EventSubProcessNode) {
            g2.setStroke(dotted);
        }

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, 10, 10);
        g2.setPaint(new Color(0, 0, 0));
        g2.draw(roundedRectangle);
        setTextNodeId(node, g2);
        g2.drawString(node.getName(), x + 10, y + 10);
        g2.setStroke(defaultStroke);

        drawWarningPlaceholderIcon(node, g2, roundedRectangle.getBounds());
    }

    @SuppressWarnings("unchecked")
    protected void buildSequenceFlow(int x, int y, Node node, SVGGraphics2D g2) {
        g2.setPaint(new Color(0, 0, 0));
        List<Connection> outgoing = node
                .getOutgoingConnections(io.automatiko.engine.workflow.process.core.Node.CONNECTION_DEFAULT_TYPE);

        if (outgoing != null && !outgoing.isEmpty()) {

            for (Connection connection : outgoing) {

                if (connection.getMetaData().get("x") != null && connection.getMetaData().get("y") != null) {
                    int[] linestart = ((List<Integer>) connection.getMetaData().get("x")).stream().mapToInt(Integer::intValue)
                            .toArray();

                    int[] lineend = ((List<Integer>) connection.getMetaData().get("y")).stream().mapToInt(Integer::intValue)
                            .toArray();
                    if (connection.getMetaData().get("association") != null) {
                        g2.setStroke(dashed);
                    }
                    g2.drawPolyline(linestart, lineend, linestart.length);
                    drawArrowLine(g2, linestart[0], lineend[0], linestart[linestart.length - 1], lineend[lineend.length - 1], 5,
                            5);
                    g2.setStroke(defaultStroke);
                }
            }
        }
    }

    /*
     * Helper methods
     */

    protected int x(Node node) {
        return (int) node.getMetaData().get("x");
    }

    protected int y(Node node) {
        return (int) node.getMetaData().get("y");
    }

    protected int width(Node node) {
        int width = (int) node.getMetaData().get("x") + (int) node.getMetaData().get("width");
        if (width > maxWidth) {
            maxWidth = width;
        }
        return (int) node.getMetaData().get("width");
    }

    protected int height(Node node) {
        int height = (int) node.getMetaData().get("y") + (int) node.getMetaData().get("height");
        if (height > maxHeight) {
            maxHeight = height;
        }
        return (int) node.getMetaData().get("height");
    }

    /**
     * Draw an arrow line between two points.
     * 
     * @param g the graphics component.
     * @param x1 x-position of first point.
     * @param y1 y-position of first point.
     * @param x2 x-position of second point.
     * @param y2 y-position of second point.
     * @param d the width of the arrow.
     * @param h the height of the arrow.
     */
    private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx * dx + dy * dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;

        x = xm * cos - ym * sin + x1;
        ym = xm * sin + ym * cos + y1;
        xm = x;

        x = xn * cos - yn * sin + x1;
        yn = xn * sin + yn * cos + y1;
        xn = x;

        int[] xpoints = { x2, (int) xm, (int) xn };
        int[] ypoints = { y2, (int) ym, (int) yn };

        g.fillPolygon(xpoints, ypoints, 3);
    }

    public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font, int extraY) {
        if (text == null) {
            return;
        }
        // Get the FontMetrics
        FontMetrics metrics = g.getFontMetrics(font);
        int pad = 0;
        int textLength = metrics.stringWidth(text);
        int maxTextWidth = (int) (rect.getWidth() - 20);
        if (extraY == 0 && textLength > maxTextWidth) {
            double singleCharLength = textLength / text.length();
            int max = (int) (maxTextWidth / singleCharLength);
            if (text.length() > max) {
                text = text.substring(0, max) + "...";
                pad = 5;
            }
        }

        // Determine the X coordinate for the text
        int x = (int) (rect.getX() + (rect.getWidth() - metrics.stringWidth(text)) / 2);
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = (int) (rect.getY() + ((rect.getHeight() - metrics.getHeight()) / 2) + metrics.getAscent());
        // Set the font
        g.setFont(font);
        // Draw the String
        g.drawString(text, x + pad, y + extraY);
    }

    public void drawCenteredIcon(Graphics g2, Rectangle rect, String icon) throws IOException {
        BufferedImage image = ImageIO.read(getClass().getResource("/icons/" + icon));
        // Determine the X coordinate for the text
        int ix = (int) (rect.getX() + (rect.getWidth() - image.getWidth()) / 2);
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int iy = (int) (rect.getY() + ((rect.getHeight() - image.getHeight()) / 2));

        g2.drawImage(image, ix, iy, null);
    }

    public void drawWarningPlaceholderIcon(Node node, SVGGraphics2D g2, Rectangle rect) throws IOException {
        BufferedImage warnImage = ImageIO.read(getClass().getResource("/icons/empty.png"));
        int wix = (int) (rect.getX() + rect.getWidth() - 23);
        int wiy = (int) (rect.getY() + 3);
        setWarningImageNodeId(node, g2);
        g2.drawImage(warnImage, wix, wiy, 20, 20, null);
    }

    protected void setTextNodeId(Node node, SVGGraphics2D g2) {
        g2.setRenderingHint(SVGHints.KEY_ELEMENT_ID, node.getMetaData().get("UniqueId") + "_text");
    }

    protected void setImageNodeId(Node node, SVGGraphics2D g2) {
        g2.setRenderingHint(SVGHints.KEY_ELEMENT_ID, node.getMetaData().get("UniqueId") + "_image");
    }

    protected void setWarningImageNodeId(Node node, SVGGraphics2D g2) {
        g2.setRenderingHint(SVGHints.KEY_ELEMENT_ID, node.getMetaData().get("UniqueId") + "_warn_image");
    }

    protected void setNodeId(Node node, SVGGraphics2D g2) {
        g2.setRenderingHint(SVGHints.KEY_ELEMENT_ID, node.getMetaData().get("UniqueId"));
    }
}
