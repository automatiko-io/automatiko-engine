package io.automatiko.engine.addons.services.archive;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.automatiko.engine.workflow.file.ByteArrayFile;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class Archive extends ByteArrayFile {

    public Archive(String name, byte[] content) {
        super(name, content);
    }

    @JsonCreator
    public Archive(@JsonProperty("name") String name, @JsonProperty("content") byte[] content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        super(name, content, attributes);
    }

    @Override
    public String type() {
        return "application/zip";
    }

    @Override
    public String toString() {
        return "Archive [name=" + name + ", content (entries)=" + attributes.get(ENTRIES_ATTR) + "]";
    }

}
