
package io.automatik.engine.addons.jobs.management.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.automatik.engine.api.jobs.ExactExpirationTime;
import io.automatik.engine.api.jobs.ProcessInstanceJobDescription;
import io.automatik.engine.api.jobs.ProcessJobDescription;
import io.automatik.engine.jobs.api.Job;
import io.automatik.engine.jobs.api.JobNotFoundException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class HttpBasedJobsServiceTest {

	public static final String CALLBACK_URL = "http://localhost";
	public static final String JOB_SERVICE_URL = "http://localhost:8085";

	private HttpBasedJobsService tested;

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	@Mock
	private Vertx vertx;

	@Mock
	private WebClient webClient;

	@Mock
	private Instance instance;

	@BeforeEach
	public void setUp() {
		when(instance.isResolvable()).thenReturn(true);
		when(instance.get()).thenReturn(webClient);

		tested = new HttpBasedJobsService(JOB_SERVICE_URL, CALLBACK_URL, vertx, instance);
		tested.initialize();
	}

	@AfterAll
	public void cleanup() {
		executor.shutdownNow();
	}

	@Test
	void testInitialize() {
		reset(instance);
		when(instance.isResolvable()).thenReturn(false);
		tested = new HttpBasedJobsService(JOB_SERVICE_URL, CALLBACK_URL, vertx, instance);
		tested.initialize();
		verify(instance, never()).get();
	}

	@Test
	void testScheduleProcessJob() {
		ProcessJobDescription processJobDescription = ProcessJobDescription.of(ExactExpirationTime.now(), 1,
				"processId", "1");
		assertThatThrownBy(() -> tested.scheduleProcessJob(processJobDescription))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void testScheduleProcessInstanceJob(@Mock HttpRequest<Buffer> request) {
		when(webClient.post(anyString())).thenReturn(request);

		ProcessInstanceJobDescription processInstanceJobDescription = ProcessInstanceJobDescription.of(123,
				ExactExpirationTime.now(), "processInstanceId", "processId", "1");
		tested.scheduleProcessInstanceJob(processInstanceJobDescription);
		verify(webClient).post("/jobs");
		ArgumentCaptor<Job> jobArgumentCaptor = forClass(Job.class);
		verify(request).sendJson(jobArgumentCaptor.capture(), any(Handler.class));
		Job job = jobArgumentCaptor.getValue();
		assertThat(job.getId()).isEqualTo(processInstanceJobDescription.id());
		assertThat(job.getExpirationTime()).isEqualTo(processInstanceJobDescription.expirationTime().get());
		assertThat(job.getProcessInstanceId()).isEqualTo(processInstanceJobDescription.processInstanceId());
		assertThat(job.getProcessId()).isEqualTo(processInstanceJobDescription.processId());
	}

	@Test
	void testCancelJob(@Mock HttpRequest<Buffer> request) {
		when(webClient.delete(anyString())).thenReturn(request);
		tested.cancelJob("123");
		verify(webClient).delete("/jobs/123");
	}

	@Test
	void testGetScheduleTime(@Mock HttpRequest<Buffer> request, @Mock HttpResponse<Buffer> response) {
		when(webClient.get(anyString())).thenReturn(request);
		Job job = new Job();
		job.setId("123");
		job.setExpirationTime(ZonedDateTime.now());
		AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
		when(asyncResult.succeeded()).thenReturn(true);
		when(asyncResult.result()).thenReturn(response);
		when(response.statusCode()).thenReturn(200);
		when(response.bodyAsJson(any())).thenReturn(job);

		doAnswer(invocationOnMock -> {
			Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocationOnMock.getArgument(0);
			executor.submit(() -> handler.handle(asyncResult));
			return null;
		}).when(request).send(any());

		ZonedDateTime scheduledTime = tested.getScheduledTime("123");
		assertThat(scheduledTime).isEqualTo(job.getExpirationTime());
		verify(webClient).get("/jobs/123");
	}

	@Test
	void testGetScheduleTimeJobNotFound(@Mock HttpRequest<Buffer> request, @Mock HttpResponse<Buffer> response) {
		when(webClient.get(anyString())).thenReturn(request);
		AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
		when(asyncResult.succeeded()).thenReturn(true);
		when(asyncResult.result()).thenReturn(response);
		when(response.statusCode()).thenReturn(404);

		doAnswer(invocationOnMock -> {
			Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocationOnMock.getArgument(0);
			executor.submit(() -> handler.handle(asyncResult));
			return null;
		}).when(request).send(any());

		assertThatThrownBy(() -> tested.getScheduledTime("123")).hasCauseExactlyInstanceOf(JobNotFoundException.class);

		verify(webClient).get("/jobs/123");
	}
}