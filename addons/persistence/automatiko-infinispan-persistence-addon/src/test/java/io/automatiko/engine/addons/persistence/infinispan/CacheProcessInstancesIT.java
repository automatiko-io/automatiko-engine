
package io.automatiko.engine.addons.persistence.infinispan;

import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.automatiko.engine.addons.persistence.AbstractProcessInstancesFactory;
import io.automatiko.engine.api.auth.SecurityPolicy;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.WorkItem;
import io.automatiko.engine.services.identity.StaticIdentityProvider;
import io.automatiko.engine.services.io.ClassPathResource;
import io.automatiko.engine.workflow.bpmn2.BpmnProcess;
import io.automatiko.engine.workflow.bpmn2.BpmnVariables;

@Testcontainers
public class CacheProcessInstancesIT {

	@Container
	public InfinispanServerContainer container = new InfinispanServerContainer();

	@Test
	public void testBasicFlow() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.addServer().host("127.0.0.1").port(ConfigurationProperties.DEFAULT_HOTROD_PORT).security()
				.authentication().username("admin").password("admin").realm("default").serverName("infinispan")
				.saslMechanism("DIGEST-MD5").clientIntelligence(ClientIntelligence.BASIC);

		RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());

		BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
		process.setProcessInstancesFactory(new CacheProcessInstancesFactory(cacheManager));
		process.configure();

		ProcessInstance<BpmnVariables> processInstance = process
				.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

		processInstance.start();
		assertEquals(STATE_ACTIVE, processInstance.status());

		SecurityPolicy asJohn = SecurityPolicy.of(new StaticIdentityProvider("john"));
		WorkItem workItem = processInstance.workItems(asJohn).get(0);
		assertNotNull(workItem);
		assertEquals("john", workItem.getParameters().get("ActorId"));
		processInstance.completeWorkItem(workItem.getId(), null, asJohn);
		assertEquals(STATE_COMPLETED, processInstance.status());
	}

	private class CacheProcessInstancesFactory extends AbstractProcessInstancesFactory {

		CacheProcessInstancesFactory(RemoteCacheManager cacheManager) {
			super(cacheManager);
		}

		@Override
		public String proto() {
			return null;
		}

		@Override
		public List<?> marshallers() {
			return Collections.emptyList();
		}
	}
}
