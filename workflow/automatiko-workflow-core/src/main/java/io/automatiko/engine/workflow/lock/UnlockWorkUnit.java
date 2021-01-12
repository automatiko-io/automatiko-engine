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
            LOGGER.debug("Unlocking instance {} on thread {} lock {}", this, Thread.currentThread().getName(), lock);
            // make sure it's completely unlocked as it only happens when instance execution is done
            while (lock.getHoldCount() > 0) {
                lock.unlock();
            }
            LOGGER.debug("Unlocked instance {} on thread {} lock {}", this, Thread.currentThread().getName(), lock);
        }

        if (removeLock) {
            ((AbstractProcess<?>) instance.process()).locks().remove(instance.businessKey());
            LOGGER.debug("Instance {} completed on thread {} removing lock {}", this, Thread.currentThread().getName(), lock);
        }
    }
}
