package io.automatiko.engine.workflow.file;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.automatiko.engine.api.workflow.files.File;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class ByteArrayFile implements File<byte[]> {

    private final String name;

    private final byte[] content;

    private final Map<String, String> attributes;

    public ByteArrayFile(String name, byte[] content) {
        this.name = name;
        this.content = content;
        this.attributes = new HashMap<>();
    }

    @JsonCreator
    public ByteArrayFile(@JsonProperty("name") String name, @JsonProperty("content") byte[] content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        this.name = name;
        this.content = content;
        this.attributes = attributes;
    }

    public String name() {
        return name;
    }

    public byte[] content() {
        return content;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public String url() {
        return "data:" + type() + ";base64," + Base64.getEncoder().encodeToString(content());
    }

    @Override
    public String toString() {
        return "ByteArrayFile [name=" + name + ", content (size)=" + content.length + ", attributes=" + attributes + "]";
    }
}
