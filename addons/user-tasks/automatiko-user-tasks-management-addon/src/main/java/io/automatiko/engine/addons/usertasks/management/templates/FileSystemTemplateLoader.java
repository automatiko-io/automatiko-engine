package io.automatiko.engine.addons.usertasks.management.templates;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class FileSystemTemplateLoader implements TemplateLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemTemplateLoader.class);

    private String templateFolder;

    private Engine engine;

    @Inject
    public FileSystemTemplateLoader(
            @ConfigProperty(name = "quarkus.automatiko.templates-folder") Optional<String> templateFolder, Engine engine) {
        this.templateFolder = templateFolder.orElse(null);
        this.engine = engine;
    }

    public void onStart(@Observes @Priority(Interceptor.Priority.APPLICATION) StartupEvent event) {
        load(engine);
    }

    public void load(Engine engine) {

        if (templateFolder == null) {
            return;
        }

        File[] templates = new File(templateFolder)
                .listFiles(f -> f.getName().toLowerCase().endsWith(".html") || f.getName().toLowerCase().endsWith(".txt")
                        || f.getName().toLowerCase().endsWith(".qute"));

        if (templates != null) {
            for (File template : templates) {
                try {
                    String templateContent = Files.readString(template.toPath());

                    Template parsed = engine.parse(templateContent);
                    String templateId = templateId(template);
                    engine.putTemplate(templateId, parsed);
                    LOGGER.info("Added template '{}' from external path {}", templateId, template.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.warn("Unable to load template from '{}' due to {}", template.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    private String templateId(File template) {
        String name = template.getName();
        // remove the extension .html, .txt, .qute
        return name.substring(0, name.lastIndexOf("."));
    }
}
