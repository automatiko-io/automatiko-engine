package io.automatiko.engine.addons.services.archive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.automatiko.engine.api.workflow.files.File;

public class ArchiveServiceTest {

    private ArchiveService service = new ArchiveService();

    @SuppressWarnings("unchecked")
    @Test
    public void testZipAndUnzipOperation() {

        ArchiveFile file1 = new ArchiveFile("hello.txt", "this is my content 1".getBytes());
        ArchiveFile file2 = new ArchiveFile("hi.txt", "this is my content 2".getBytes());
        ArchiveFile file3 = new ArchiveFile("bye.txt", "this is my content 3".getBytes());

        Archive archive = service.zip("archive.zip", file1, file2, file3);
        assertThat(archive).isNotNull();
        assertThat(archive.name()).isEqualTo("archive.zip");
        assertThat(archive.type()).isEqualTo("application/zip");
        assertThat(archive.content()).isNotNull();
        assertThat(archive.attributes()).hasSize(1).containsEntry("entries", "hello.txt,hi.txt,bye.txt");

        List<File<byte[]>> files = service.unzip(archive);
        assertThat(files).isNotNull().hasSize(3);

    }
}
