package io.quarkus.funqy.knative.events;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * THIS IS JUST FOR THE TESTS, ACTUAL IMPL IS IN QUARKUS
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CloudEventOutputBuilder {

    public static class CloudEventOutput<T> {
        private Type javaType;
        private String type;
        private String source;
        private T data;

        protected CloudEventOutput() {

        }

        /**
         * If returns null then javaType should be inferred from data or some other mechanism
         * 
         * @return
         */
        public Type javaType() {
            return javaType;
        }

        public String type() {
            return type;
        }

        public String source() {
            return source;
        }

        public T data() {
            return data;
        }

    }

    public static class GenericType<T> {
        protected GenericType() {

        }
    }

    private CloudEventOutput event = new CloudEventOutput();

    public CloudEventOutputBuilder type(String type) {
        event.type = type;
        return this;
    }

    public CloudEventOutputBuilder source(String source) {
        event.source = source;
        return this;
    }

    public CloudEventOutputBuilder javaType(Type javaType) {
        event.javaType = javaType;
        return this;
    }

    public CloudEventOutputBuilder javaType(GenericType generic) {
        ParameterizedType pt = (ParameterizedType) generic.getClass().getGenericSuperclass();
        event.javaType = pt.getActualTypeArguments()[0];
        return this;
    }

    public <T> CloudEventOutput<T> build(T data) {
        event.data = data;
        return event;
    }
}
