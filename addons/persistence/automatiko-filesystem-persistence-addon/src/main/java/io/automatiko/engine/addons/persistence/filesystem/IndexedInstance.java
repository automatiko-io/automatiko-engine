package io.automatiko.engine.addons.persistence.filesystem;

import java.util.Collection;

public class IndexedInstance {

    private final String id;

    private Collection<String> tags;

    public IndexedInstance(String id, Collection<String> tags) {
        this.id = id;
        this.tags = tags;
    }

    public String id() {
        return id;
    }

    public boolean match(String... values) {
        for (String value : values) {
            if (tags.contains(value)) {
                return true;
            }
        }

        return false;
    }

}
