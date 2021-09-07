package io.automatiko.engine.addons.services.email;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.automatiko.engine.workflow.file.ByteArrayFile;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Attachment extends ByteArrayFile {

    public Attachment(String name, byte[] content) {
        super(name, content);
    }

    public Attachment(@JsonProperty("name") String name, @JsonProperty("content") byte[] content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        super(name, content, attributes);
    }

}
