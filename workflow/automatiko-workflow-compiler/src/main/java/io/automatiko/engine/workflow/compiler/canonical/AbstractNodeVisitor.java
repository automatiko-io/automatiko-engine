
package io.automatiko.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.CUSTOM_AUTO_START;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.HIDDEN;
import static io.automatiko.engine.workflow.process.executable.core.factory.MappableNodeFactory.METHOD_IN_JQ_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.MappableNodeFactory.METHOD_IN_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.MappableNodeFactory.METHOD_OUT_JQ_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.MappableNodeFactory.METHOD_OUT_MAPPING;
import static io.automatiko.engine.workflow.process.executable.core.factory.NodeFactory.METHOD_DONE;
import static io.automatiko.engine.workflow.process.executable.core.factory.NodeFactory.METHOD_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Mappable;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskInputJqAssignmentAction;
import io.automatiko.engine.workflow.base.instance.impl.jq.TaskOutputJqAssignmentAction;
import io.automatiko.engine.workflow.process.core.impl.ConnectionImpl;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.StartNode;

public abstract class AbstractNodeVisitor<T extends Node> extends AbstractVisitor {

    protected abstract String getNodeKey();

    public void visitNode(WorkflowProcess process, T node, BlockStmt body, VariableScope variableScope,
            ProcessMetaData metadata) {
        visitNode(process, FACTORY_FIELD_NAME, node, body, variableScope, metadata);
        if (isAdHocNode(node) && !(node instanceof HumanTaskNode) && !(node instanceof BoundaryEventNode)
                && !(node instanceof EventSubProcessNode)) {
            metadata.addSignal(node.getName(), null, node);
        }
    }

    private boolean isAdHocNode(Node node) {
        return (node.getIncomingConnections() == null || node.getIncomingConnections().isEmpty())
                && !(node instanceof StartNode)
                && !Boolean.parseBoolean((String) node.getMetaData().get(CUSTOM_AUTO_START));
    }

    protected String getNodeId(T node) {
        String parentId = node.getParentContainer() instanceof WorkflowProcess ? ""
                : String.valueOf(node.getParentContainer().getNodes().length);
        return getNodeKey() + node.getId() + parentId;
    }

    public void visitNode(WorkflowProcess process, String factoryField, T node, BlockStmt body,
            VariableScope variableScope, ProcessMetaData metadata) {
    }

    protected MethodCallExpr getNameMethod(T node, String defaultName) {
        return getFactoryMethod(getNodeId(node), METHOD_NAME,
                new StringLiteralExpr(getOrDefault(node.getName(), defaultName)));
    }

    protected MethodCallExpr getDoneMethod(String object) {
        return getFactoryMethod(object, METHOD_DONE);
    }

    protected AssignExpr getAssignedFactoryMethod(String factoryField, Class<?> typeClass, String variableName,
            String methodName, Expression... args) {
        ClassOrInterfaceType type = new ClassOrInterfaceType(null, typeClass.getCanonicalName());

        MethodCallExpr variableMethod = new MethodCallExpr(new NameExpr(factoryField), methodName);

        for (Expression arg : args) {
            variableMethod.addArgument(arg);
        }

        return new AssignExpr(new VariableDeclarationExpr(type, variableName), variableMethod,
                AssignExpr.Operator.ASSIGN);
    }

    public static Statement makeAssignment(Variable v) {
        String name = v.getSanitizedName();
        return makeAssignment(name, v);
    }

    public static Statement makeAssignment(String targetLocalVariable, Variable processVariable) {
        ClassOrInterfaceType type = parseClassOrInterfaceType(processVariable.getType().getStringType());
        // `type` `name` = (`type`) `context.getVariable
        AssignExpr assignExpr = new AssignExpr(new VariableDeclarationExpr(type, targetLocalVariable),
                new CastExpr(type, new MethodCallExpr(new NameExpr(KCONTEXT_VAR), "getVariable")
                        .addArgument(new StringLiteralExpr(targetLocalVariable))),
                AssignExpr.Operator.ASSIGN);
        return new ExpressionStmt(assignExpr);
    }

