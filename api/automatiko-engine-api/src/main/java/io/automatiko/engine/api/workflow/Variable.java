package io.automatiko.engine.api.workflow;

import java.util.Map;

import io.automatiko.engine.api.workflow.datatype.DataType;

public interface Variable {

    String getId();

    String getName();

    DataType getType();

    Object getValue();

    Object getMetaData(String name);

    Map<String, Object> getMetaData();
}
