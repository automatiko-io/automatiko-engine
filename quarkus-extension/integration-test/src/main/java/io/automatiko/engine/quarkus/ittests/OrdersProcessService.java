
package io.automatiko.engine.quarkus.ittests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.automatiko.engine.api.Model;
import io.automatiko.engine.api.workflow.Process;
import io.automatiko.engine.api.workflow.ProcessInstance;
import io.automatiko.engine.api.workflow.ProcessInstances;
import io.automatiko.engine.api.workflow.WorkItem;

@Path("/runProcess")
public class OrdersProcessService {

	@Inject
	@Named("orders_1_0")
	Process<? extends Model> orderProcess;

	@Inject
	@Named("orderItems")
	Process<? extends Model> orderItemsProcess;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String testOrderProcess() {
		Model m = orderProcess.createModel();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("approver", "john");
		parameters.put("order", new Order("12345", false, 0.0));
		m.fromMap(parameters);

		ProcessInstance<?> processInstance = orderProcess.createInstance(m);
		processInstance.start();

		assert (io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_ACTIVE == processInstance.status());
		Model result = (Model) processInstance.variables();
		assert (result.toMap().size() == 2);
		assert (((Order) result.toMap().get("order")).getTotal() > 0);

		ProcessInstances<? extends Model> orderItemProcesses = orderItemsProcess.instances();
		assert (orderItemProcesses.values(1, 10).size() == 1);

		ProcessInstance<?> childProcessInstance = orderItemProcesses.values(1, 10).iterator().next();

		List<WorkItem> workItems = childProcessInstance.workItems();
		assert (workItems.size()) == 1;

		childProcessInstance.completeWorkItem(workItems.get(0).getId(), null);

		assert (io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED == childProcessInstance
				.status());
		assert (io.automatiko.engine.api.runtime.process.ProcessInstance.STATE_COMPLETED == processInstance.status());

		// no active process instances for both orders and order items processes
		assert (orderProcess.instances().values(1, 10).size() == 0);
		assert (orderItemsProcess.instances().values(1, 10).size() == 0);

		return "OK";
	}
}
