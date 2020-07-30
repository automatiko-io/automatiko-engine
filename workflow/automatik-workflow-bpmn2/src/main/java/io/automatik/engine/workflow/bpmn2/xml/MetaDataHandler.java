
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.HashSet;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.ValueObject;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.datatype.DataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.StringDataType;
import io.automatik.engine.workflow.bpmn2.core.Lane;
import io.automatik.engine.workflow.bpmn2.core.Message;
import io.automatik.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.executable.core.ExecutableProcess;

public class MetaDataHandler extends BaseAbstractHandler implements Handler {
	public MetaDataHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet();
			this.validParents.add(Node.class);
			this.validParents.add(ExecutableProcess.class);
			this.validParents.add(Variable.class);
			this.validParents.add(SequenceFlow.class);
			this.validParents.add(Lane.class);
			this.validParents.add(Message.class);

			this.validPeers = new HashSet();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		Object parent = parser.getParent();
		final String name = attrs.getValue("name");
		emptyAttributeCheck(localName, "name", name, parser);
		return new MetaDataWrapper(parent, name);
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return null;
	}

	public Class generateNodeFor() {
		return MetaDataWrapper.class;
	}

	public class MetaDataWrapper implements ValueObject {
		private Object parent;
		private String name;

		public MetaDataWrapper(Object parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		public Object getValue() {
			return getMetaData().get(name);
		}

		public void setValue(Object value) {
			getMetaData().put(name, value);
		}

		public Map<String, Object> getMetaData() {
			if (parent instanceof Node) {
				return ((Node) parent).getMetaData();
			} else if (parent instanceof ExecutableProcess) {
				return ((ExecutableProcess) parent).getMetaData();
			} else if (parent instanceof Variable) {
				return ((Variable) parent).getMetaData();
			} else if (parent instanceof SequenceFlow) {
				return ((SequenceFlow) parent).getMetaData();
			} else if (parent instanceof Lane) {
				return ((Lane) parent).getMetaData();
			} else if (parent instanceof Message) {
				return ((Message) parent).getMetaData();
			} else {
				throw new IllegalArgumentException("Unknown parent " + parent);
			}
		}

		public DataType getType() {
			return new StringDataType();
		}

		public void setType(DataType type) {
		}
	}

}
