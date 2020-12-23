
package io.automatiko.engine.workflow.bpmn2.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Error extends Signal implements Serializable {

    private static final long serialVersionUID = 510l;

    private String errorCode;

    private Map<String, Object> metaData = new HashMap<String, Object>();

    public Error(String id, String errorCode, String itemRef) {
        super(id, itemRef);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String name, Object data) {
        this.metaData.put(name, data);
    }
}
