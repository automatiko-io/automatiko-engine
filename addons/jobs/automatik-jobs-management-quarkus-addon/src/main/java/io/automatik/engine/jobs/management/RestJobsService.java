
package io.automatik.engine.jobs.management;

import java.net.URI;
import java.util.Objects;

import io.automatik.engine.api.jobs.JobsService;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.jobs.api.URIBuilder;

public abstract class RestJobsService implements JobsService {

	public static final String JOBS_PATH = "/jobs";

	private URI jobsServiceUri;
	private String callbackEndpoint;

	public RestJobsService(String jobServiceUrl, String callbackEndpoint) {
		this.jobsServiceUri = Objects.nonNull(jobServiceUrl) ? buildJobsServiceURI(jobServiceUrl) : null;
		this.callbackEndpoint = callbackEndpoint;
	}

	public String getCallbackEndpoint(ProcessInstanceJobDescription description) {
		return URIBuilder.toURI(callbackEndpoint + "/management/jobs/" + description.processId() + "/instances/"
				+ description.processInstanceId() + "/timers/" + description.id()).toString();
	}

	private URI buildJobsServiceURI(String jobServiceUrl) {
		return URIBuilder.toURI(jobServiceUrl + JOBS_PATH);
	}

	public URI getJobsServiceUri() {
		return jobsServiceUri;
	}
}
