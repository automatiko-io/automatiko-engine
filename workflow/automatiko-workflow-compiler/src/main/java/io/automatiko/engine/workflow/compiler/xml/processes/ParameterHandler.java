
package io.automatiko.engine.workflow.compiler.xml.processes;

import java.util.HashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatiko.engine.api.workflow.datatype.DataType;
import io.automatiko.engine.workflow.base.core.ParameterDefinition;
import io.automatiko.engine.workflow.base.core.TypeObject;
import io.automatiko.engine.workflow.base.core.ValueObject;
import io.automatiko.engine.workflow.base.core.Work;
import io.automatiko.engine.workflow.base.core.impl.ParameterDefinitionImpl;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;

public class ParameterHandler extends BaseAbstractHandler implements Handler {

	public ParameterHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(Work.class);
			this.validPeers = new HashSet<Class<?>>();
			this.validPeers.add(null);
			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		final String name = attrs.getValue("name");
		emptyAttributeCheck(localName, "name", name, parser);
		Work work = (Work) parser.getParent();
		ParameterDefinition parameterDefinition = new ParameterDefinitionImpl();
		parameterDefinition.setName(name);
		work.addParameterDefinition(parameterDefinition);
		return new ParameterWrapper(parameterDefinition, work);
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return null;
	}

	public Class<?> generateNodeFor() {
		return ParameterWrapper.class;
	}

	public class ParameterWrapper implements TypeObject, ValueObject {
		private Work work;
		private ParameterDefinition parameterDefinition;

		public ParameterWrapper(ParameterDefinition parameterDefinition, Work work) {
			this.work = work;
			this.parameterDefinition = parameterDefinition;
		}

		public DataType getType() {
			return parameterDefinition.getType();
		}

		public void setType(DataType type) {
			parameterDefinition.setType(type);
		}

		public Object getValue() {
			return work.getParameter(parameterDefinition.getName());
		}

		public void setValue(Object value) {
			work.setParameter(parameterDefinition.getName(), value);
		}
	}

}
