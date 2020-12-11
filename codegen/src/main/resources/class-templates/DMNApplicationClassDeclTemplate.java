

public class DecisionModels implements io.automatik.engine.api.decision.DecisionModels {

    private final static org.kie.dmn.api.core.DMNRuntime dmnRuntime = io.automatik.engine.decision.dmn.DmnRuntimeProvider.from();
    

    public void init(io.automatik.engine.api.Application app) {
        app.config().decision().decisionEventListeners().listeners().forEach(l -> dmnRuntime.addListener((org.kie.dmn.api.core.event.DMNRuntimeEventListener)l));
    }

    public io.automatik.engine.api.decision.DecisionModel getDecisionModel(java.lang.String namespace, java.lang.String name) {
        return new io.automatik.engine.decision.dmn.DmnDecisionModel(dmnRuntime, namespace, name, null);
    }

}
