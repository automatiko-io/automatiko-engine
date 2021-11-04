package io.quarkus.funqy.knative.events;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * CloudEvent.
 * FOR TEST PURPOSE ONLY AS IT IS REFERENCED IN GENERATED CODE
 */
public interface CloudEvent<T> {

    String id();

    String specVersion();

    String source();

    String type();

    String subject();

    OffsetDateTime time();

    Map<String, String> extensions();

    String dataSchema();

    String dataContentType();

    T data();

}
