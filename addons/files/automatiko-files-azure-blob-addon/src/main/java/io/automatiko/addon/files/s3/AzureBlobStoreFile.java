package io.automatiko.addon.files.s3;

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
public class AzureBlobStoreFile extends ByteArrayFile {

    private String url;

    public AzureBlobStoreFile(String name, byte[] content) {
        super(name, content);
    }

    @JsonCreator
    public AzureBlobStoreFile(@JsonProperty("name") String name, @JsonProperty("content") byte[] content,
            @JsonProperty("attributes") Map<String, String> attributes) {
        super(name, content, attributes);
    }

    @JsonGetter
    public String name() {
        return name;
    }

    public byte[] content() {
        if (content == null) {
            AzureBlobStoreStore store = Arc.container().instance(AzureBlobStoreStore.class).orElse(null);
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
        return "AzureBlob [name=" + name + ", content (url)=" + url + ", attributes=" + attributes + "]";
    }
}
