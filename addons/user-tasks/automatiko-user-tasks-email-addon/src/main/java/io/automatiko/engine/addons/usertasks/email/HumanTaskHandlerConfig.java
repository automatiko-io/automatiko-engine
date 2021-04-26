package io.automatiko.engine.addons.usertasks.email;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.workflow.DefaultWorkItemHandlerConfig;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemHandler;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Engine;

@ApplicationScoped
public class HumanTaskHandlerConfig extends DefaultWorkItemHandlerConfig {

    @Inject
    public HumanTaskHandlerConfig(Mailer mailer, EmailAddressResolver emailAddressResolver, Engine engine,
            @ConfigProperty(name = "quarkus.automatiko.serviceUrl") Optional<String> serviceUrl) {
        register("Human Task",
                new HumanTaskWorkItemHandler(
                        new HumanTaskLifeCycleWithEmail(mailer, emailAddressResolver, engine, serviceUrl)));
    }

}
