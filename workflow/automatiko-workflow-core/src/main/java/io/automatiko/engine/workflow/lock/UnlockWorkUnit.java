package io.automatiko.engine.workflow.lock;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.uow.WorkUnit;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.workflow.AbstractProcess;

public class UnlockWorkUnit implements WorkUnit<ReentrantLock> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(UnlockWorkUnit.class);

    private ReentrantLock lock;
    private ProcessInstance<?> instance;

    private boolean removeLock;

    public UnlockWorkUnit(ProcessInstance<?> instance, ReentrantLock lock) {
        this.instance = instance;
        this.lock = lock;
    }

    public UnlockWorkUnit(ProcessInstance<?> instance, ReentrantLock lock, boolean removeLock) {
        this.instance = instance;
        this.lock = lock;
        this.removeLock = removeLock;
    }

    @Override
    public ReentrantLock data() {
        return null;
    }

    @Override
    public void perform() {
        unlock();
    }

    @Override
    public void abort() {
        unlock();
    }

    @Override
    public Integer priority() {
        return 10000;
    }

    protected void unlock() {
        if (lock.isHeldByCurrentThread()) {
            LOGGER.debug("Unlocking instance {} ({}) on thread {} lock {}", instance.id(), instance.businessKey(),
                    Thread.currentThread().getName(), lock);
            // make sure it's completely unlocked as it only happens when instance execution is done
            while (lock.getHoldCount() > 0) {
                lock.unlock();
            }
            LOGGER.debug("Unlocked instance {} ({}) on thread {} lock {}", instance.id(), instance.businessKey(),
                    Thread.currentThread().getName(), lock);
        }

        if (removeLock) {
            ((AbstractProcess<?>) instance.process()).locks().remove(instance.businessKey());
            LOGGER.debug("Instance {} ({}) completed on thread {} removing lock {}", instance.id(), instance.businessKey(),
                    Thread.currentThread().getName(), lock);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((instance.id() == null) ? 0 : instance.id().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnlockWorkUnit other = (UnlockWorkUnit) obj;
        if (instance.id() == null) {
            if (other.instance.id() != null)
                return false;
        } else if (!instance.id().equals(other.instance.id()))
            return false;
        return true;
    }
}
