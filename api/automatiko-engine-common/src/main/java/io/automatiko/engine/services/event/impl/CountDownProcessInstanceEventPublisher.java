package io.automatiko.engine.services.event.impl;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.automatiko.engine.api.event.DataEvent;
import io.automatiko.engine.api.event.EventPublisher;
import io.automatiko.engine.services.event.ProcessInstanceDataEvent;

/**
 * Event publisher used for tracking execution to be able to efficiently wait for completion
 * Main use case for it is testing of business logic
 */
public class CountDownProcessInstanceEventPublisher implements EventPublisher {

    private static CountDownLatch latch;

    @Override
    public void publish(DataEvent<?> event) {
        if (event instanceof ProcessInstanceDataEvent) {
            latch.countDown();
        }
    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        events.forEach(e -> publish(e));
    }

    /**
     * Sets the count down latch to given number of process instance events.
     * 
     * @param count number of expected events
     */
    public void reset(int count) {
        latch = new CountDownLatch(count);
    }

    /**
     * Blocks the execution and wait for all expected process instance events to arrive
     * 
     * @param timeout maximum amount of time (in seconds) it should wait
     * @throws InterruptedException in case it was interrupted while waiting
     */
    public void waitTillCompletion(long timeout) throws InterruptedException {
        waitTillCompletion(timeout, TimeUnit.SECONDS);
    }

    /**
     * Blocks the execution and wait for all expected process instance events to arrive
     * 
     * @param timeout maximum amount of time (in given time unit) it should wait
     * @param unit time unit of the given timeout
     * @throws InterruptedException in case it was interrupted while waiting
     */
    public void waitTillCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        boolean isZero = latch.await(timeout, unit);

        if (!isZero) {
            throw new RuntimeException("Exceeded time waiting on completion");
        }
    }
}
