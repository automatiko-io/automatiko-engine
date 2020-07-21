
package io.automatik.engine.codegen.metadata;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class MetaDataWriter {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private MetaDataWriter() {

	}

	/**
	 * Writes the given labels into the image_metadata.json file in the given
	 * directory
	 * 
	 * @param targetDirectory
	 * @param labels
	 */
	public static void writeLabelsImageMetadata(final File targetDirectory, final Map<String, String> labels) {
		try {
			Path imageMetaDataFile = Paths.get(targetDirectory.getAbsolutePath(), "image_metadata.json");
			ImageMetaData imageMetadata;

			if (imageMetaDataFile.toFile().exists()) {
				// read the file to merge the content
				imageMetadata = MAPPER.readValue(imageMetaDataFile.toFile(), ImageMetaData.class);
			} else {
				imageMetadata = new ImageMetaData();
			}
			imageMetadata.add(labels);

			Files.createDirectories(imageMetaDataFile.getParent());
			Files.write(imageMetaDataFile, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(imageMetadata)
					.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
