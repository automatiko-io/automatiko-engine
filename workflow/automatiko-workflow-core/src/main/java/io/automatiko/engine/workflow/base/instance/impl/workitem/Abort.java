package io.automatiko.engine.workflow.base.instance.impl.workitem;

import java.util.Arrays;
import java.util.List;

import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;
import io.automatiko.engine.workflow.base.instance.impl.humantask.phases.Claim;
import io.automatiko.engine.workflow.base.instance.impl.humantask.phases.Release;

public class Abort implements LifeCyclePhase {

    public static final String ID = "abort";
    public static final String STATUS = "Aborted";

    private List<String> allowedTransitions = Arrays.asList(Active.ID, Claim.ID, Release.ID);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String status() {
        return STATUS;
    }

    @Override
    public boolean isTerminating() {
        return false;
    }

    @Override
    public boolean canTransition(LifeCyclePhase phase) {
        return allowedTransitions.contains(phase.id());
    }

    @Override
    public boolean isCompleting() {
        return false;
    }

}
