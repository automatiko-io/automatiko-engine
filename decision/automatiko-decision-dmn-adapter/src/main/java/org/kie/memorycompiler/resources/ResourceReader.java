package org.kie.memorycompiler.resources;

import java.util.Collection;

public interface ResourceReader {

    boolean isAvailable(final String pResourceName);

    byte[] getBytes(final String pResourceName);

    Collection<String> getFileNames();

    void mark();

    Collection<String> getModifiedResourcesSinceLastMark();
}
