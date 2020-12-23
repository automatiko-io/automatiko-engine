
package io.automatiko.engine.workflow.bpmn2.xml;

import org.xml.sax.Attributes;

import io.automatiko.engine.workflow.base.core.timer.Timer;
import io.automatiko.engine.workflow.compiler.xml.XmlDumper;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.node.TimerNode;

public class TimerNodeHandler extends AbstractNodeHandler {

	protected Node createNode(Attributes attrs) {
		throw new IllegalArgumentException("Reading in should be handled by intermediate catch event handler");
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return TimerNode.class;
	}

	public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
		TimerNode timerNode = (TimerNode) node;
		writeNode("intermediateCatchEvent", timerNode, xmlDump, metaDataType);
		xmlDump.append(">" + EOL);
		writeExtensionElements(node, xmlDump);
		xmlDump.append("      <timerEventDefinition>" + EOL);
		Timer timer = timerNode.getTimer();
		if (timer != null && (timer.getDelay() != null || timer.getDate() != null)) {
			if (timer.getTimeType() == Timer.TIME_DURATION) {
				xmlDump.append("        <timeDuration xsi:type=\"tFormalExpression\">"
						+ XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeDuration>" + EOL);
			} else if (timer.getTimeType() == Timer.TIME_CYCLE) {

				if (timer.getPeriod() != null) {
					xmlDump.append("        <timeCycle xsi:type=\"tFormalExpression\">"
							+ XmlDumper.replaceIllegalChars(timer.getDelay()) + "###"
							+ XmlDumper.replaceIllegalChars(timer.getPeriod()) + "</timeCycle>" + EOL);
				} else {
					xmlDump.append("        <timeCycle xsi:type=\"tFormalExpression\">"
							+ XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeCycle>" + EOL);
				}
			} else if (timer.getTimeType() == Timer.TIME_DATE) {
				xmlDump.append("        <timeDate xsi:type=\"tFormalExpression\">"
						+ XmlDumper.replaceIllegalChars(timer.getDelay()) + "</timeDate>" + EOL);
			}
		}
		xmlDump.append("      </timerEventDefinition>" + EOL);
		endNode("intermediateCatchEvent", xmlDump);
	}

}
