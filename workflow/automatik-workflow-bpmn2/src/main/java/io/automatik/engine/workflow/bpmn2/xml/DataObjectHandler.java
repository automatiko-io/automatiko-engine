
package io.automatik.engine.workflow.bpmn2.xml;

import static io.automatik.engine.workflow.compiler.util.ClassUtils.constructClass;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import io.automatik.engine.api.workflow.datatype.DataType;
import io.automatik.engine.workflow.base.core.ContextContainer;
import io.automatik.engine.workflow.base.core.context.variable.Variable;
import io.automatik.engine.workflow.base.core.context.variable.VariableScope;
import io.automatik.engine.workflow.base.core.datatype.impl.type.BooleanDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.FloatDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.IntegerDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatik.engine.workflow.base.core.datatype.impl.type.StringDataType;
import io.automatik.engine.workflow.bpmn2.core.ItemDefinition;
import io.automatik.engine.workflow.bpmn2.core.SequenceFlow;
import io.automatik.engine.workflow.compiler.xml.BaseAbstractHandler;
import io.automatik.engine.workflow.compiler.xml.ExtensibleXmlParser;
import io.automatik.engine.workflow.compiler.xml.Handler;
import io.automatik.engine.workflow.compiler.xml.ProcessBuildData;
import io.automatik.engine.workflow.process.core.Node;

public class DataObjectHandler extends BaseAbstractHandler implements Handler {

    public DataObjectHandler() {
        initValidParents();
        initValidPeers();
        this.allowNesting = false;
    }

    protected void initValidParents() {
        this.validParents = new HashSet<Class<?>>();
        this.validParents.add(ContextContainer.class);
        this.validParents.add(Node.class);
    }

    protected void initValidPeers() {
        this.validPeers = new HashSet<Class<?>>();
        this.validPeers.add(null);
        this.validPeers.add(Variable.class);
        this.validPeers.add(Node.class);
        this.validPeers.add(SequenceFlow.class);
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

            if (variableScope == null) {
                return null;
            }

            List variables = variableScope.getVariables();
            Variable variable = new Variable();
            variable.setMetaData("DataObject", "true");
            variable.setId(id);
            variable.setName(name);
            variable.setMetaData(id, variable.getName());

            if (localName.equals("dataInput")) {
                variable.setMetaData("DataInput", true);
            } else if (localName.equals("dataOutput")) {
                variable.setMetaData("DataOutput", true);
            }

            // retrieve type from item definition
            DataType dataType = new ObjectDataType();
            Map<String, ItemDefinition> itemDefinitions = (Map<String, ItemDefinition>) ((ProcessBuildData) parser
                    .getData()).getMetaData("ItemDefinitions");
            if (itemDefinitions != null) {
                ItemDefinition itemDefinition = itemDefinitions.get(itemSubjectRef);
                if (itemDefinition != null) {

                    String structureRef = itemDefinition.getStructureRef();

                    if ("java.lang.Boolean".equals(structureRef) || "Boolean".equals(structureRef)) {
                        dataType = new BooleanDataType();

                    } else if ("java.lang.Integer".equals(structureRef) || "Integer".equals(structureRef)) {
                        dataType = new IntegerDataType();

                    } else if ("java.lang.Float".equals(structureRef) || "Float".equals(structureRef)) {
                        dataType = new FloatDataType();

                    } else if ("java.lang.String".equals(structureRef) || "String".equals(structureRef)) {
                        dataType = new StringDataType();

                    } else if ("java.lang.Object".equals(structureRef) || "Object".equals(structureRef)) {
                        // use FQCN of Object
                        dataType = new ObjectDataType(java.lang.Object.class, structureRef);

                    } else {
                        dataType = new ObjectDataType(constructClass(structureRef, parser.getClassLoader()),
                                structureRef);
                    }
                }
            }
            variable.setType(dataType);
            variables.add(variable);
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
