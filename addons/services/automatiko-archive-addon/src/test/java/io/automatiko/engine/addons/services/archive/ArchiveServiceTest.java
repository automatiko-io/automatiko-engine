package io.automatiko.engine.addons.services.archive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.workflow.files.File;
import io.automatiko.engine.workflow.file.ByteArrayFile;

public class ArchiveServiceTest {

    private ArchiveService service = new ArchiveService();

    @Test
    public void testZipAndUnzipOperation() {

        ByteArrayFile file1 = new ByteArrayFile("hello.txt", "this is my content 1".getBytes());
        ByteArrayFile file2 = new ByteArrayFile("hi.txt", "this is my content 2".getBytes());
        ByteArrayFile file3 = new ByteArrayFile("bye.txt", "this is my content 3".getBytes());

        Archive archive = service.zip("archive.zip", file1, file2, file3);
        assertThat(archive).isNotNull();
        assertThat(archive.name()).isEqualTo("archive.zip");
        assertThat(archive.type()).isEqualTo("application/zip");
        assertThat(archive.content()).isNotNull();
        assertThat(archive.attributes()).hasSize(1).containsEntry("entries", "hello.txt,hi.txt,bye.txt");

        List<File<byte[]>> files = service.unzip(archive);
        assertThat(files).isNotNull().hasSize(3);

    }

    @Test
    public void testZipWithDuplicatedEntries() {

        ByteArrayFile file1 = new ByteArrayFile("hello.txt", "this is my content 1".getBytes());
        ByteArrayFile file2 = new ByteArrayFile("hi.txt", "this is my content 2".getBytes());
        ByteArrayFile file3 = new ByteArrayFile("hello.txt", "this is my content 3".getBytes());

        Archive archive = service.zip("archive.zip", file1, file2, file3);
        assertThat(archive).isNotNull();
        assertThat(archive.name()).isEqualTo("archive.zip");
        assertThat(archive.type()).isEqualTo("application/zip");
        assertThat(archive.content()).isNotNull();
        assertThat(archive.attributes()).hasSize(1).containsEntry("entries", "hello.txt,hi.txt");

    }
}
