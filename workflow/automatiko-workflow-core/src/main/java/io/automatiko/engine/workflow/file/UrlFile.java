package io.automatiko.engine.workflow.file;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.automatiko.engine.api.workflow.files.File;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class UrlFile implements File<String> {

    private final String name;

    private final String content;

    private final Map<String, String> attributes;

    public UrlFile(String name, String content) {
        this.name = name;
        this.content = content;
        this.attributes = new HashMap<>();
    }

    @JsonCreator
    public UrlFile(@JsonProperty("name") String name, @JsonProperty("content") String content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        this.name = name;
        this.content = content;
        this.attributes = attributes;
    }

    public String name() {
        return name;
    }

    public String content() {
        return content;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public String url() {
        return content();
    }

    @Override
    public String toString() {
        return "UrlFile [name=" + name + ", content (url)=" + content + ", attributes=" + attributes + "]";
    }
}
