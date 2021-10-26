package io.automatiko.engine.addons.services.receiveemail;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.automatiko.engine.workflow.file.ByteArrayFile;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Attachment extends ByteArrayFile {

    public Attachment(String name, byte[] content) {
        super(name, content == null ? new byte[0] : content);
    }

    public Attachment(@JsonProperty("name") String name, @JsonProperty("content") byte[] content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        super(name, content, attributes);
    }

}
