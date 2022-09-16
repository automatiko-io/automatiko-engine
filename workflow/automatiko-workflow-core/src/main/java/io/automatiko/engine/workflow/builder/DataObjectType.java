package io.automatiko.engine.workflow.builder;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class DataObjectType<T> {
    protected final Type superType;
    protected final Type type;

    protected DataObjectType() {
        Type superClass = getClass().getGenericSuperclass();

        superType = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        type = ((ParameterizedType) superType).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }

    public Type getSuperType() {
        return superType;
    }

    public Type getSuperRawType() {
        return ((ParameterizedType) superType).getRawType();
    }
}