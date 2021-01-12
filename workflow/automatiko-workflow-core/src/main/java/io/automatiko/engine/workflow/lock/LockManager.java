package io.automatiko.engine.workflow.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {

    private ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

    public synchronized ReentrantLock lock(String id) {

        return locks.computeIfAbsent(id, key -> new ReentrantLock());
    }

    public void remove(String id) {
        if (id != null) {
            locks.remove(id);
        }
    }

}
