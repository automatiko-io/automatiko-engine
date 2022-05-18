package io.automatiko.addon.files.mongodb;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.automatiko.engine.workflow.file.ByteArrayFile;
import io.quarkus.arc.Arc;

@JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY)
public class GridFSFile extends ByteArrayFile {

    private String url;

    public GridFSFile(String name, byte[] content) {
        super(name, content);
    }

    @JsonCreator
    public GridFSFile(@JsonProperty("name") String name, @JsonProperty("content") byte[] content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        super(name, content, attributes);
    }

    @JsonGetter
    public String name() {
        return name;
    }

    public byte[] content() {
        if (content == null) {
            GridFSStore store = Arc.container().instance(GridFSStore.class).orElse(null);
            if (store != null) {
                content = store.content(url());
            }
        }

        return content;
    }

    @Override
    @JsonGetter
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    @JsonGetter
    public String url() {
        return url;
    }

    @JsonSetter
    public void url(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "GridFSFile [name=" + name + ", content (url)=" + url + ", attributes=" + attributes + "]";
    }
}
