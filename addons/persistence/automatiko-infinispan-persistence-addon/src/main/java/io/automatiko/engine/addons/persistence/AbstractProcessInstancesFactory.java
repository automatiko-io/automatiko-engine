
package io.automatiko.engine.addons.persistence;

import java.util.Collections;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.protostream.MessageMarshaller;

import io.automatiko.engine.addons.persistence.infinispan.CacheProcessInstances;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstancesFactory;

/**
 * This class must always have exact FQCN as
 * <code>io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory</code>
 *
 */
public abstract class AbstractProcessInstancesFactory implements ProcessInstancesFactory {

    protected RemoteCacheManager cacheManager;

    public AbstractProcessInstancesFactory(RemoteCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public CacheProcessInstances createProcessInstances(Process<?> process) {
        List<?> marshallers = marshallers();
        return new CacheProcessInstances(process, cacheManager, template(), proto(), codec(),
                marshallers.toArray(new MessageMarshaller<?>[marshallers.size()]));
    }

    public String proto() {
        return null;
    }

    public List<?> marshallers() {
        return Collections.emptyList();
    }

    public String template() {
        return null;
    }
}
