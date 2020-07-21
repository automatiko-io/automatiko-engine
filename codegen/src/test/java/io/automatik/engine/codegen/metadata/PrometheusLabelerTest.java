
package io.automatik.engine.codegen.metadata;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.automatik.engine.codegen.metadata.Labeler;
import io.automatik.engine.codegen.metadata.PrometheusLabeler;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusLabelerTest {

	@Test
	void testGenerateLabels() {
		final Labeler labeler = new PrometheusLabeler();
		final Map<String, String> labels = labeler.generateLabels();

		assertThat(labels).size().isEqualTo(4);
		assertThat(labels).containsEntry(PrometheusLabeler.LABEL_PATH, PrometheusLabeler.DEFAULT_PATH);
		assertThat(labels).containsEntry(PrometheusLabeler.LABEL_PORT, PrometheusLabeler.DEFAULT_PORT);
		assertThat(labels).containsEntry(PrometheusLabeler.LABEL_SCHEME, PrometheusLabeler.DEFAULT_SCHEME);
		assertThat(labels).containsEntry(PrometheusLabeler.LABEL_SCRAPE, PrometheusLabeler.DEFAULT_SCRAPE);
	}

}
