
package io.automatik.engine.workflow.compiler.canonical;

import static io.automatik.engine.workflow.compiler.canonical.AbstractVisitor.KCONTEXT_VAR;
import static io.automatik.engine.workflow.process.executable.core.Metadata.MAPPING_VARIABLE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.MESSAGE_TYPE;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_REF;
import static io.automatik.engine.workflow.process.executable.core.Metadata.TRIGGER_TYPE;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

import io.automatik.engine.api.definition.process.Node;
import io.automatik.engine.services.utils.StringUtils;

public class TriggerMetaData {

    public enum TriggerType {
        ConsumeMessage,
        ProduceMessage,
        Signal,
        Error,
        Condition
    }

    // name of the trigger derived from message or signal
    private String name;
    // type of the trigger e.g. message, signal, timer...
    private TriggerType type;
    // data type of the event associated with this trigger
    private String dataType;
    // reference in the model of the process the event should be mapped to
    private String modelRef;
    // reference to owner of the trigger usually node
    private String ownerId;
    // human readable description that can be used in instructions
    private String description;

    // optional correlation to be used when trigger has be executed
    private String correlation;
    private String correlationExpression;

    private boolean start = false;

    private Map<String, Object> context = new HashMap<String, Object>();

    public TriggerMetaData(String name, String type, String dataType, String modelRef, String ownerId, String description) {
        this(name, type, dataType, modelRef, ownerId, description, null, null);
    }

    public TriggerMetaData(String name, String type, String dataType, String modelRef, String ownerId, String description,
            String correlation, String correlationExpression) {
        this.name = name;
        this.type = TriggerType.valueOf(type);
        this.dataType = dataType;
        this.modelRef = modelRef;
        this.ownerId = ownerId;
        this.description = description;
        this.correlation = correlation;
        this.correlationExpression = correlationExpression;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TriggerType getType() {
        return type;
    }

    public void setType(TriggerType type) {
        this.type = type;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getModelRef() {
        return modelRef;
    }

    public void setModelRef(String modelRef) {
        this.modelRef = modelRef;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCorrelation() {
        return correlation;
    }

    public void setCorrelation(String correlation) {
        this.correlation = correlation;
    }

    public String getCorrelationExpression() {
        return correlationExpression;
    }

    public void setCorrelationExpression(String correlationExpression) {
        this.correlationExpression = correlationExpression;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public TriggerMetaData validate() {
        if (TriggerType.ConsumeMessage.equals(type) || TriggerType.ProduceMessage.equals(type)) {

            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(dataType) || StringUtils.isEmpty(modelRef)) {
                throw new IllegalArgumentException("Message Trigger information is not complete " + this);
            }
        } else if (TriggerType.Signal.equals(type) && StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Signal Trigger information is not complete " + this);
        }

        return this;
    }

    @Override
    public String toString() {
        return "TriggerMetaData [name=" + name + ", type=" + type + ", dataType=" + dataType + ", modelRef=" + modelRef
                + "]";
    }

    public static LambdaExpr buildLambdaExpr(Node node, ProcessMetaData metadata) {
        Map<String, Object> nodeMetaData = node.getMetaData();
        TriggerMetaData triggerMetaData = new TriggerMetaData((String) nodeMetaData.get(TRIGGER_REF),
                (String) nodeMetaData.get(TRIGGER_TYPE), (String) nodeMetaData.get(MESSAGE_TYPE),
                (String) nodeMetaData.get(MAPPING_VARIABLE), String.valueOf(node.getId()), node.getName()).validate();
        triggerMetaData.addContext(node.getMetaData());
        metadata.addTrigger(triggerMetaData);

        // and add trigger action
        BlockStmt actionBody = new BlockStmt();
        CastExpr variable = new CastExpr(new ClassOrInterfaceType(null, triggerMetaData.getDataType()),
                new MethodCallExpr(new NameExpr(KCONTEXT_VAR), "getVariable")
                        .addArgument(new StringLiteralExpr(triggerMetaData.getModelRef())));
        MethodCallExpr producerMethodCall = new MethodCallExpr(new NameExpr("producer_" + node.getId()), "produce")
                .addArgument(new MethodCallExpr(new NameExpr("kcontext"), "getProcessInstance")).addArgument(variable);
        actionBody.addStatement(producerMethodCall);
        return new LambdaExpr(new Parameter(new UnknownType(), KCONTEXT_VAR), // (kcontext) ->
                actionBody);
    }

    public Object getContext(String name) {
        return context.get(name);
    }

    public Object getContext(String name, String defaultValue) {
        Object value = getContext(name);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public void addContext(Map<String, Object> data) {
        this.context.putAll(data);
    }
}
