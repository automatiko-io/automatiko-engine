
package io.automatik.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.Work;
import io.automatik.engine.workflow.base.core.impl.WorkImpl;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class WorkHandler extends BaseAbstractHandler implements Handler {

	public WorkHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(WorkItemNode.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		WorkItemNode workItemNode = (WorkItemNode) parser.getParent();
		final String name = attrs.getValue("name");
		emptyAttributeCheck(localName, "name", name, parser);
		Work work = new WorkImpl();
		work.setName(name);
		workItemNode.setWork(work);
		return work;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return null;
	}

	public Class generateNodeFor() {
		return Work.class;
	}

}
