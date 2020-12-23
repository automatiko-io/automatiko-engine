
package io.automatiko.engine.workflow.compiler.xml.processes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatiko.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatiko.engine.workflow.compiler.xml.Handler;

public class VariableHandler extends BaseAbstractHandler implements Handler {
	public VariableHandler() {
		if ((this.validParents == null) && (this.validPeers == null)) {
			this.validParents = new HashSet<Class<?>>();
			this.validParents.add(ContextContainer.class);

			this.validPeers = new HashSet<Class<?>>();
			this.validPeers.add(null);

			this.allowNesting = false;
		}
	}

	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		ContextContainer contextContainer = (ContextContainer) parser.getParent();
		final String name = attrs.getValue("name");
		emptyAttributeCheck(localName, "name", name, parser);

		VariableScope variableScope = (VariableScope) contextContainer.getDefaultContext(VariableScope.VARIABLE_SCOPE);
		Variable variable = new Variable();
		if (variableScope != null) {
			variable.setName(name);
			List<Variable> variables = variableScope.getVariables();
			if (variables == null) {
				variables = new ArrayList<Variable>();
				variableScope.setVariables(variables);
			}
			variables.add(variable);
		} else {
			throw new SAXParseException("Could not find default variable scope.", parser.getLocator());
		}

		return variable;
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return null;
	}

	public Class<?> generateNodeFor() {
		return Variable.class;
	}

}
