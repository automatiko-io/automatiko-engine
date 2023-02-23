package io.automatiko.engine.addons.services.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.workflow.ServiceExecutionError;
import io.automatiko.engine.api.workflow.files.File;
import io.automatiko.engine.workflow.file.ByteArrayFile;

@ApplicationScoped
public class ArchiveService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveService.class);

    /**
     * Builds an archive with given files. built archive will be named based on the given
     * name so it should include the extension as well.
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>zipFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param name name of the archive
     * @param files files to be included in the archive
     * @return built archive with given files
     */
    @SuppressWarnings("rawtypes")
    public Archive zip(String name, Collection<File> files) {
        return zip(name, files.toArray(File[]::new));
    }

    /**
     * Builds an archive with given files. built archive will be named based on the given
     * name so it should include the extension as well.
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>zipFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param name name of the archive
     * @param files files to be included in the archive
     * @return built archive with given files
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Archive zip(String name, File... files) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<String> entries = new ArrayList<>();
        try (ZipOutputStream zipOut = new ZipOutputStream(output)) {

            for (File<byte[]> file : files) {
                if (file == null || file.content() == null) {
                    continue;
                }

                if (entries.contains(file.name())) {
                    LOGGER.warn("Duplicated archive entry with name '{}', will be ignored", file.name());
                    continue;
                }

                ZipEntry zipEntry = new ZipEntry(file.name());
                zipOut.putNextEntry(zipEntry);
                zipOut.write(file.content());

                entries.add(file.name());
            }

        } catch (IOException e) {
            throw new ServiceExecutionError("zipFailure", e.getMessage());
        }

        Archive archive = new Archive(name, output.toByteArray());
        archive.attributes().put(Archive.ENTRIES_ATTR, entries.stream().collect(Collectors.joining(",")));
        return archive;
    }

    /**
     * Extracts given archive into a list of files.
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>unzipFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param archive archive to be extracted
     * @return list of files extracted from the archive
     */
    public List<File<byte[]>> unzip(Archive archive) {
        List<File<byte[]>> files = new ArrayList<File<byte[]>>();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(archive.content())) {
            try (ZipInputStream zipIn = new ZipInputStream(bais)) {
                byte[] buffer = new byte[1024];

                ZipEntry entry = zipIn.getNextEntry();
                while (entry != null) {

                    if (entry.isDirectory()) {
                        entry = zipIn.getNextEntry();
                        continue;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zipIn.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    baos.close();

                    files.add(new ByteArrayFile(entry.getName(), baos.toByteArray()));

                    entry = zipIn.getNextEntry();
                }
            } catch (Exception e) {
                throw new ServiceExecutionError("unzipFailure", e);
            }
        } catch (IOException e1) {
            throw new ServiceExecutionError("unzipFailure", e1);
        }
        return files;
    }
}
