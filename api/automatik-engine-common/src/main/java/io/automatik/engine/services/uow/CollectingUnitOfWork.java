
package io.automatik.engine.services.uow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.automatik.engine.api.event.EventBatch;
import io.automatik.engine.api.event.EventManager;
import io.automatik.engine.api.uow.UnitOfWork;
import io.automatik.engine.api.uow.WorkUnit;
import io.automatik.engine.api.workflow.MutableProcessInstances;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.ProcessInstanceReadMode;
import io.automatik.engine.api.workflow.ProcessInstances;

/**
 * Simple unit of work that collects work elements throughout the life of the
 * unit and invokes all of them at the end when end method is invoked. It does
 * not invoke the work when abort is invoked, only clears the collected items.
 *
 */
public class CollectingUnitOfWork implements UnitOfWork {

    private Set<WorkUnit<?>> collectedWork;
    private boolean done;

    private final EventManager eventManager;

    private Map<String, ProcessInstances<?>> instances = new HashMap<String, ProcessInstances<?>>();

    public CollectingUnitOfWork(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public void start() {
        checkDone();
        if (collectedWork == null) {
            collectedWork = new LinkedHashSet<>();
        }
    }

    @Override
    public void end() {
        checkStarted();
        EventBatch batch = eventManager.newBatch();

        for (WorkUnit<?> work : sorted()) {
            batch.append(work.data());
            work.perform();
        }
        eventManager.publish(batch);
        done();
    }

    @Override
    public void abort() {
        checkStarted();
        for (WorkUnit<?> work : sorted()) {
            work.abort();
        }
        done();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void intercept(WorkUnit work) {
        checkStarted();
        if (work == null) {
            throw new NullPointerException("Work must be non null");
        }
        collectedWork.remove(work);
        collectedWork.add(work);
    }

    @Override
    public ProcessInstances<?> managedProcessInstances(Process<?> process, ProcessInstances<?> instances) {

        return this.instances.computeIfAbsent(process.id(),
                pid -> new ManagedProcessInstances((MutableProcessInstances<?>) instances));
    }

    protected Collection<WorkUnit<?>> sorted() {
        List<WorkUnit<?>> sortedCollectedWork = new ArrayList<>(collectedWork);
        sortedCollectedWork.sort((u1, u2) -> u1.priority().compareTo(u2.priority()));

        return sortedCollectedWork;
    }

    protected void checkDone() {
        if (done) {
            throw new IllegalStateException("Unit of work is already done (ended or aborted)");
        }
    }

    protected void checkStarted() {
        if (collectedWork == null) {
            throw new IllegalStateException("Unit of work is not started");
        }
    }

    protected void done() {
        done = true;
        collectedWork = null;
        instances.clear();
    }

    @SuppressWarnings("rawtypes")
    private class ManagedProcessInstances implements MutableProcessInstances {

        private MutableProcessInstances<?> delegate;

        private Map<String, ProcessInstance<?>> local = new HashMap<String, ProcessInstance<?>>();

        public ManagedProcessInstances(MutableProcessInstances<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<?> findById(String id, ProcessInstanceReadMode mode) {
            if (local.containsKey(id)) {
                return Optional.of(local.get(id));
            }

            Optional<?> found = delegate.findById(id, mode);

            if (found.isPresent()) {
                ProcessInstance<?> pi = (ProcessInstance<?>) found.get();
                local.put(id, pi);

                intercept(new WorkUnit<ProcessInstance<?>>() {

                    @Override
                    public ProcessInstance<?> data() {
                        return pi;
                    }

                    @Override
                    public void perform() {
                        pi.disconnect();
                    }

                    @Override
                    public void abort() {
                        pi.disconnect();
                    }

                    @Override
                    public Integer priority() {
                        return 200;
                    }
                });
            }

            return found;
        }

        @Override
        public Collection values(ProcessInstanceReadMode mode) {
            return delegate.values(mode);
        }

        @Override
        public Integer size() {
            return delegate.size();
        }

        @Override
        public boolean exists(String id) {
            return local.containsKey(id) || delegate.exists(id);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void create(String id, ProcessInstance instance) {
            delegate.create(id, instance);

            local.put(id, instance);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void update(String id, ProcessInstance instance) {
            delegate.update(id, instance);

            local.put(id, instance);
        }

        @Override
        public void remove(String id) {
            delegate.remove(id);

            local.remove(id);
        }

    }

}
