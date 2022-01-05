
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory.METHOD_JQ_IN_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory.METHOD_JQ_OUT_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory.METHOD_VARIABLE;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.instance.impl.jq.InputJqAssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.OutputJqAssignmentAction;
import io.automatiko.engine.workflow.compiler.util.ClassUtils;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.executable.core.factory.CompositeContextNodeFactory;

public class CompositeContextNodeVisitor<T extends CompositeContextNode> extends AbstractCompositeNodeVisitor<T> {

    public CompositeContextNodeVisitor(Map<Class<?>, AbstractNodeVisitor<? extends Node>> nodesVisitors) {
        super(nodesVisitors);
    }

    @Override
    protected String getNodeKey() {
        return "compositeContextNode";
    }

    protected Class<? extends CompositeContextNodeFactory> factoryClass() {
        return CompositeContextNodeFactory.class;
    }

    protected String factoryMethod() {
        return getNodeKey();
    }

    protected String getDefaultName() {
        return "Composite";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, T node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, factoryClass(), getNodeId(node), factoryMethod(),
                new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, getDefaultName()));
        visitMetaData(node.getMetaData(), body, getNodeId(node));
        VariableScope variableScopeNode = (VariableScope) node.getDefaultContext(VariableScope.VARIABLE_SCOPE);

        if (variableScope != null) {
            visitVariableScope(getNodeId(node), variableScopeNode, body, new HashSet<>());
        }
        visitDataMapping(process, getNodeId(node), node, body);
        visitCustomFields(node, variableScope).forEach(body::addStatement);

        // composite context node might not have variable scope
        // in that case inherit it from parent
        VariableScope scope = variableScope;
        if (node.getDefaultContext(VariableScope.VARIABLE_SCOPE) != null
                && !((VariableScope) node.getDefaultContext(VariableScope.VARIABLE_SCOPE)).getVariables().isEmpty()) {
            scope = (VariableScope) node.getDefaultContext(VariableScope.VARIABLE_SCOPE);
        }
        body.addStatement(getFactoryMethod(getNodeId(node), CompositeContextNodeFactory.METHOD_AUTO_COMPLETE,
                new BooleanLiteralExpr(node.isAutoComplete())));
        visitNodes(process, getNodeId(node), node.getNodes(), body, scope, metadata);
        visitConnections(getNodeId(node), node.getNodes(), body);
        body.addStatement(getDoneMethod(getNodeId(node)));
    }

    protected Stream<MethodCallExpr> visitCustomFields(T compositeContextNode, VariableScope variableScope) {
        return Stream.empty();
    }

    protected void visitVariableScope(String contextNode, VariableScope variableScope, BlockStmt body,
            Set<String> visitedVariables) {
        if (variableScope != null && !variableScope.getVariables().isEmpty()) {
            for (Variable variable : variableScope.getVariables()) {
                if (!visitedVariables.add(variable.getName())) {
                    continue;
                }
                String tags = (String) variable.getMetaData(Variable.VARIABLE_TAGS);
                ClassOrInterfaceType variableType = new ClassOrInterfaceType(null,
                        ObjectDataType.class.getSimpleName());
                ObjectCreationExpr variableValue = new ObjectCreationExpr(null, variableType,
                        new NodeList<>(
                                new ClassExpr(new ClassOrInterfaceType(null,
                                        ClassUtils.parseClassname(variable.getType().getStringType()))),
                                new StringLiteralExpr(variable.getType().getStringType())));
                body.addStatement(
                        getFactoryMethod(contextNode, METHOD_VARIABLE, new StringLiteralExpr(variable.getName()),
                                variableValue, new StringLiteralExpr(Variable.VARIABLE_TAGS),
                                (tags != null ? new StringLiteralExpr(tags) : new NullLiteralExpr())));
            }
        }
    }

    protected void visitDataMapping(WorkflowProcess process, String contextNode, T node, BlockStmt body) {
        boolean serverless = ProcessToExecModelGenerator.isServerlessWorkflow(process);

        if (serverless) {
            for (DataAssociation association : node.getInAssociations()) {

                if (association.getAssignments() != null && !association.getAssignments().isEmpty()) {
                    InputJqAssignmentAction action = (InputJqAssignmentAction) association.getAssignments().get(0)
                            .getMetaData("Action");

                    String inputFilter = action.getInputFilterExpression();
                    body.addStatement(
                            getFactoryMethod(contextNode, METHOD_JQ_IN_MAPPING,
                                    (inputFilter != null ? new StringLiteralExpr().setString(inputFilter)
                                            : new NullLiteralExpr())));
                }
            }
            for (DataAssociation association : node.getOutAssociations()) {

                if (association.getAssignments() != null && !association.getAssignments().isEmpty()) {
                    OutputJqAssignmentAction action = (OutputJqAssignmentAction) association.getAssignments().get(0)
                            .getMetaData("Action");

                    String outputFilter = action.getOutputFilterExpression();
                    body.addStatement(
                            getFactoryMethod(contextNode, METHOD_JQ_OUT_MAPPING,
                                    (outputFilter != null ? new StringLiteralExpr().setString(outputFilter)
                                            : new NullLiteralExpr())));
                }
            }
        }
    }

}
