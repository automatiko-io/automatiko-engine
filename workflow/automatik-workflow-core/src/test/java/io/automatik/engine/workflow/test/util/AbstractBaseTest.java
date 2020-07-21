
package io.automatik.engine.workflow.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;

import io.automatik.engine.api.definition.process.Process;
import io.automatik.engine.workflow.base.instance.InternalProcessRuntime;
import io.automatik.engine.workflow.base.instance.ProcessRuntimeImpl;
import io.automatik.engine.workflow.base.instance.impl.util.LoggingPrintStream;
import io.automatik.engine.workflow.process.test.TestProcessEventListener;

public abstract class AbstractBaseTest {

	protected Logger logger;

	@BeforeEach
	public void before(TestInfo testInfo) {
		addLogger();
		logger.debug("> " + testInfo.getDisplayName());
	}

	public abstract void addLogger();

	protected static AtomicInteger uniqueIdGen = new AtomicInteger(0);

	public InternalProcessRuntime createProcessRuntime(Process... process) {
		Map<String, Process> mappedProcesses = Stream.of(process).collect(Collectors.toMap(Process::getId, p -> p));

		InternalProcessRuntime processRuntime = new ProcessRuntimeImpl(mappedProcesses);
		return processRuntime;
	}

	public void showEventHistory(InternalProcessRuntime processRuntime) {
		TestProcessEventListener procEventListener = (TestProcessEventListener) processRuntime
				.getProcessEventListeners().iterator().next();
		for (String event : procEventListener.getEventHistory()) {
			System.out.println("\"" + event + "\",");
		}
	}

	public void verifyEventHistory(String[] eventOrder, List<String> eventHistory) {
		int max = eventOrder.length > eventHistory.size() ? eventOrder.length : eventHistory.size();
		logger.debug("{} | {}", "EXPECTED", "TEST");
		for (int i = 0; i < max; ++i) {
			String expected = "", real = "";
			if (i < eventOrder.length) {
				expected = eventOrder[i];
			}
			if (i < eventHistory.size()) {
				real = eventHistory.get(i);
			}
			logger.debug("{} | {}", expected, real);
			assertEquals(expected, real, "Mismatch in expected event");
		}
		assertEquals(eventOrder.length, eventHistory.size(), "Mismatch in number of events expected.");
	}

	@BeforeAll
	public static void configure() {
		LoggingPrintStream.interceptSysOutSysErr();
	}

	@AfterAll
	public static void reset() {
		LoggingPrintStream.resetInterceptSysOutSysErr();
	}

	protected byte[] readBytesFromInputStream(InputStream input, boolean closeInput) throws IOException {
		try {
			byte[] buffer = createBytesBuffer(input);
			try (ByteArrayOutputStream output = new ByteArrayOutputStream(buffer.length)) {
				int n = 0;
				while (-1 != (n = input.read(buffer))) {
					output.write(buffer, 0, n);
				}
				return output.toByteArray();
			}
		} finally {
			try {
				if (closeInput) {
					input.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}

	protected byte[] createBytesBuffer(InputStream input) throws IOException {
		return new byte[Math.max(input.available(), 8192)];
	}
}