    public static Statement makeAssignmentVersions(Variable processVariable) {
        String targetLocalVariable = processVariable.getSanitizedName();
        ClassOrInterfaceType type = new ClassOrInterfaceType(null, new SimpleName(List.class.getCanonicalName()),
                NodeList.nodeList(parseClassOrInterfaceType(processVariable.getType().getStringType())));
        // List<`type`> `name$` = (`type`) `context.getVariable
        AssignExpr assignExpr = new AssignExpr(new VariableDeclarationExpr(type, targetLocalVariable + "$"),
                new CastExpr(type, new MethodCallExpr(new NameExpr(KCONTEXT_VAR), "getVariable")
                        .addArgument(new StringLiteralExpr(targetLocalVariable + "$"))),
                AssignExpr.Operator.ASSIGN);
        return new ExpressionStmt(assignExpr);
    }

    public static Statement makeAssignmentFromMap(Variable v) {
        String name = v.getSanitizedName();
        return makeAssignmentFromMap(name, v);
    }

    public static Statement makeAssignmentFromMap(String targetLocalVariable, Variable processVariable) {
        ClassOrInterfaceType type = parseClassOrInterfaceType(processVariable.getType().getStringType());
        AssignExpr assignExpr = new AssignExpr(new VariableDeclarationExpr(type, targetLocalVariable),
                new CastExpr(type, new MethodCallExpr(new NameExpr("variables"), "get")
                        .addArgument(new StringLiteralExpr(targetLocalVariable))),
                AssignExpr.Operator.ASSIGN);
        return new ExpressionStmt(assignExpr);
    }

    protected Statement makeAssignmentFromModel(Variable v) {
        return makeAssignmentFromModel(v, v.getSanitizedName());
    }

    protected Statement makeAssignmentFromModel(Variable v, String name) {
        ClassOrInterfaceType type = parseClassOrInterfaceType(v.getType().getStringType());
        // `type` `name` = (`type`) `model.get<Name>
        AssignExpr assignExpr = new AssignExpr(new VariableDeclarationExpr(type, name),
                new CastExpr(type, new MethodCallExpr(new NameExpr("model"), "get" + StringUtils.capitalize(name))),
                AssignExpr.Operator.ASSIGN);

        return new ExpressionStmt(assignExpr);
    }

    protected Statement makeAssignmentFromJsonModel(Variable v, String name) {
        ClassOrInterfaceType jsonNodeType = parseClassOrInterfaceType("com.fasterxml.jackson.databind.JsonNode");
        ClassOrInterfaceType type = parseClassOrInterfaceType(v.getType().getStringType());
        // `type` `name` = (`type`) `model.get<Name>
        AssignExpr assignExpr = new AssignExpr(new VariableDeclarationExpr(type, name),
                new CastExpr(type,
                        new MethodCallExpr(null, "fromJsonNode")
                                .addArgument(new CastExpr(jsonNodeType,
                                        new MethodCallExpr(new NameExpr("model"), "getWorkflowdata")
                                                .addArgument(new StringLiteralExpr(name))))
                                .addArgument(new NameExpr(type.asString() + ".class"))),
                AssignExpr.Operator.ASSIGN);

        return new ExpressionStmt(assignExpr);
    }

