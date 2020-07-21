
package io.automatik.engine.workflow.bpmn2.xml;

import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.bpmn2.core.Association;
import io.automatik.engine.workflow.bpmn2.core.Lane;
import io.automatik.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.core.Node;
import io.automatik.engine.workflow.process.core.node.WorkItemNode;

public class PropertyHandler extends BaseAbstractHandler implements Handler {

	public PropertyHandler() {
		initValidParents();
		initValidPeers();
		this.allowNesting = false;
	}

	protected void initValidParents() {
		this.validParents = new HashSet<Class<?>>();
		this.validParents.add(ContextContainer.class);
		this.validParents.add(WorkItemNode.class);
	}

	protected void initValidPeers() {
		this.validPeers = new HashSet<Class<?>>();
		this.validPeers.add(null);
		this.validPeers.add(Lane.class);
		this.validPeers.add(Variable.class);
		this.validPeers.add(Node.class);
		this.validPeers.add(SequenceFlow.class);
		this.validPeers.add(Lane.class);
		this.validPeers.add(Association.class);
	}

	@SuppressWarnings("unchecked")
	public Object start(final String uri, final String localName, final Attributes attrs,
			final ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);

		final String id = attrs.getValue("id");
		final String name = attrs.getValue("name");
		final String itemSubjectRef = attrs.getValue("itemSubjectRef");

		Object parent = parser.getParent();
		if (parent instanceof ContextContainer) {
			ContextContainer contextContainer = (ContextContainer) parent;
			VariableScope variableScope = (VariableScope) contextContainer
					.getDefaultContext(VariableScope.VARIABLE_SCOPE);
			List variables = variableScope.getVariables();
			Variable variable = new Variable();
			variable.setId(id);
			// if name is given use it as variable name instead of id
			if (name != null && name.length() > 0) {
				variable.setName(name);
				variable.setMetaData(name, variable.getName());
			} else {
				variable.setName(id);
			}
			variable.setMetaData("ItemSubjectRef", itemSubjectRef);
			variable.setMetaData(id, variable.getName());
			variables.add(variable);

			((ProcessBuildData) parser.getData()).setMetaData("Variable", variable);
			return variable;
		}

		return new Variable();
	}

	public Object end(final String uri, final String localName, final ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	public Class<?> generateNodeFor() {
		return Variable.class;
	}

}
