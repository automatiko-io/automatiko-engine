package io.automatiko.engine.quarkus.ui;

import javax.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ui.FormProvider;
import io.quarkus.arc.DefaultBean;

@DefaultBean
@ApplicationScoped
public class DefaultFormProvider implements FormProvider {

    private static final String DEFAULT_NOT_FOUND = "<html><head><title>Form not found</title></head><body><h3><center>Form not found</center></h3></body></html>";

    @Override
    public String form(Process<?> process) {
        return DEFAULT_NOT_FOUND;
    }

}
