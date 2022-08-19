package io.automatiko.engine.addons.usertasks.management.templates;

import io.quarkus.qute.Engine;

public interface TemplateLoader {

    void load(Engine engine);
}
