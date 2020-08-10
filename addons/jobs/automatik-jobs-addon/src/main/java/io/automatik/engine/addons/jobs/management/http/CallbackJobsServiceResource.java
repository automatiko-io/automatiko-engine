
package io.automatik.engine.addons.jobs.management.http;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.automatik.engine.api.Application;
import io.automatik.engine.api.workflow.Process;
import io.automatik.engine.api.workflow.ProcessInstance;
import io.automatik.engine.api.workflow.Processes;
import io.automatik.engine.services.time.TimerInstance;
import io.automatik.engine.services.uow.UnitOfWorkExecutor;
import io.automatik.engine.workflow.Sig;

@Path("/management/jobs")
public class CallbackJobsServiceResource {

	@Inject
	Processes processes;

	@Inject
	Application application;

	@POST
	@Path("{processId}/instances/{processInstanceId}/timers/{timerId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response triggerTimer(@PathParam("processId") String processId,
			@PathParam("processInstanceId") String processInstanceId, @PathParam("timerId") String timerId,
			@QueryParam("limit") @DefaultValue("0") Integer limit) {
		if (processId == null || processInstanceId == null) {
			return Response.status(Status.BAD_REQUEST).entity("Process id and Process instance id must be given")
					.build();
		}

		Process<?> process = processes.processById(processId);
		if (process == null) {
			return Response.status(Status.NOT_FOUND).entity("Process with id " + processId + " not found").build();
		}

		return UnitOfWorkExecutor.executeInUnitOfWork(application.unitOfWorkManager(), () -> {
			Optional<? extends ProcessInstance<?>> processInstanceFound = process.instances()
					.findById(processInstanceId);
			if (processInstanceFound.isPresent()) {
				ProcessInstance<?> processInstance = processInstanceFound.get();
				String[] ids = timerId.split("_");
				processInstance
						.send(Sig.of("timerTriggered", TimerInstance.with(Long.parseLong(ids[1]), timerId, limit)));
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity("Process instance with id " + processInstanceId + " not found").build();
			}

			return Response.status(Status.OK).build();

		});

	}

}
