
package io.automatiko.engine.workflow.compiler.canonical;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.ParameterDefinition;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.WorkItemNode;

public class ServiceTaskDescriptor {

    private final ClassLoader contextClassLoader;
    private String interfaceName;
    private String operationName;
    private final String implementation;
    private final Map<String, String> parameters;
    private final WorkItemNode workItemNode;
    private final String mangledName;
    Class<?> cls;

    private OpenAPIMetaData openapi;

    private boolean isServerlessWorkflow;

    private List<Class<?>> opTypes = Collections.emptyList();

    ServiceTaskDescriptor(WorkflowProcess process, WorkItemNode workItemNode, ClassLoader contextClassLoader) {
        this.workItemNode = workItemNode;
        this.isServerlessWorkflow = process.getMetaData().containsKey("IsServerlessWorkflow");
        interfaceName = (String) workItemNode.getWork().getParameter("Interface");
        operationName = (String) workItemNode.getWork().getParameter("Operation");

        implementation = (String) workItemNode.getWork().getParameter("implementation");

        if (implementation.equalsIgnoreCase("##webservice")) {
            openapi = OpenAPIMetaData.of((String) workItemNode.getWork().getParameter("interfaceImplementationRef"));

            interfaceName = "io.automatiko.engine.app.rest." + StringUtils.capitalize(openapi.name());
            openapi.addOperation(operationName);

            opTypes = openapi.parameters(operationName);
        }

        this.contextClassLoader = contextClassLoader;

        NodeValidator.of("workItemNode", workItemNode.getName()).notEmpty("interfaceName", interfaceName)
                .notEmpty("operationName", operationName).validate();

        parameters = serviceTaskParameters();

        mangledName = mangledHandlerName(interfaceName, operationName, String.valueOf(workItemNode.getId()));
    }

    public String mangledName() {
        return mangledName;
    }

    public String implementation() {
        return implementation;
    }

    public OpenAPIMetaData openapi() {
        return openapi;
    }

    private Map<String, String> serviceTaskParameters() {
        String type = (String) workItemNode.getWork().getParameter("ParameterType");
        Map<String, String> parameters = null;
        if (type != null) {
            if (isDefaultParameterType(type)) {
                type = inferParameterType();
            }

            parameters = Collections.singletonMap("Parameter", type);
        } else {
            parameters = new LinkedHashMap<>();

            for (ParameterDefinition def : workItemNode.getWork().getParameterDefinitions()) {
                parameters.put(def.getName(), def.getType().getStringType());
            }
        }
        return parameters;
    }

