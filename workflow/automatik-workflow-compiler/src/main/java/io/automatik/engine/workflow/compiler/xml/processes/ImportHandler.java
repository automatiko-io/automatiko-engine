
package io.automatik.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.impl.WorkflowProcessImpl;

public class ImportHandler extends BaseAbstractHandler implements Handler {
	public ImportHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(Process.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		WorkflowProcessImpl process = (WorkflowProcessImpl) parser.getParent();

		final String name = attrs.getValue("name");
		emptyAttributeCheck(localName, "name", name, parser);

		java.util.Set<String> list = process.getImports();
		if (list == null) {
			list = new HashSet<String>();
			process.setImports(list);
		}
		list.add(name);

		return null;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		final Element element = parser.endElementBuilder();
		return null;
	}

	public Class generateNodeFor() {
		return null;
	}

}
