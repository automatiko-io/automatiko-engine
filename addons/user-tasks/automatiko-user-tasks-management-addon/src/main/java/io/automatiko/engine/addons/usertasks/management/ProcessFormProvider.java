package io.automatiko.engine.addons.usertasks.management;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ui.FormProvider;
import io.automatiko.engine.workflow.AbstractProcess;
import io.quarkus.qute.Engine;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@ApplicationScoped
public class ProcessFormProvider implements FormProvider {

    private static final String DEFAULT_NOT_FOUND_TEMPLATE = "workflow-not-found";

    private static final String DEFAULT_NOT_AUTHORIZED_TEMPLATE = "workflow-not-authorized";

    private static final String DEFAULT_NOT_FOUND = "<html><head><title>Form not found</title></head><body><h3><center>Form not found</center></h3></body></html>";

    private static final String DEFAULT_NOT_AUTHORIZED = "<html><head><title>Not authorized</title></head><body><h3><center>You're not authorized to create new instances</center></h3></body></html>";

    private Engine engine;
    private String serviceUrl;

    @Inject
    public ProcessFormProvider(Engine engine,
            @ConfigProperty(name = "quarkus.automatiko.service-url") Optional<String> serviceUrl) {
        this.engine = engine;
        this.serviceUrl = serviceUrl.orElse(null);
    }

    @Override
    public String form(Process<?> process) {

        boolean canCreateInstance = process.accessPolicy().canCreateInstance(IdentityProvider.get());

        if (!canCreateInstance) {
            Template template = engine.getTemplate(DEFAULT_NOT_AUTHORIZED_TEMPLATE);
            if (template != null) {
                TemplateInstance templateInstance = template.data("id", process.id())
                        .data("name", process.name())
                        .data("description", process.description())
                        .data("version", process.version());

                return templateInstance.render();
            }
            return DEFAULT_NOT_AUTHORIZED;
        }

        Template template = engine.getTemplate(process.id());
        if (template == null) {

            String packageName = ((AbstractProcess<?>) process).process().getPackageName();
            if (packageName != null && !packageName.isEmpty()) {

                template = engine.getTemplate(packageName.replace(".", "/") + "/" + process.id());
            }

        }

        if (template != null) {
            String pathprefix = "";
            if (process.version() != null) {
                pathprefix = "v" + process.version().replaceAll("\\.", "_") + "/";
            }
            String link = (serviceUrl == null ? ""
                    : serviceUrl) + "/" + pathprefix + ((AbstractProcess<?>) process).process().getId();
            TemplateInstance templateInstance = template.data("id", process.id())
                    .data("name", process.name())
                    .data("description", process.description())
                    .data("version", process.version())
                    .data("url", new RawString(link));

            return templateInstance.render();
        }

        template = engine.getTemplate(DEFAULT_NOT_FOUND_TEMPLATE);
        if (template != null) {
            TemplateInstance templateInstance = template.data("id", process.id())
                    .data("name", process.name())
                    .data("description", process.description())
                    .data("version", process.version());

            return templateInstance.render();
        }
        return DEFAULT_NOT_FOUND;
    }

}