    // assume 1 single arg as above
    private String inferParameterType() {
        loadClass();
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(operationName) && m.getParameterCount() == 1) {
                return m.getParameterTypes()[0].getCanonicalName();
            }
        }
        throw new IllegalArgumentException(MessageFormat.format(
                "Invalid work item \"{0}\": could not find a method called \"{1}\" in class \"{2}\"",
                workItemNode.getName(), operationName, interfaceName));
    }

    private void loadClass() {
        if (cls != null) {
            return;
        }
        try {
            cls = contextClassLoader.loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Invalid work item \"{0}\": class not found for interfaceName \"{1}\"",
                            workItemNode.getName(), interfaceName));
        }
    }

    private boolean isDefaultParameterType(String type) {
        return type.equals("java.lang.Object") || type.equals("Object");
    }

    private String mangledHandlerName(String interfaceName, String operationName, String nodeName) {
        return String.format("%s_%s_%s_Handler", interfaceName, operationName, nodeName);
    }

    public CompilationUnit generateHandlerClassForService() {
        CompilationUnit compilationUnit = new CompilationUnit("io.automatiko.engine.app.handlers");

        compilationUnit.getTypes().add(classDeclaration());

        return compilationUnit;
    }

    protected ClassOrInterfaceDeclaration classDeclaration() {
        String unqualifiedName = StaticJavaParser.parseName(mangledName).removeQualifier().asString();
        ClassOrInterfaceDeclaration cls = new ClassOrInterfaceDeclaration().setName(unqualifiedName)
                .setModifiers(Modifier.Keyword.PUBLIC).addImplementedType(WorkItemHandler.class.getCanonicalName());
        ClassOrInterfaceType serviceType = new ClassOrInterfaceType(null, interfaceName);
        FieldDeclaration serviceField = new FieldDeclaration()
                .addVariable(new VariableDeclarator(serviceType, "service"));
        cls.addMember(serviceField);

        // executeWorkItem method
        BlockStmt executeWorkItemBody = new BlockStmt();

        MethodCallExpr callService = new MethodCallExpr(new NameExpr("service"), operationName);

        int counter = 0;
        for (Map.Entry<String, String> paramEntry : parameters.entrySet()) {
            MethodCallExpr getParamMethod = new MethodCallExpr(new NameExpr("workItem"), "getParameter")
                    .addArgument(new StringLiteralExpr(paramEntry.getKey()));

            if (isServerlessWorkflow) {
                String paramType = opTypes.size() > counter ? opTypes.get(counter).getCanonicalName() : paramEntry.getValue();

                MethodCallExpr extractValueMethod = new MethodCallExpr(
                        new NameExpr("io.automatiko.engine.workflow.json.ValueExtractor"), "extract")
                                .addArgument(getParamMethod)
                                .addArgument(new NameExpr(paramType + ".class"));
                callService
                        .addArgument(
                                new CastExpr(new ClassOrInterfaceType(null, paramType), extractValueMethod));

            } else {
                callService
                        .addArgument(new CastExpr(new ClassOrInterfaceType(null, paramEntry.getValue()), getParamMethod));
            }
            counter++;
        }
        MethodCallExpr completeWorkItem = completeWorkItem(executeWorkItemBody, callService);

        executeWorkItemBody.addStatement(completeWorkItem);

        if (implementation.equalsIgnoreCase("##webservice")) {
            BlockStmt catchbody = new BlockStmt();
            catchbody.addStatement(new ThrowStmt(new ObjectCreationExpr(null,
                    new ClassOrInterfaceType(null, "io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError"),
                    NodeList.nodeList(
                            new MethodCallExpr(new NameExpr("String"), "valueOf").addArgument(new MethodCallExpr(
                                    new MethodCallExpr(new NameExpr("wex"), "getResponse"), "getStatus")),
                            new NameExpr("wex")))));
            CatchClause catchClause = new CatchClause(
                    new Parameter(new ClassOrInterfaceType(null, "javax.ws.rs.WebApplicationException"), "wex"),
                    catchbody);

            BlockStmt unavailablecatchbody = new BlockStmt();
            unavailablecatchbody.addStatement(new ThrowStmt(new ObjectCreationExpr(null,
                    new ClassOrInterfaceType(null, "io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError"),
                    NodeList.nodeList(
                            new StringLiteralExpr("503"),
                            new NameExpr("ex")))));
            CatchClause unavailablecatchClause = new CatchClause(
                    new Parameter(new ClassOrInterfaceType(null, "javax.ws.rs.ProcessingException"), "ex"),
                    unavailablecatchbody);

            TryStmt trystmt = new TryStmt(executeWorkItemBody, NodeList.nodeList(catchClause, unavailablecatchClause), null);

            executeWorkItemBody = new BlockStmt().addStatement(trystmt);
        }

        MethodDeclaration executeWorkItem = new MethodDeclaration().setModifiers(Modifier.Keyword.PUBLIC)
                .setType(void.class).setName("executeWorkItem").setBody(executeWorkItemBody)
                .addParameter(WorkItem.class.getCanonicalName(), "workItem")
                .addParameter(WorkItemManager.class.getCanonicalName(), "workItemManager");

        // abortWorkItem method
        BlockStmt abortWorkItemBody = new BlockStmt();
        MethodDeclaration abortWorkItem = new MethodDeclaration().setModifiers(Modifier.Keyword.PUBLIC)
                .setType(void.class).setName("abortWorkItem").setBody(abortWorkItemBody)
                .addParameter(WorkItem.class.getCanonicalName(), "workItem")
                .addParameter(WorkItemManager.class.getCanonicalName(), "workItemManager");

        // getName method
        MethodDeclaration getName = new MethodDeclaration().setModifiers(Modifier.Keyword.PUBLIC).setType(String.class)
                .setName("getName")
                .setBody(new BlockStmt().addStatement(new ReturnStmt(new StringLiteralExpr(mangledName))));
        cls.addMember(executeWorkItem).addMember(abortWorkItem).addMember(getName);

        return cls;
    }

    private MethodCallExpr completeWorkItem(BlockStmt executeWorkItemBody, MethodCallExpr callService) {
        Expression results = null;
        List<DataAssociation> outAssociations = workItemNode.getOutAssociations();
        if (outAssociations.isEmpty()) {
            executeWorkItemBody.addStatement(callService);
            results = new NullLiteralExpr();
        } else {
            VariableDeclarationExpr resultField = new VariableDeclarationExpr().addVariable(new VariableDeclarator(
                    new ClassOrInterfaceType(null, Object.class.getCanonicalName()), "result", callService));

            executeWorkItemBody.addStatement(resultField);

            results = new MethodCallExpr(new NameExpr("java.util.Collections"), "singletonMap")
                    .addArgument(new StringLiteralExpr(outAssociations.get(0).getSources().get(0)))
                    .addArgument(new NameExpr("result"));
        }

        MethodCallExpr completeWorkItem = new MethodCallExpr(new NameExpr("workItemManager"), "completeWorkItem")
                .addArgument(new MethodCallExpr(new NameExpr("workItem"), "getId")).addArgument(results);
        return completeWorkItem;
    }

}
