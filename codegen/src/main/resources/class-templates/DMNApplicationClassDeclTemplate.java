

public class DecisionModels implements io.automatiko.engine.api.decision.DecisionModels {

    private final static org.kie.dmn.api.core.DMNRuntime dmnRuntime = io.automatiko.engine.decision.dmn.DmnRuntimeProvider.fromClassPath();
    

    public void init(io.automatiko.engine.api.Application app) {
        app.config().decision().decisionEventListeners().listeners().forEach(l -> dmnRuntime.addListener((org.kie.dmn.api.core.event.DMNRuntimeEventListener)l));
    }

    public io.automatiko.engine.api.decision.DecisionModel getDecisionModel(java.lang.String namespace, java.lang.String name) {
        return new io.automatiko.engine.decision.dmn.DmnDecisionModel(dmnRuntime, namespace, name, null);
    }
}
