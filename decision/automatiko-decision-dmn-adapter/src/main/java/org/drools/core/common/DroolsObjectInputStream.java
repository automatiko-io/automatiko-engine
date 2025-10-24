package org.drools.core.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class DroolsObjectInputStream extends ObjectInputStream {

    protected DroolsObjectInputStream() throws IOException, SecurityException {
        super();
    }

    public Map<String, Object> getCustomExtensions() {
        return null;
    }

    public void addCustomExtensions(String key, Object extension) {

    }
}
