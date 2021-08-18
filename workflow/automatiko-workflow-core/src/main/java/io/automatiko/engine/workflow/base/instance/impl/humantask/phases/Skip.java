
package io.automatiko.engine.workflow.base.instance.impl.humantask.phases;

import java.util.Arrays;
import java.util.List;

import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.runtime.process.HumanTaskWorkItem;
import io.automatiko.engine.api.runtime.process.WorkItem;
import io.automatiko.engine.api.workflow.workitem.LifeCyclePhase;
import io.automatiko.engine.api.workflow.workitem.Policy;
import io.automatiko.engine.api.workflow.workitem.Transition;
import io.automatiko.engine.workflow.base.instance.impl.humantask.HumanTaskWorkItemImpl;
import io.automatiko.engine.workflow.base.instance.impl.workitem.Active;

/**
 * Skip life cycle phase that applies to human tasks. It will set the status to
 * "Skipped"
 *
 * It can transition from
 * <ul>
 * <li>Active</li>
 * <li>Claim</li>
 * <li>Release</li>
 * </ul>
 * 
 * This is a terminating (final) phase.
 */
public class Skip implements LifeCyclePhase {

    public static final String ID = "skip";
    public static final String STATUS = "Skipped";

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
    public boolean isCompleting() {
        return true;
    }

    @Override
    public boolean canTransition(LifeCyclePhase phase) {
        return allowedTransitions.contains(phase.id());
    }

    @Override
    public void apply(WorkItem workitem, Transition<?> transition) {

        if (transition.policies() != null) {
            for (Policy<?> policy : transition.policies()) {
                if (policy instanceof SecurityPolicy) {
                    ((HumanTaskWorkItemImpl) workitem).setActualOwner(((SecurityPolicy) policy).value().getName());
                    break;
                }
            }
        }
        workitem.getResults().put("ActorId", ((HumanTaskWorkItem) workitem).getActualOwner());
    }
}
