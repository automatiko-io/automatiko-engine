
package io.automatik.engine.workflow.compiler.xml.processes;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.timer.Timer;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.TimerNode;

public class TimerNodeHandler extends AbstractNodeHandler {

	protected Node createNode() {
		return new TimerNode();
	}

	public void handleNode(final Node node, final Element element, final String uri, final String localName,
			final ExtensibleXmlParser parser) throws SAXException {
		super.handleNode(node, element, uri, localName, parser);
		TimerNode timerNode = (TimerNode) node;
		String delay = element.getAttribute("delay");
		String period = element.getAttribute("period");
		if ((delay != null && delay.length() > 0) || (period != null && period.length() > 0)) {
			Timer timer = timerNode.getTimer();
			if (timer == null) {
				timer = new Timer();
				timerNode.setTimer(timer);
			}
			if (delay != null && delay.length() != 0) {
				timer.setDelay(delay);
			}
			if (period != null && period.length() != 0) {
				timer.setPeriod(period);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return TimerNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, boolean includeMeta) {
		TimerNode timerNode = (TimerNode) node;
		writeNode("timerNode", timerNode, xmlDump, includeMeta);
		Timer timer = timerNode.getTimer();
		if (timer != null) {
			xmlDump.append("delay=\"" + timer.getDelay() + "\" ");
			if (timer.getPeriod() != null) {
				xmlDump.append(" period=\"" + timer.getPeriod() + "\" ");
			}
		}
		if (includeMeta && containsMetaData(timerNode)) {
			xmlDump.append(">" + EOL);
			writeMetaData(timerNode, xmlDump);
			endNode("timerNode", xmlDump);
		} else {
			endNode(xmlDump);
		}
	}

}
