
package io.automatiko.engine.workflow.compiler.canonical;

import static io.automatiko.engine.workflow.process.executable.core.ExecutableNodeContainerFactory.METHOD_CONNECTION;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_DYNAMIC;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_GLOBAL;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_IMPORTS;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_NAME;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_PACKAGE_NAME;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VALIDATE;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VERSION;
import static io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory.METHOD_VISIBILITY;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.LINK_NODE_HIDDEN;
import static io.automatiko.engine.workflow.process.executable.core.Metadata.UNIQUE_ID;
import static io.automatiko.engine.workflow.process.executable.core.factory.NodeFactory.METHOD_DONE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import io.automatiko.engine.api.definition.process.Connection;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.workflow.base.core.ContextContainer;
import io.automatiko.engine.workflow.base.core.FunctionTagDefinition;
import io.automatiko.engine.workflow.base.core.TagDefinition;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.compiler.util.ClassUtils;
import io.automatiko.engine.workflow.process.core.Node;
import io.automatiko.engine.workflow.process.core.NodeContainer;
import io.automatiko.engine.workflow.process.core.node.ActionNode;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.CompositeContextNode;
import io.automatiko.engine.workflow.process.core.node.DynamicNode;
import io.automatiko.engine.workflow.process.core.node.EndNode;
import io.automatiko.engine.workflow.process.core.node.EventNode;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.FaultNode;
import io.automatiko.engine.workflow.process.core.node.ForEachNode;
import io.automatiko.engine.workflow.process.core.node.HumanTaskNode;
import io.automatiko.engine.workflow.process.core.node.Join;
import io.automatiko.engine.workflow.process.core.node.MilestoneNode;
import io.automatiko.engine.workflow.process.core.node.RuleSetNode;
import io.automatiko.engine.workflow.process.core.node.Split;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.StateNode;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.core.node.TimerNode;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;
import io.automatiko.engine.workflow.process.executable.core.ExecutableProcessFactory;
import io.automatiko.engine.workflow.process.executable.core.factory.VariableFactory;
import io.automatiko.engine.workflow.util.PatternConstants;

public class ProcessVisitor extends AbstractVisitor {

    public static final String DEFAULT_VERSION = "";

    private Map<Class<?>, AbstractNodeVisitor<? extends io.automatiko.engine.api.definition.process.Node>> nodesVisitors = new HashMap<>();

    public ProcessVisitor(ClassLoader contextClassLoader) {

        this.nodesVisitors.put(StartNode.class, new StartNodeVisitor());
        this.nodesVisitors.put(ActionNode.class, new ActionNodeVisitor());
        this.nodesVisitors.put(EndNode.class, new EndNodeVisitor());
        this.nodesVisitors.put(HumanTaskNode.class, new HumanTaskNodeVisitor());
        this.nodesVisitors.put(WorkItemNode.class, new WorkItemNodeVisitor<>(contextClassLoader));
        this.nodesVisitors.put(SubProcessNode.class, new LambdaSubProcessNodeVisitor());
        this.nodesVisitors.put(Split.class, new SplitNodeVisitor());
        this.nodesVisitors.put(Join.class, new JoinNodeVisitor());
        this.nodesVisitors.put(FaultNode.class, new FaultNodeVisitor());
        this.nodesVisitors.put(RuleSetNode.class, new RuleSetNodeVisitor(contextClassLoader));
        this.nodesVisitors.put(BoundaryEventNode.class, new BoundaryEventNodeVisitor());
        this.nodesVisitors.put(EventNode.class, new EventNodeVisitor());
        this.nodesVisitors.put(ForEachNode.class, new ForEachNodeVisitor(nodesVisitors));
        this.nodesVisitors.put(CompositeContextNode.class, new CompositeContextNodeVisitor<>(nodesVisitors));
        this.nodesVisitors.put(EventSubProcessNode.class, new EventSubProcessNodeVisitor(nodesVisitors));
        this.nodesVisitors.put(TimerNode.class, new TimerNodeVisitor());
        this.nodesVisitors.put(MilestoneNode.class, new MilestoneNodeVisitor());
        this.nodesVisitors.put(DynamicNode.class, new DynamicNodeVisitor(nodesVisitors));
        this.nodesVisitors.put(StateNode.class, new StateNodeVisitor(nodesVisitors));
    }

