package io.automatik.engine.decision.dmn;

import java.util.ArrayList;
import java.util.List;

import io.automatik.engine.api.decision.DecisionModel;
import io.automatik.engine.api.decision.DecisionModels;

public class DmnDecisionModels implements DecisionModels {

    private List<DmnDecisionModel> decisionModels = new ArrayList<>();

    public DmnDecisionModels(List<DmnDecisionModel> decisionModels) {
        if (decisionModels != null) {
            this.decisionModels.addAll(decisionModels);
        }
    }

    @Override
    public DecisionModel getDecisionModel(String namespace, String name) {
        return decisionModels.stream()
                .filter(dm -> dm.getDMNModel().getNamespace().equals(namespace) && dm.getDMNModel().getName().equals(name))
                .findAny()
                .orElse(null);
    }

}
