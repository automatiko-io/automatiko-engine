
package io.automatiko.engine.codegen.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.automatiko.engine.codegen.metadata.PersistenceProtoFilesLabeler;
import io.automatiko.engine.services.utils.IoUtils;

public class PersistenceProtoFilesLabelerTest {

	@Test
	void testGenerateLabels() throws URISyntaxException, IOException {
		final PersistenceProtoFilesLabeler labeler = new PersistenceProtoFilesLabeler();
		final File protoFile = new File(this.getClass().getResource("/automatik-types.proto").toURI());
		final File automatikApplication = new File(this.getClass().getResource("/automatik-application.proto").toURI());

		String originalContent = new String(Files.readAllBytes(protoFile.toPath()));

		assertThat(protoFile).isNotNull();
		assertThat(automatikApplication).isNotNull();

		labeler.processProto(protoFile);
		labeler.processProto(automatikApplication);

		final Map<String, String> labels = labeler.generateLabels();

		assertThat(labels).size().isEqualTo(1);
		assertThat(labels).containsKey(labeler.generateKey(protoFile));
		final byte[] bytes = Base64.getDecoder()
				.decode(labels.get("io.automatik/persistence/proto/automatik-types.proto"));

		byte[] decompresed = decompres(bytes);
		String roundTrip = new String(decompresed);
		assertThat(roundTrip).isEqualTo(originalContent);
	}

	@Test
	void testGenerateLabelsIOException() throws URISyntaxException {
		final PersistenceProtoFilesLabeler labeler = new PersistenceProtoFilesLabeler();
		Assertions.assertThrows(UncheckedIOException.class, () -> labeler.processProto(new File("target")));
	}

	private byte[] decompres(byte[] bytes) throws IOException {
		try (final ByteArrayInputStream fileContents = new ByteArrayInputStream(bytes)) {
			try (final GZIPInputStream gzip = new GZIPInputStream(fileContents)) {
				return IoUtils.readBytesFromInputStream(gzip);
			}

		}
	}

}
