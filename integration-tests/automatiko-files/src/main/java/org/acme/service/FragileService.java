package org.acme.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.workflow.ServiceExecutionError;

@ApplicationScoped
public class FragileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FragileService.class);

    private AtomicLong counter = new AtomicLong(0);

    private AtomicBoolean block = new AtomicBoolean(false);
    private AtomicBoolean failing = new AtomicBoolean(false);
    private String failingCode = "500";

    public Integer getAvailability() {
        maybeFail();
        return 1;
    }

    private void maybeFail() {
        // introduce some artificial failures
        final Long invocationNumber = counter.getAndIncrement();
        if (failing.get()) {
            LOGGER.error("Invocation {} failing", invocationNumber);
            throw new ServiceExecutionError(failingCode, "Service failed.");
        }
        if (block.get()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        LOGGER.info("Invocation {} OK", invocationNumber);
    }

    public Integer getCounter() {
        return counter.intValue();
    }

    public void toogle(String errorCode) {
        if (failing.get()) {
            failing.set(false);
        } else {
            failingCode = errorCode;
            failing.set(true);
        }
    }

    public void toogleBlock() {
        if (block.get()) {
            block.set(false);
        } else {
            block.set(true);
        }
    }

    public void reset() {
        failing.set(false);
        block.set(false);
    }
}
