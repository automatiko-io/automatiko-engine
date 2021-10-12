
package io.automatiko.engine.workflow.compiler.canonical;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
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
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.automatiko.engine.api.definition.process.Node;
import io.automatiko.engine.api.definition.process.NodeContainer;
import io.automatiko.engine.api.definition.process.WorkflowProcess;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.runtime.process.WorkItemHandler;
import io.automatiko.engine.api.runtime.process.WorkItemManager;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionManager;
import io.automatiko.engine.services.utils.StringUtils;
import io.automatiko.engine.workflow.base.core.ParameterDefinition;
import io.automatiko.engine.workflow.base.core.event.EventFilter;
import io.automatiko.engine.workflow.base.core.event.EventTypeFilter;
import io.automatiko.engine.workflow.process.core.node.BoundaryEventNode;
import io.automatiko.engine.workflow.process.core.node.DataAssociation;
import io.automatiko.engine.workflow.process.core.node.EventSubProcessNode;
import io.automatiko.engine.workflow.process.core.node.EventTrigger;
import io.automatiko.engine.workflow.process.core.node.StartNode;
import io.automatiko.engine.workflow.process.core.node.Trigger;
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

            Map<String, String> params = extractParams();
            openapi.addOperation(operationName, params);

            opTypes = openapi.parameters(operationName);
        }

        this.contextClassLoader = contextClassLoader;

        NodeValidator.of("workItemNode", workItemNode.getName()).notEmpty("interfaceName", interfaceName)
                .notEmpty("operationName", operationName).validate();

        parameters = serviceTaskParameters();

        mangledName = mangledHandlerName(process.getId() + ModelMetaData.version(process.getVersion()), interfaceName,
                operationName, String.valueOf(workItemNode.getId()));
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

    public Object metadata(String name, Object defaultValue) {
        Object value = workItemNode.getMetaData(name);

        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    private Map<String, String> extractParams() {
        if (this.operationName.contains("?")) {
            String[] elements = operationName.split("\\?");

            this.operationName = elements[0];

            String paramString = elements[1];

            Map<String, String> params = new HashMap<>();

            String[] parameters = paramString.split("&");

            for (String param : parameters) {
                String[] pair = param.split("=");

                params.put(pair[0], pair[1]);
            }

            return params;
        }

        return Collections.emptyMap();
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

    private String mangledHandlerName(String processId, String interfaceName, String operationName, String nodeName) {
        return String.format("%s_%s_%s_%s_Handler", interfaceName, operationName, processId, nodeName);
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

        ClassOrInterfaceType completionHandlerType = new ClassOrInterfaceType(null,
                WorkItemExecutionManager.class.getCanonicalName());
        FieldDeclaration completionHandlerField = new FieldDeclaration()
                .addVariable(new VariableDeclarator(completionHandlerType, "completionHandler"));
        cls.addMember(completionHandlerField);

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

        MethodCallExpr completionHandlerCompleteMethod = completeWorkItemViaHandler();

        IfStmt handleCompletion = new IfStmt(
                new BinaryExpr(new NameExpr("completionHandler"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                new BlockStmt().addStatement(completeWorkItem), new BlockStmt().addStatement(completionHandlerCompleteMethod));

        executeWorkItemBody.addStatement(handleCompletion);

        if (implementation.equalsIgnoreCase("##webservice")) {
            BlockStmt catchbody = new BlockStmt();
            catchbody.addStatement(new ThrowStmt(new ObjectCreationExpr(null,
                    new ClassOrInterfaceType(null, "io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError"),
                    NodeList.nodeList(new MethodCallExpr(
                            new NameExpr("wex"), "getMessage"),
                            new MethodCallExpr(new NameExpr("String"), "valueOf").addArgument(new MethodCallExpr(
                                    new MethodCallExpr(new NameExpr("wex"), "getResponse"), "getStatus")),
                            new MethodCallExpr(new NameExpr("io.automatiko.engine.services.utils.IoUtils"), "valueOf")
                                    .addArgument(new MethodCallExpr(
                                            new MethodCallExpr(new NameExpr("wex"), "getResponse"), "getEntity"))))));
            CatchClause catchClause = new CatchClause(
                    new Parameter(new ClassOrInterfaceType(null, "javax.ws.rs.WebApplicationException"), "wex"),
                    catchbody);

            BlockStmt unavailablecatchbody = new BlockStmt();
            unavailablecatchbody.addStatement(new ThrowStmt(new ObjectCreationExpr(null,
                    new ClassOrInterfaceType(null, "io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError"),
                    NodeList.nodeList(
                            new StringLiteralExpr("503"),
                            new MethodCallExpr(
                                    new NameExpr("ex"), "getMessage"),
                            new NameExpr("ex")))));
            CatchClause unavailablecatchClause = new CatchClause(
                    new Parameter(new ClassOrInterfaceType(null, "javax.ws.rs.ProcessingException"), "ex"),
                    unavailablecatchbody);

            TryStmt trystmt = new TryStmt(executeWorkItemBody, NodeList.nodeList(catchClause, unavailablecatchClause), null);

            executeWorkItemBody = new BlockStmt().addStatement(trystmt);
        }
        Set<String> handledErrorCodes = collectHandledErrorCodes();
        if (!handledErrorCodes.isEmpty()) {
            // add exception wrapper to handle errors that have error handlers attached
            BlockStmt runtimeCatchBody = new BlockStmt();
            MethodCallExpr wrapMethodCall = new MethodCallExpr(new NameExpr("io.automatiko.engine.workflow.ErrorMapper"),
                    "wrap")
                            .addArgument(new NameExpr("rex"));

            for (String errorCode : handledErrorCodes) {
                wrapMethodCall.addArgument(new StringLiteralExpr(errorCode));
            }

            runtimeCatchBody.addStatement(
                    new ThrowStmt(wrapMethodCall));
            CatchClause runtimeCatchClause = new CatchClause(
                    new Parameter(new ClassOrInterfaceType(null, RuntimeException.class.getCanonicalName()), "rex"),
                    runtimeCatchBody);
            TryStmt wrapperTryCatch = new TryStmt(executeWorkItemBody, NodeList.nodeList(runtimeCatchClause), null);
            executeWorkItemBody = new BlockStmt().addStatement(wrapperTryCatch);
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

    private Set<String> collectHandledErrorCodes() {
        Set<String> errorCodes = new HashSet<>();
        NodeContainer container = workItemNode.getParentContainer();
        String thisNodeId = (String) workItemNode.getMetaData("UniqueId");
        for (Node node : container.getNodes()) {
            if (node instanceof BoundaryEventNode) {
                String errorCode = (String) node.getMetaData().get("ErrorEvent");
                boolean hasErrorCode = (Boolean) node.getMetaData().get("HasErrorEvent");

                if (hasErrorCode && ((BoundaryEventNode) node).getAttachedToNodeId().equals(thisNodeId)) {
                    errorCodes.add(errorCode);
                }
            }
        }

        // next collect event subprocess node with error start event from this level and to all parents
        String replaceRegExp = "Error-|Escalation-";
        for (Node node : container.getNodes()) {
            if (node instanceof EventSubProcessNode) {
                EventSubProcessNode eventSubProcessNode = (EventSubProcessNode) node;

                Node[] nodes = eventSubProcessNode.getNodes();
                for (Node subNode : nodes) {
                    // avoids cyclomatic complexity
                    if (subNode == null || !(subNode instanceof StartNode)) {
                        continue;
                    }
                    List<Trigger> triggers = ((StartNode) subNode).getTriggers();
                    if (triggers == null) {
                        continue;
                    }
                    for (Trigger trigger : triggers) {
                        if (trigger instanceof EventTrigger) {
                            final List<EventFilter> filters = ((EventTrigger) trigger).getEventFilters();

                            for (EventFilter filter : filters) {
                                if (filter instanceof EventTypeFilter) {
                                    eventSubProcessNode.addEvent((EventTypeFilter) filter);

                                    String type = ((EventTypeFilter) filter).getType();
                                    if (type.startsWith("Error-")) {
                                        String trimmedType = type.replaceFirst(replaceRegExp, "");
                                        for (String error : trimmedType.split(",")) {
                                            errorCodes.add(error);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return errorCodes;
    }

    private MethodCallExpr completeWorkItem(BlockStmt executeWorkItemBody, MethodCallExpr callService) {
        Expression results = null;
        List<DataAssociation> outAssociations = workItemNode.getOutAssociations();

        if (hasReturn()) {
            VariableDeclarationExpr resultField = new VariableDeclarationExpr().addVariable(new VariableDeclarator(
                    new ClassOrInterfaceType(null, Object.class.getCanonicalName()), "result", callService));

            executeWorkItemBody.addStatement(resultField);
            if (outAssociations.isEmpty()) {
                results = new NullLiteralExpr();
            } else {
                results = new MethodCallExpr(new NameExpr("java.util.Collections"), "singletonMap")
                        .addArgument(new StringLiteralExpr(outAssociations.get(0).getSources().get(0)))
                        .addArgument(new NameExpr("result"));
            }
        } else if (outAssociations.isEmpty()) {
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

    private MethodCallExpr completeWorkItemViaHandler() {
        ClassOrInterfaceType errorMapper = new ClassOrInterfaceType(null,
                implementation.equalsIgnoreCase("##webservice") ? "io.automatiko.engine.service.rest.WebErrorMapper"
                        : "io.automatiko.engine.workflow.ErrorMapper");

        Expression name = null;
        Expression source = null;
        List<DataAssociation> outAssociations = workItemNode.getOutAssociations();
        if (hasReturn()) {
            if (outAssociations.isEmpty()) {
                name = new NullLiteralExpr();
            } else {
                name = new StringLiteralExpr(outAssociations.get(0).getSources().get(0));
            }
            source = new NameExpr("result");
        } else if (outAssociations.isEmpty()) {
            name = new NullLiteralExpr();
            source = new NullLiteralExpr();
        } else {
            name = new StringLiteralExpr(outAssociations.get(0).getSources().get(0));
            source = new NameExpr("result");
        }

        MethodCallExpr complitionHandlerCompleteMethod = new MethodCallExpr(new NameExpr("completionHandler"), "complete")
                .addArgument(new MethodCallExpr(new NameExpr("workItem"), "getProcessId"))
                .addArgument(name)
                .addArgument(new NameExpr("workItem"))
                .addArgument(new NameExpr("workItemManager"))
                .addArgument(source)
                .addArgument(new ObjectCreationExpr(null, errorMapper, NodeList.nodeList()));
        return complitionHandlerCompleteMethod;
    }

    protected boolean hasReturn() {
        if (implementation.equalsIgnoreCase("##webservice")) {

            return openapi.hasParameter(operationName, "mode", "async");
        }

        loadClass();

        for (Method method : cls.getMethods()) {
            if (method.getName().equals(operationName)) {
                return method.getReturnType() != void.class;
            }
        }

        return false;
    }

}
