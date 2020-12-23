
package io.automatiko.engine.jobs.api;

import java.net.URI;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.jobs.api.URIBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class URIBuilderTest {

	@Test
	void testToURIHttpNoPort() {
		URI jobServiceURL = URIBuilder.toURI("http://localhost/resource1/resource2?x=1&y=2");
		assertHttp(jobServiceURL, "http", 80);
	}

	@Test
	void testToURIHttpsNoPort() {
		URI jobServiceURL = URIBuilder.toURI("https://localhost/resource1/resource2?x=1&y=2");
		assertHttp(jobServiceURL, "https", 443);
	}

	@Test
	void testToURIHttpWithPort() {
		URI jobServiceURL = URIBuilder.toURI("http://localhost:8080/resource1/resource2?x=1&y=2");
		assertHttp(jobServiceURL, "http", 8080);
	}

	@Test
	void testToURIHttpsWithPort() {
		URI jobServiceURL = URIBuilder.toURI("https://localhost:4443/resource1/resource2?x=1&y=2");
		assertHttp(jobServiceURL, "https", 4443);
	}

	private void assertHttp(URI jobServiceURL, String http, int i) {
		assertThat(jobServiceURL.getScheme()).isEqualTo(http);
		assertThat(jobServiceURL.getHost()).isEqualTo("localhost");
		assertThat(jobServiceURL.getPort()).isEqualTo(i);
		assertThat(jobServiceURL.getPath()).isEqualTo("/resource1/resource2");
		assertThat(jobServiceURL.getQuery()).isEqualTo("x=1&y=2");
	}
}