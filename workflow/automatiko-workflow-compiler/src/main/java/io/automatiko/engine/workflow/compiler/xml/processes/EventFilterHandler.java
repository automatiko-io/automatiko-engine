
package io.automatiko.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;

public class EventFilterHandler extends BaseAbstractHandler implements Handler {

	public EventFilterHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(EventNode.class);
			this.validParents.add(EventTrigger.class);

			this.validPeers = new HashSet<Class<?>>();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		Object parent = parser.getParent();
		final String type = element.getAttribute("type");
		emptyAttributeCheck(localName, "type", type, parser);
		if ("eventType".equals(type)) {
			final String eventType = element.getAttribute("eventType");
			emptyAttributeCheck(localName, "eventType", eventType, parser);
			EventTypeFilter eventTypeFilter = new EventTypeFilter();
			eventTypeFilter.setType(eventType);
			if (parent instanceof EventNode) {
				((EventNode) parent).addEventFilter(eventTypeFilter);
			} else if (parent instanceof EventTrigger) {
				((EventTrigger) parent).addEventFilter(eventTypeFilter);
			}
		} else {
			throw new IllegalArgumentException("Unknown event filter type: " + type);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Class generateNodeFor() {
		return EventFilter.class;
	}

}
