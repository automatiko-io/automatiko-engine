
package io.automatiko.engine.addons.jobs.management.http;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.automatiko.engine.api.jobs.JobsService;
import io.automatiko.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatiko.engine.api.jobs.ProcessJobDescription;
import io.automatiko.engine.jobs.api.Job;
import io.automatiko.engine.jobs.api.JobBuilder;
import io.automatiko.engine.jobs.api.JobNotFoundException;
import io.automatiko.engine.jobs.api.URIBuilder;
import io.quarkus.arc.properties.IfBuildProperty;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@IfBuildProperty(name = "quarkus.automatiko.jobs.type", stringValue = "http")
@ApplicationScoped
public class HttpBasedJobsService implements JobsService {

	public static final String JOBS_PATH = "/jobs";

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpBasedJobsService.class);

	private Vertx vertx;

	private Instance<WebClient> providedWebClient;

	private WebClient client;

	private URI jobsServiceUri;
	private String callbackEndpoint;

	@Inject
	public HttpBasedJobsService(@ConfigProperty(name = "quarkus.automatiko.jobs.http.url") String jobServiceUrl,
			@ConfigProperty(name = "quarkus.automatiko.service-url") String callbackEndpoint, Vertx vertx,
			Instance<WebClient> providedWebClient) {
		this.jobsServiceUri = Objects.nonNull(jobServiceUrl) ? buildJobsServiceURI(jobServiceUrl) : null;
		this.callbackEndpoint = callbackEndpoint;
		this.vertx = vertx;
		this.providedWebClient = providedWebClient;
	}

	HttpBasedJobsService() {
		this(null, null, null, null);
	}

	@PostConstruct
	void initialize() {
		DatabindCodec.mapper().disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
		DatabindCodec.mapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		DatabindCodec.mapper().registerModule(new JavaTimeModule());
		DatabindCodec.mapper().findAndRegisterModules();

		DatabindCodec.prettyMapper().registerModule(new JavaTimeModule());
		DatabindCodec.prettyMapper().findAndRegisterModules();
		DatabindCodec.prettyMapper().disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
		DatabindCodec.prettyMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		if (providedWebClient.isResolvable()) {
			this.client = providedWebClient.get();
			LOGGER.debug("Using provided web client instance");
		} else {
			final URI jobServiceURL = getJobsServiceUri();
			this.client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(jobServiceURL.getHost())
					.setDefaultPort(jobServiceURL.getPort()));
			LOGGER.debug("Creating new instance of web client for host {} and port {}", jobServiceURL.getHost(),
					jobServiceURL.getPort());
		}
	}

	@Override
	public String scheduleProcessJob(ProcessJobDescription description) {

		throw new UnsupportedOperationException("Scheduling for process jobs is not yet implemented");
	}

	@Override
	public String scheduleProcessInstanceJob(ProcessInstanceJobDescription description) {
		String callback = getCallbackEndpoint(description);
		LOGGER.debug("Job to be scheduled {} with callback URL {}", description, callback);
		final Job job = JobBuilder.builder().id(description.id()).expirationTime(description.expirationTime().get())
				.repeatInterval(description.expirationTime().repeatInterval())
				.repeatLimit(description.expirationTime().repeatLimit()).priority(0).callbackEndpoint(callback)
				.processId(description.processId()).processInstanceId(description.processInstanceId())
				.rootProcessId(description.rootProcessId()).rootProcessInstanceId(description.rootProcessInstanceId())
				.build();

		client.post(JOBS_PATH).sendJson(job, res -> {

			if (res.succeeded() && res.result().statusCode() == 200) {
				LOGGER.debug("Creating of the job {} done with status code {} ", job, res.result().statusCode());
			} else {
				LOGGER.error("Scheduling of job {} failed with response code {}", job, res.result().statusCode(),
						res.cause());
			}
		});

		return job.getId();
	}

	@Override
	public boolean cancelJob(String id) {
		client.delete(JOBS_PATH + "/" + id).send(res -> {
			if (res.succeeded() && (res.result().statusCode() == 200 || res.result().statusCode() == 404)) {
				LOGGER.debug("Canceling of the job {} done with status code {} ", id, res.result().statusCode());
			} else {
				LOGGER.error("Canceling of job {} failed with response code {}", id, res.result().statusCode(),
						res.cause());
			}
		});

		return true;
	}

	@Override
	public ZonedDateTime getScheduledTime(String id) {
		CompletableFuture<Job> future = new CompletableFuture<Job>();

		client.get(JOBS_PATH + "/" + id).send(res -> {
			if (res.succeeded() && res.result().statusCode() == 200) {
				future.complete(res.result().bodyAsJson(Job.class));
			} else if (res.succeeded() && res.result().statusCode() == 404) {
				future.completeExceptionally(new JobNotFoundException(id));
			} else {
				future.completeExceptionally(new RuntimeException("Unable to find job with id " + id));
			}
		});

		try {
			return future.get().getExpirationTime();
		} catch (Exception e) {
			if (e.getCause() != null) {
				throw new RuntimeException(e.getCause());
			}

			throw new RuntimeException(e);
		}

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
