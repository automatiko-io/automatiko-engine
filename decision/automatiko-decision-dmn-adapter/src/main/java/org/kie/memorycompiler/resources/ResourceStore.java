package org.kie.memorycompiler.resources;

public interface ResourceStore {

    void write(final String pResourceName, final byte[] pResourceData);

    void write(final String pResourceName, final byte[] pResourceData, boolean createFolder);

    byte[] read(final String pResourceName);

    void remove(final String pResourceName);
}
