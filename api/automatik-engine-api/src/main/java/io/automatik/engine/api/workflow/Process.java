
package io.automatik.engine.api.workflow;

import io.automatik.engine.api.Model;
import io.automatik.engine.api.auth.AccessPolicy;

public interface Process<T> {

    ProcessInstance<T> createInstance(T workingMemory);

    ProcessInstance<T> createInstance(String businessKey, T workingMemory);

    ProcessInstances<T> instances();

    <S> void send(Signal<S> sig);

    T createModel();

    ProcessInstance<? extends Model> createInstance(Model m);

    ProcessInstance<? extends Model> createInstance(String businessKey, Model m);

    String id();

    String name();

    String version();

    void activate();

    void deactivate();

    AccessPolicy<? extends ProcessInstance<T>> accessPolicy();

    default String image() {
        return null;
    }
}