    protected void addNodeMappings(WorkflowProcess process, Mappable node, BlockStmt body, String variableName) {

        boolean serverless = ProcessToExecModelGenerator.isServerlessWorkflow(process);

        if (serverless) {
            for (DataAssociation association : node.getInAssociations()) {

                if (association.getAssignments() != null && !association.getAssignments().isEmpty()) {
                    TaskInputJqAssignmentAction action = (TaskInputJqAssignmentAction) association.getAssignments().get(0)
                            .getMetaData("Action");

                    String inputFilter = action.getInputFilterExpression();
                    Set<String> params = action.getParamNames();

                    List<Expression> expressions = new ArrayList<>();

                    expressions
                            .add(inputFilter != null ? new StringLiteralExpr().setString(inputFilter) : new NullLiteralExpr());

                    for (String param : params) {

                        expressions.add(param != null ? new StringLiteralExpr().setString(param) : new NullLiteralExpr());

                    }

                    body.addStatement(
                            getFactoryMethod(variableName, METHOD_IN_JQ_MAPPING,
                                    expressions.toArray(Expression[]::new)));

                }
            }
            for (DataAssociation association : node.getOutAssociations()) {

                if (association.getAssignments() != null && !association.getAssignments().isEmpty()) {
                    TaskOutputJqAssignmentAction action = (TaskOutputJqAssignmentAction) association.getAssignments().get(0)
                            .getMetaData("Action");

                    String outputFilter = action.getOutputFilterExpression();
                    String scopeFilter = action.getScopeFilter();
                    body.addStatement(
                            getFactoryMethod(variableName, METHOD_OUT_JQ_MAPPING,
                                    (outputFilter != null ? new StringLiteralExpr().setString(outputFilter)
                                            : new NullLiteralExpr()),
                                    (scopeFilter != null ? new StringLiteralExpr().setString(scopeFilter)
                                            : new NullLiteralExpr()),
                                    new BooleanLiteralExpr(action.isIgnoreScopeFilter())));
                }
            }
        } else {

            for (Entry<String, String> entry : node.getInMappings().entrySet()) {
                body.addStatement(getFactoryMethod(variableName, METHOD_IN_MAPPING, new StringLiteralExpr(entry.getKey()),
                        new StringLiteralExpr(entry.getValue())));
            }
            for (Entry<String, String> entry : node.getOutMappings().entrySet()) {
                body.addStatement(getFactoryMethod(variableName, METHOD_OUT_MAPPING, new StringLiteralExpr(entry.getKey()),
                        new StringLiteralExpr(entry.getValue())));
            }
        }
    }

    protected String extractVariableFromExpression(String variableExpression) {
        if (variableExpression.startsWith("#{")) {
            return variableExpression.substring(2, variableExpression.indexOf('.'));
        }
        return variableExpression;
    }

    protected void visitConnections(String factoryField, Node[] nodes, BlockStmt body) {
        List<Connection> connections = new ArrayList<>();
        for (Node node : nodes) {
            for (List<Connection> connectionList : node.getIncomingConnections().values()) {
                connections.addAll(connectionList);
            }
        }
        for (Connection connection : connections) {
            visitConnection(factoryField, connection, body);
        }
    }

    protected void visitConnection(String factoryField, Connection connection, BlockStmt body) {
        // if the connection is a hidden one (compensations), don't dump
        Object hidden = ((ConnectionImpl) connection).getMetaData(HIDDEN);
        if (hidden != null && ((Boolean) hidden)) {
            return;
        }

        body.addStatement(getFactoryMethod(factoryField, "connection",
                new LongLiteralExpr(connection.getFrom().getId()), new LongLiteralExpr(connection.getTo().getId()),
                new StringLiteralExpr(getOrDefault((String) connection.getMetaData().get("UniqueId"), ""))));
    }

    protected static LambdaExpr createLambdaExpr(String consequence, VariableScope scope) {
        BlockStmt conditionBody = new BlockStmt();
        List<Variable> variables = scope.getVariables();
        variables.stream().map(ActionNodeVisitor::makeAssignment).forEach(conditionBody::addStatement);

        variables.stream().filter(v -> v.hasTag(Variable.VERSIONED_TAG)).map(ActionNodeVisitor::makeAssignmentVersions)
                .forEach(conditionBody::addStatement);

        conditionBody.addStatement(new ReturnStmt(new EnclosedExpr(new NameExpr(consequence))));

        return new LambdaExpr(new Parameter(new UnknownType(), KCONTEXT_VAR), // (kcontext) ->
                conditionBody);
    }

}
