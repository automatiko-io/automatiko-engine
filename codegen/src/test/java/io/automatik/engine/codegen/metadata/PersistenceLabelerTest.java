
package io.automatik.engine.codegen.metadata;

import org.junit.jupiter.api.Test;

import io.automatik.engine.codegen.metadata.PersistenceLabeler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistenceLabelerTest {

	@Test
	void testGeneratedLabels() {
		Map<String, String> labels = new PersistenceLabeler().generateLabels();
		assertThat(labels).containsEntry(PersistenceLabeler.PERSISTENCE_LABEL_PREFIX, "true");
	}

}