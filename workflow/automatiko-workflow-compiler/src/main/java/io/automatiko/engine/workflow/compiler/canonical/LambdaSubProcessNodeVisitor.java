
package io.automatiko.engine.workflow.compiler.canonical;

import static com.github.javaparser.StaticJavaParser.parse;
import static io.automatiko.engine.workflow.compiler.util.ClassUtils.constructClass;
import static io.automatiko.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_INDEPENDENT;
import static io.automatiko.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_PROCESS_ID;
import static io.automatiko.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_PROCESS_NAME;
import static io.automatiko.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_PROCESS_VERSION;
import static io.automatiko.engine.workflow.process.executable.core.factory.SubProcessNodeFactory.METHOD_WAIT_FOR_COMPLETION;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.context.variable.Variable;
import io.automatiko.engine.workflow.base.core.context.variable.VariableScope;
import io.automatiko.engine.workflow.base.core.datatype.impl.type.ObjectDataType;
import io.automatiko.engine.workflow.base.instance.impl.util.VariableUtil;
import io.automatiko.engine.workflow.process.core.node.SubProcessNode;
import io.automatiko.engine.workflow.process.executable.core.factory.SubProcessNodeFactory;
import io.automatiko.engine.workflow.util.PatternConstants;

public class LambdaSubProcessNodeVisitor extends AbstractNodeVisitor<SubProcessNode> {

    @Override
    protected String getNodeKey() {
        return "subProcessNode";
    }

