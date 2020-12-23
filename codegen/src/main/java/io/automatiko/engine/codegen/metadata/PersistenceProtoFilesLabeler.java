
package io.automatiko.engine.codegen.metadata;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import io.automatiko.engine.codegen.process.persistence.proto.Proto;

/**
 * Responsible for generating image labels representing generated proto files
 */
public class PersistenceProtoFilesLabeler implements Labeler {

	private static final String PERSISTENCE_PROTO_LABEL_PREFIX = ImageMetaData.LABEL_PREFIX + "persistence/proto/";
	private static final String APPLICATION_PROTO = "automatik-application.proto";
	public static final String PROTO_FILE_EXT = ".proto";
	private final Map<String, String> encodedProtos = new HashMap<>();

	/**
	 * Transforms the given {@link Proto} into a format for the
	 * {@link ImageMetaData}
	 * 
	 * @param file that will be added to the label
	 * @throws IOException
	 */
	public void processProto(final File file) {
		try {
			if (file != null && !APPLICATION_PROTO.equalsIgnoreCase(file.getName())) {
				this.encodedProtos.put(generateKey(file), compressFile(file));
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Error while processing proto files as image labels", e);
		}
	}

	protected String compressFile(final File file) throws IOException {
		final byte[] contents = Files.readAllBytes(file.toPath());
		if (contents == null) {
			return "";
		}
		try (final ByteArrayOutputStream fileContents = new ByteArrayOutputStream(contents.length)) {
			try (final GZIPOutputStream gzip = new GZIPOutputStream(fileContents)) {
				gzip.write(contents);
			}
			return Base64.getEncoder().encodeToString(fileContents.toByteArray());
		}
	}

	protected String generateKey(final File file) {
		return String.format("%s%s", PERSISTENCE_PROTO_LABEL_PREFIX, file.getName());
	}

	@Override
	public Map<String, String> generateLabels() {
		return encodedProtos;
	}

}
