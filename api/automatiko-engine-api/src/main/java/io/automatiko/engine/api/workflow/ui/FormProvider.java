package io.automatiko.engine.api.workflow.ui;

import io.automatiko.engine.api.workflow.Process;

public interface FormProvider {

    /**
     * Returns valid form (usually HTML) that can be used to interact to create new instances
     * of the process that is given
     * 
     * @param process process definition this form corresponds to
     * @return a form if found otherwise null
     */
    String form(Process<?> process);
}
