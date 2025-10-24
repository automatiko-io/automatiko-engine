package org.drools.core.common;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Map;

public class DroolsObjectOutputStream extends ObjectOutputStream {

    public DroolsObjectOutputStream() throws IOException, SecurityException {
        super();
    }

    public DroolsObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    public Map<String, Object> getCustomExtensions() {
        return null;
    }

    public void addCustomExtensions(String key, Object extension) {

    }

    public void addCloneByIdentity(String key, Object identity) {

    }

    public Map<String, Object> getClonedByIdentity() {
        return null;
    }

    public boolean isCloning() {
        return false;
    }
}
