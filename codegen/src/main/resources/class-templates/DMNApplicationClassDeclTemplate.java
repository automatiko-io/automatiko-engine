
public class DecisionModels implements org.kie.kogito.decision.DecisionModels {

    private final static org.kie.dmn.api.core.DMNRuntime dmnRuntime = org.kie.kogito.dmn.DMNKogito.createGenericDMNRuntime();
    

    public void init(io.automatik.engine.api.Application app) {
        app.config().decision().decisionEventListeners().listeners().forEach(dmnRuntime::addListener);
    }

    public io.automatik.engine.api.decision.DecisionModel getDecisionModel(java.lang.String namespace, java.lang.String name) {
        return new io.automatik.engine.decision.dmn.DmnDecisionModel(dmnRuntime, namespace, name, execIdSupplier);
    }

}