    @Override
    public void visitNode(WorkflowProcess process, String factoryField, SubProcessNode node, BlockStmt body,
            VariableScope variableScope,
            ProcessMetaData metadata) {
        InputStream resourceAsStream = this.getClass()
                .getResourceAsStream("/class-templates/SubProcessFactoryTemplate.java");
        Optional<Expression> retValue = parse(resourceAsStream).findFirst(Expression.class);
        String name = node.getName();
        String subProcessId = node.getProcessId();
        String subProcessVersion = ModelMetaData.version(node.getProcessVersion());

        NodeValidator.of(getNodeKey(), name).notEmpty("subProcessId", subProcessId).validate();

        body.addStatement(getAssignedFactoryMethod(factoryField, SubProcessNodeFactory.class, getNodeId(node),
                getNodeKey(), new LongLiteralExpr(node.getId()))).addStatement(getNameMethod(node, "Call Activity"))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_PROCESS_ID, new StringLiteralExpr(subProcessId)))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_PROCESS_NAME,
                        new StringLiteralExpr(getOrDefault(node.getProcessName(), ""))))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_PROCESS_VERSION,
                        new StringLiteralExpr(getOrDefault(node.getProcessVersion(), ""))))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_WAIT_FOR_COMPLETION,
                        new BooleanLiteralExpr(node.isWaitForCompletion())))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_INDEPENDENT,
                        new BooleanLiteralExpr(node.isIndependent())));

        Map<String, String> inputTypes = (Map<String, String>) node.getMetaData("BPMN.InputTypes");

        String subProcessModelClassName = ProcessToExecModelGenerator.extractModelClassName(subProcessId,
                subProcessVersion);
        ModelMetaData subProcessModel = new ModelMetaData(process.getType(), subProcessId, subProcessVersion,
                metadata.getPackageName(),
                subProcessModelClassName, WorkflowProcess.PRIVATE_VISIBILITY,
                VariableDeclarations.ofRawInfo(inputTypes), false, "Data model for " + name,
                "A complete data model for " + name);

        retValue.ifPresent(retValueExpression -> {
            retValueExpression.findAll(ClassOrInterfaceType.class).stream()
                    .filter(t -> t.getNameAsString().equals("$Type$"))
                    .forEach(t -> t.setName(subProcessModelClassName));

            retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("bind"))
                    .ifPresent(m -> m.setBody(bind(variableScope, node, subProcessModel, process)));
            retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("createInstance"))
                    .ifPresent(m -> m.setBody(createInstance(node, metadata)));
            retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("unbind"))
                    .ifPresent(m -> m.setBody(unbind(variableScope, node, process)));
            retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("abortInstance"))
                    .ifPresent(m -> m.setBody(abortInstance(node, metadata)));
            retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("findInstance"))
                    .ifPresent(m -> m.setBody(findInstance(node, metadata, subProcessModelClassName)));
            retValueExpression.findFirst(MethodDeclaration.class, m -> m.getNameAsString().equals("findInstanceByStatus"))
                    .ifPresent(m -> m.setBody(findInstanceByStatus(node, metadata, subProcessModelClassName)));
        });

        if (retValue.isPresent()) {
            body.addStatement(getFactoryMethod(getNodeId(node), getNodeKey(), retValue.get()));
        } else {
            body.addStatement(getFactoryMethod(getNodeId(node), getNodeKey()));
        }

        visitMetaData(node.getMetaData(), body, getNodeId(node));
        body.addStatement(getDoneMethod(getNodeId(node)));
    }

    private BlockStmt bind(VariableScope variableScope, SubProcessNode subProcessNode, ModelMetaData subProcessModel,
            WorkflowProcess process) {
        BlockStmt actionBody = new BlockStmt();
        actionBody.addStatement(subProcessModel.newInstance("model"));

        // checks if this (parent) process is serverless
        boolean isServerless = ProcessToExecModelGenerator.isServerlessWorkflow(process);
        // checks if the process to be called (child) is serverless
        boolean isServerlessSP = (boolean) subProcessNode.getMetaData().getOrDefault("serverless", false);

        if (isServerlessSP) {

            if (isServerless) {
                for (Map.Entry<String, String> e : subProcessNode.getInMappings().entrySet()) {
                    // check if given mapping is an expression
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
                    if (matcher.find()) {

                        String expression = matcher.group(1);
                        String topLevelVariable = expression.split("\\.")[0];
                        Variable v = variableScope.findVariable(topLevelVariable);

                        if (v != null && actionBody.findFirst(VariableDeclarationExpr.class,
                                vd -> vd.getVariable(0).getNameAsString().equals(v.getSanitizedName())).isEmpty()) {
                            actionBody.addStatement(makeAssignment(v));
                        } else {
                            expression = expression.replace("\\\"", "\"");
                        }
                        actionBody.addStatement(
                                subProcessModel.callSetter("model", e.getKey(), dotNotationToGetExpression(expression)));
                    } else {

                        Variable v = variableScope.findVariable(e.getValue());
                        if (v != null) {
                            if (actionBody.findFirst(VariableDeclarationExpr.class,
                                    vd -> vd.getVariable(0).getNameAsString().equals(v.getSanitizedName())).isEmpty()) {
                                actionBody.addStatement(makeAssignment(v));
                            }
                            actionBody.addStatement(subProcessModel.callSetter("model", e.getKey(), e.getValue()));
                        }
                    }
                }
            } else {

                for (Map.Entry<String, String> e : subProcessNode.getInMappings().entrySet()) {
                    // check if given mapping is an expression
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
                    if (matcher.find()) {

                        String expression = matcher.group(1);
                        String topLevelVariable = expression.split("\\.")[0];
                        Variable v = variableScope.findVariable(topLevelVariable);

                        if (v != null && actionBody.findFirst(VariableDeclarationExpr.class,
                                vd -> vd.getVariable(0).getNameAsString().equals(v.getSanitizedName())).isEmpty()) {
                            actionBody.addStatement(makeAssignment(v));
                        } else {
                            expression = expression.replace("\\\"", "\"");
                        }
                        actionBody.addStatement(new MethodCallExpr(new NameExpr("model"), "addWorkflowdata")
                                .addArgument(new StringLiteralExpr(e.getKey()))
                                .addArgument(new MethodCallExpr(null, "toJsonNode")
                                        .addArgument(dotNotationToGetExpression(expression))));

                    } else {

                        Variable v = variableScope.findVariable(e.getValue());
                        if (v != null) {
                            if (actionBody.findFirst(VariableDeclarationExpr.class,
                                    vd -> vd.getVariable(0).getNameAsString().equals(v.getSanitizedName())).isEmpty()) {
                                actionBody.addStatement(makeAssignment(v));
                            }

                            actionBody.addStatement(new MethodCallExpr(new NameExpr("model"), "addWorkflowdata")
                                    .addArgument(new StringLiteralExpr(e.getKey()))
                                    .addArgument(new MethodCallExpr(null, "toJsonNode")
                                            .addArgument(new NameExpr(e.getValue()))));
                        }
                    }
                }
            }
        } else {
            if (isServerless) {

                actionBody.addStatement(new MethodCallExpr(null, "convertData")
                        .addArgument(new MethodCallExpr(new NameExpr(KCONTEXT_VAR), "getVariables"))
                        .addArgument(new NameExpr("model")));
            } else {
                for (Map.Entry<String, String> e : subProcessNode.getInMappings().entrySet()) {
                    // check if given mapping is an expression
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
                    if (matcher.find()) {

                        String expression = matcher.group(1);
                        String topLevelVariable = expression.split("\\.")[0];
                        Variable v = variableScope.findVariable(topLevelVariable);

                        if (v != null && actionBody.findFirst(VariableDeclarationExpr.class,
                                vd -> vd.getVariable(0).getNameAsString().equals(v.getSanitizedName())).isEmpty()) {
                            actionBody.addStatement(makeAssignment(v));
                        } else {
                            expression = expression.replace("\\\"", "\"");
                        }
                        actionBody.addStatement(
                                subProcessModel.callSetter("model", e.getKey(), dotNotationToGetExpression(expression)));
                    } else {

                        Variable v = variableScope.findVariable(e.getValue());
                        if (v != null) {
                            if (actionBody.findFirst(VariableDeclarationExpr.class,
                                    vd -> vd.getVariable(0).getNameAsString().equals(v.getSanitizedName())).isEmpty()) {
                                actionBody.addStatement(makeAssignment(v));
                            }
                            actionBody.addStatement(subProcessModel.callSetter("model", e.getKey(), e.getValue()));
                        }
                    }
                }
            }
        }

        actionBody.addStatement(new ReturnStmt(new NameExpr("model")));
        return actionBody;
    }

    private BlockStmt createInstance(SubProcessNode subProcessNode, ProcessMetaData metadata) {

        String processId = ProcessToExecModelGenerator.extractProcessId(subProcessNode.getProcessId(),
                ModelMetaData.version(subProcessNode.getProcessVersion()));
        String processFielName = "process" + processId;

        MethodCallExpr processInstanceSupplier = new MethodCallExpr(new NameExpr(processFielName), "createInstance")
                .addArgument("model");

        metadata.addSubProcess(processId,
                subProcessNode.getProcessId() + ModelMetaData.version(subProcessNode.getProcessVersion()));

        return new BlockStmt().addStatement(new ReturnStmt(processInstanceSupplier));
    }

    private BlockStmt abortInstance(SubProcessNode subProcessNode, ProcessMetaData metadata) {

        String processId = ProcessToExecModelGenerator.extractProcessId(subProcessNode.getProcessId(),
                ModelMetaData.version(subProcessNode.getProcessVersion()));
        String processFielName = "process" + processId;

        MethodCallExpr processInstanceSupplier = new MethodCallExpr("internalAbortInstance")
                .addArgument(new MethodCallExpr(new MethodCallExpr(new NameExpr(processFielName), "instances"), "findById")
                        .addArgument(new NameExpr("instanceId")));

        return new BlockStmt().addStatement(processInstanceSupplier);
    }

    private BlockStmt findInstance(SubProcessNode subProcessNode, ProcessMetaData metadata, String subProcessModelClassName) {

        String processId = ProcessToExecModelGenerator.extractProcessId(subProcessNode.getProcessId(),
                ModelMetaData.version(subProcessNode.getProcessVersion()));
        String processFielName = "process" + processId;

        MethodCallExpr findInstanceMethod = new MethodCallExpr(new MethodCallExpr(new NameExpr(processFielName), "instances"),
                "findById")
                        .addArgument(new NameExpr("instanceId"));

        CastExpr castExpr = new CastExpr(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null,
                        new SimpleName(io.automatiko.engine.api.workflow.ProcessInstance.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, subProcessModelClassName))))),
                findInstanceMethod);
        ReturnStmt processInstanceSupplier = new ReturnStmt(castExpr);
        return new BlockStmt().addStatement(processInstanceSupplier);
    }

    private BlockStmt findInstanceByStatus(SubProcessNode subProcessNode, ProcessMetaData metadata,
            String subProcessModelClassName) {

        String processId = ProcessToExecModelGenerator.extractProcessId(subProcessNode.getProcessId(),
                ModelMetaData.version(subProcessNode.getProcessVersion()));
        String processFielName = "process" + processId;

        MethodCallExpr findInstanceMethod = new MethodCallExpr(new MethodCallExpr(new NameExpr(processFielName), "instances"),
                "findById")
                        .addArgument(new NameExpr("instanceId"))
                        .addArgument(new NameExpr("status"))
                        .addArgument(new NameExpr("io.automatiko.engine.api.workflow.ProcessInstanceReadMode.MUTABLE"));

        CastExpr castExpr = new CastExpr(new ClassOrInterfaceType(null, new SimpleName(Optional.class.getCanonicalName()),
                NodeList.nodeList(new ClassOrInterfaceType(null,
                        new SimpleName(io.automatiko.engine.api.workflow.ProcessInstance.class.getCanonicalName()),
                        NodeList.nodeList(new ClassOrInterfaceType(null, subProcessModelClassName))))),
                findInstanceMethod);
        ReturnStmt processInstanceSupplier = new ReturnStmt(castExpr);
        return new BlockStmt().addStatement(processInstanceSupplier);

    }

    private BlockStmt unbind(VariableScope variableScope, SubProcessNode subProcessNode, WorkflowProcess process) {
        BlockStmt stmts = new BlockStmt();
        // checks if this (parent) process is serverless
        boolean isServerless = ProcessToExecModelGenerator.isServerlessWorkflow(process);
        // checks if the process to be called (child) is serverless
        boolean isServerlessSP = (boolean) subProcessNode.getMetaData().getOrDefault("serverless", false);

        if (isServerlessSP) {
            if (isServerless) {
                for (Map.Entry<String, String> e : subProcessNode.getOutMappings().entrySet()) {

                    // check if given mapping is an expression
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
                    if (matcher.find()) {

                        String expression = matcher.group(1);
                        String topLevelVariable = VariableUtil.nameFromDotNotation(expression);
                        Map<String, String> dataOutputs = (Map<String, String>) subProcessNode.getMetaData("BPMN.OutputTypes");
                        Variable variable = new Variable();
                        variable.setName(topLevelVariable);
                        variable.setType(
                                new ObjectDataType(constructClass(dataOutputs.get(e.getKey())), dataOutputs.get(e.getKey())));

                        stmts.addStatement(makeAssignment(variableScope.findVariable(topLevelVariable)));
                        stmts.addStatement(makeAssignmentFromModel(variable, e.getKey()));

                        stmts.addStatement(VariableUtil.transformDotNotation(expression, e.getKey()) + ";");

                        stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
                                .addArgument(new StringLiteralExpr(topLevelVariable)).addArgument(topLevelVariable));
                    } else {

                        stmts.addStatement(makeAssignmentFromModel(variableScope.findVariable(e.getValue()), e.getKey()));
                        stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
                                .addArgument(new StringLiteralExpr(e.getValue())).addArgument(e.getKey()));
                    }

                }
            } else {
                for (Map.Entry<String, String> e : subProcessNode.getOutMappings().entrySet()) {

                    // check if given mapping is an expression
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
                    if (matcher.find()) {

                        String expression = matcher.group(1);
                        String topLevelVariable = VariableUtil.nameFromDotNotation(expression);
                        Map<String, String> dataOutputs = (Map<String, String>) subProcessNode.getMetaData("BPMN.OutputTypes");
                        Variable variable = new Variable();
                        variable.setName(topLevelVariable);
                        variable.setType(
                                new ObjectDataType(constructClass(dataOutputs.get(e.getKey())), dataOutputs.get(e.getKey())));

                        stmts.addStatement(makeAssignment(variableScope.findVariable(topLevelVariable)));
                        stmts.addStatement(makeAssignmentFromJsonModel(variable, e.getKey()));

                        stmts.addStatement(VariableUtil.transformDotNotation(expression, e.getKey()) + ";");

                        stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
                                .addArgument(new StringLiteralExpr(topLevelVariable)).addArgument(topLevelVariable));
                    } else {

                        stmts.addStatement(makeAssignmentFromJsonModel(variableScope.findVariable(e.getValue()), e.getKey()));
                        stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
                                .addArgument(new StringLiteralExpr(e.getValue())).addArgument(e.getKey()));
                    }

                }

            }

        } else {

            if (isServerless) {
                stmts.addStatement(new MethodCallExpr(null, "copyData")
                        .addArgument(new MethodCallExpr(new NameExpr("model"), "toMap"))
                        .addArgument(new NameExpr(KCONTEXT_VAR)));
            } else {
                for (Map.Entry<String, String> e : subProcessNode.getOutMappings().entrySet()) {

                    // check if given mapping is an expression
                    Matcher matcher = PatternConstants.PARAMETER_MATCHER.matcher(e.getValue());
                    if (matcher.find()) {

                        String expression = matcher.group(1);
                        String topLevelVariable = VariableUtil.nameFromDotNotation(expression);
                        Map<String, String> dataOutputs = (Map<String, String>) subProcessNode.getMetaData("BPMN.OutputTypes");
                        Variable variable = new Variable();
                        variable.setName(topLevelVariable);
                        variable.setType(
                                new ObjectDataType(constructClass(dataOutputs.get(e.getKey())), dataOutputs.get(e.getKey())));

                        stmts.addStatement(makeAssignment(variableScope.findVariable(topLevelVariable)));
                        stmts.addStatement(makeAssignmentFromModel(variable, e.getKey()));

                        stmts.addStatement(VariableUtil.transformDotNotation(expression, e.getKey()) + ";");

                        stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
                                .addArgument(new StringLiteralExpr(topLevelVariable)).addArgument(topLevelVariable));
                    } else {

                        stmts.addStatement(makeAssignmentFromModel(variableScope.findVariable(e.getValue()), e.getKey()));
                        stmts.addStatement(new MethodCallExpr().setScope(new NameExpr(KCONTEXT_VAR)).setName("setVariable")
                                .addArgument(new StringLiteralExpr(e.getValue())).addArgument(e.getKey()));
                    }

                }
            }
        }

        return stmts;
    }

    protected Expression dotNotationToSetExpression(String dotNotation, String value) {
        String defaultValue = null;
        if (dotNotation.contains(":")) {

            String[] items = dotNotation.split(":");
            dotNotation = items[0];
            defaultValue = items[1];
        }
        String[] elements = dotNotation.split("\\.");
        Expression scope = new NameExpr(elements[0]);
        if (elements.length == 1) {
            return new AssignExpr(scope, new NameExpr(value), AssignExpr.Operator.ASSIGN);
        }
        for (int i = 1; i < elements.length - 1; i++) {
            scope = new MethodCallExpr().setScope(scope).setName("get" + StringUtils.capitalize(elements[i]));
        }
        Expression argument;
        if (defaultValue != null) {
            argument = new ConditionalExpr(
                    new BinaryExpr(new NameExpr(value), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
                    new NameExpr(value),
                    new StringLiteralExpr(defaultValue));
        } else {
            argument = new NameExpr(value);
        }
        return new MethodCallExpr().setScope(scope)
                .setName("set" + StringUtils.capitalize(elements[elements.length - 1])).addArgument(argument);
    }

    protected Expression dotNotationToGetExpression(String dotNotation) {
        if (dotNotation.matches(".*\\(.*\\).*")) {
            return new NameExpr(dotNotation);
        }

        String defaultValue = null;
        if (dotNotation.contains(":")) {

            String[] items = dotNotation.split(":");
            dotNotation = items[0];
            defaultValue = items[1];
        }

        String[] elements = dotNotation.split("\\.");
        Expression scope = new NameExpr(elements[0]);

        if (elements.length == 1) {
            return scope;
        }

        for (int i = 1; i < elements.length; i++) {
            scope = new MethodCallExpr().setScope(scope).setName("get" + StringUtils.capitalize(elements[i]));
        }
        if (defaultValue != null) {
            return new MethodCallExpr(
                    new MethodCallExpr(new NameExpr(Optional.class.getCanonicalName()), "ofNullable").addArgument(scope),
                    "orElse").addArgument(new StringLiteralExpr(defaultValue));
        }
        return scope;
    }
}
