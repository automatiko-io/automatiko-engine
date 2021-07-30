package io.automatiko.engine.workflow.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import io.automatiko.engine.api.workflow.ArchivedVariable;
import io.automatiko.engine.api.workflow.files.File;
import io.automatiko.engine.services.utils.IoUtils;

public class FileArchivedVariable extends ArchivedVariable {

    public FileArchivedVariable(String name, Object value) {
        super(name, value);
    }

    @Override
    public byte[] data() {
        File<?> file = (File<?>) getValue();

        if (file.content() instanceof InputStream) {
            try {
                return IoUtils.readBytesFromInputStream((InputStream) file.content());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return (byte[]) file.content();
        }

    }

    @Override
    public String getName() {
        File<?> file = (File<?>) getValue();
        return file.name();
    }

}
