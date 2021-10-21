package io.automatiko.engine.quarkus.variable;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.automatiko.engine.api.workflow.VariableAugmentor;
import io.automatiko.engine.workflow.base.instance.context.variable.DefaultVariableInitializer;
import io.quarkus.arc.DefaultBean;

@Dependent
@DefaultBean
public class DefaultVariableInitializerImpl extends DefaultVariableInitializer {

    Set<VariableAugmentor> discoveredAugmentors = new HashSet<>();

    @Inject
    public DefaultVariableInitializerImpl(Instance<VariableAugmentor> discoveredAugmentors) {
        this.discoveredAugmentors = discoveredAugmentors.stream().collect(Collectors.toSet());
    }

    @Override
    public Set<VariableAugmentor> augmentors() {
        return discoveredAugmentors;
    }

}
