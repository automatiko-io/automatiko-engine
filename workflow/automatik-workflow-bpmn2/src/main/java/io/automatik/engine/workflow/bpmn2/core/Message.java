
package io.automatik.engine.workflow.bpmn2.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {

    private static final long serialVersionUID = 510l;

    private String id;
    private String type;
    private String name;

    private Map<String, Object> metaData = new HashMap<String, Object>();

    public Message(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCorrelation() {
        return (String) this.metaData.get("correlation");
    }

    public void setCorrelation(String correlation) {
        this.metaData.put("correlation", correlation);
    }

    public String getCorrelationExpression() {
        return (String) this.metaData.get("correlationExpression");
    }

    public void setCorrelationExpression(String correlationExpression) {
        this.metaData.put("correlationExpression", correlationExpression);
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String name, Object data) {
        this.metaData.put(name, data);
    }
}