    public void visitProcess(WorkflowProcess process, MethodDeclaration processMethod, ProcessMetaData metadata,
            String workflowType) {
        BlockStmt body = new BlockStmt();

        ClassOrInterfaceType processFactoryType = new ClassOrInterfaceType(null,
                ExecutableProcessFactory.class.getSimpleName());

        // create local variable factory and assign new fluent process to it
        VariableDeclarationExpr factoryField = new VariableDeclarationExpr(processFactoryType, FACTORY_FIELD_NAME);
        MethodCallExpr assignFactoryMethod = new MethodCallExpr(new NameExpr(processFactoryType.getName().asString()),
                "createProcess");
        assignFactoryMethod.addArgument(new StringLiteralExpr(process.getId()))
                .addArgument(new StringLiteralExpr(workflowType))
                .addArgument(new BooleanLiteralExpr(ProcessToExecModelGenerator.isServerlessWorkflow(process)));
        body.addStatement(new AssignExpr(factoryField, assignFactoryMethod, AssignExpr.Operator.ASSIGN));

        // item definitions
        Set<String> visitedVariables = new HashSet<>();
        VariableScope variableScope = (VariableScope) ((io.automatiko.engine.workflow.base.core.Process) process)
                .getDefaultContext(VariableScope.VARIABLE_SCOPE);

        visitVariableScope(variableScope, body, visitedVariables);
        visitSubVariableScopes(process.getNodes(), body, visitedVariables);

        Collection<TagDefinition> tagDefinitions = ((io.automatiko.engine.workflow.process.core.WorkflowProcess) process)
                .getTagDefinitions();
        if (tagDefinitions != null) {

            for (TagDefinition tag : tagDefinitions) {
                if (tag instanceof FunctionTagDefinition) {
                    String expression = tag.getExpression();
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(expression);
                    if (matcher.find()) {
                        expression = matcher.group(1);
                    }
                    BlockStmt actionBody = new BlockStmt();
                    List<Variable> variables = variableScope.getVariables();
                    variables.stream().filter(v -> tag.getExpression().contains(v.getName()))
                            .map(ActionNodeVisitor::makeAssignmentFromMap)
                            .forEach(actionBody::addStatement);
                    actionBody.addStatement(new ReturnStmt(new NameExpr(expression)));

                    LambdaExpr lambda = new LambdaExpr(
                            NodeList.nodeList(new Parameter(new UnknownType(), "exp"),
                                    new Parameter(new UnknownType(), "variables")),
                            actionBody);

                    body.addStatement(
                            getFactoryMethod(FACTORY_FIELD_NAME, "tag", new StringLiteralExpr(tag.getId()),
                                    new StringLiteralExpr(tag.getExpression()), lambda));

                } else {
                    body.addStatement(
                            getFactoryMethod(FACTORY_FIELD_NAME, "tag", new StringLiteralExpr(tag.getId()),
                                    new StringLiteralExpr(tag.getExpression()), new NullLiteralExpr()));
                }
            }
        }

        metadata.setDynamic(((io.automatiko.engine.workflow.process.core.WorkflowProcess) process).isDynamic());
        // the process itself
        body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_NAME, new StringLiteralExpr(process.getName())))
                .addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_PACKAGE_NAME,
                        new StringLiteralExpr(process.getPackageName())))
                .addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_DYNAMIC,
                        new BooleanLiteralExpr(metadata.isDynamic())))
                .addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VERSION,
                        new StringLiteralExpr(getOrDefault(process.getVersion(), DEFAULT_VERSION))))
                .addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VISIBILITY, new StringLiteralExpr(
                        getOrDefault(process.getVisibility(), WorkflowProcess.PUBLIC_VISIBILITY))));

        visitMetaData(process.getMetaData(), body, FACTORY_FIELD_NAME);

        visitHeader(process, body);

        List<Node> processNodes = new ArrayList<>();
        for (io.automatiko.engine.api.definition.process.Node procNode : process.getNodes()) {
            processNodes.add((io.automatiko.engine.workflow.process.core.Node) procNode);
        }
        visitNodes(process, processNodes, body, variableScope, metadata);
        visitConnections(process.getNodes(), body);

        body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_VALIDATE));

        MethodCallExpr getProcessMethod = new MethodCallExpr(new NameExpr(FACTORY_FIELD_NAME), "getProcess");
        body.addStatement(new ReturnStmt(getProcessMethod));
        processMethod.setBody(body);
    }

    private void visitVariableScope(VariableScope variableScope, BlockStmt body, Set<String> visitedVariables) {
        if (variableScope != null && !variableScope.getVariables().isEmpty()) {
            for (Variable variable : variableScope.getVariables()) {

                if (!visitedVariables.add(variable.getName())) {
                    continue;
                }

                ClassOrInterfaceType variableType = new ClassOrInterfaceType(null,
                        ObjectDataType.class.getSimpleName());
                ObjectCreationExpr variableValue = new ObjectCreationExpr(null, variableType,
                        new NodeList<>(
                                new ClassExpr(new ClassOrInterfaceType(null,
                                        ClassUtils.parseClassname(variable.getType().getStringType()))),
                                new StringLiteralExpr(variable.getType().getStringType())));

                body.addStatement(getAssignedFactoryMethod(FACTORY_FIELD_NAME, VariableFactory.class,
                        "$var_" + variable.getSanitizedName(), "variable",
                        new Expression[] { new StringLiteralExpr(variable.getId()), new StringLiteralExpr(variable.getName()),
                                variableValue }));

                visitMetaData(variable.getMetaData(), body, "$var_" + variable.getSanitizedName());
                body.addStatement(getFactoryMethod("$var_" + variable.getSanitizedName(), METHOD_DONE));
            }
        }
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

    private void visitSubVariableScopes(io.automatiko.engine.api.definition.process.Node[] nodes, BlockStmt body,
            Set<String> visitedVariables) {
        for (io.automatiko.engine.api.definition.process.Node node : nodes) {
            if (node instanceof ContextContainer) {
                VariableScope variableScope = (VariableScope) ((ContextContainer) node)
                        .getDefaultContext(VariableScope.VARIABLE_SCOPE);
                if (variableScope != null) {
                    visitVariableScope(variableScope, body, visitedVariables);
                }
            }
            if (node instanceof NodeContainer) {
                visitSubVariableScopes(((NodeContainer) node).getNodes(), body, visitedVariables);
            }
        }
    }

    private void visitHeader(WorkflowProcess process, BlockStmt body) {
        Map<String, Object> metaData = getMetaData(process.getMetaData());
        Set<String> imports = ((io.automatiko.engine.workflow.base.core.Process) process).getImports();
        Map<String, String> globals = ((io.automatiko.engine.workflow.base.core.Process) process).getGlobals();
        if ((imports != null && !imports.isEmpty()) || (globals != null && globals.size() > 0) || !metaData.isEmpty()) {
            if (imports != null) {
                for (String s : imports) {
                    body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_IMPORTS, new StringLiteralExpr(s)));
                }
            }
            if (globals != null) {
                for (Map.Entry<String, String> global : globals.entrySet()) {
                    body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_GLOBAL,
                            new StringLiteralExpr(global.getKey()), new StringLiteralExpr(global.getValue())));
                }
            }
        }
    }

    private Map<String, Object> getMetaData(Map<String, Object> input) {
        Map<String, Object> metaData = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String name = entry.getKey();
            if (entry.getKey().startsWith("custom") && entry.getValue() instanceof String) {
                metaData.put(name, entry.getValue());
            }
        }
        return metaData;
    }

    private <U extends io.automatiko.engine.api.definition.process.Node> void visitNodes(WorkflowProcess process,
            List<U> nodes, BlockStmt body, VariableScope variableScope, ProcessMetaData metadata) {
        for (U node : nodes) {
            AbstractNodeVisitor<U> visitor = (AbstractNodeVisitor<U>) nodesVisitors.get(node.getClass());
            if (visitor == null) {
                throw new IllegalStateException("No visitor found for node " + node.getClass().getName());
            }
            visitor.visitNode(process, node, body, variableScope, metadata);
        }
    }

    private void visitConnections(io.automatiko.engine.api.definition.process.Node[] nodes, BlockStmt body) {

        List<Connection> connections = new ArrayList<>();
        for (io.automatiko.engine.api.definition.process.Node node : nodes) {
            for (List<Connection> connectionList : node.getIncomingConnections().values()) {
                connections.addAll(connectionList);
            }
        }
        for (Connection connection : connections) {
            visitConnection(connection, body);
        }
    }

    private void visitConnection(Connection connection, BlockStmt body) {
        // if the connection was generated by a link event, don't dump.
        if (isConnectionRepresentingLinkEvent(connection)) {
            return;
        }
        if (Boolean.TRUE.equals(connection.getMetaData().get("association"))) {
            body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_CONNECTION,
                    new LongLiteralExpr(connection.getFrom().getId()), new LongLiteralExpr(connection.getTo().getId()),
                    new StringLiteralExpr(getOrDefault((String) connection.getMetaData().get(UNIQUE_ID), "")),
                    new BooleanLiteralExpr(true)));
        } else {
            body.addStatement(getFactoryMethod(FACTORY_FIELD_NAME, METHOD_CONNECTION,
                    new LongLiteralExpr(connection.getFrom().getId()), new LongLiteralExpr(connection.getTo().getId()),
                    new StringLiteralExpr(getOrDefault((String) connection.getMetaData().get(UNIQUE_ID), ""))));
        }
    }

    private boolean isConnectionRepresentingLinkEvent(Connection connection) {
        return connection.getMetaData().get(LINK_NODE_HIDDEN) != null;
    }

}
