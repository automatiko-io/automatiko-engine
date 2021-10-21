package io.automatiko.engine.workflow.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializationOfFilesTest {

    @Test
    public void testUrlFileSerialization() throws JsonProcessingException {

        UrlFile attachment = new UrlFile("test.pdf", "http://host:8080/file");
        attachment.attributes().put("author", "john");
        attachment.attributes().put("size", "100");

        ObjectMapper mapper = new ObjectMapper();

        String value = mapper.writeValueAsString(attachment);

        assertThat(value).isNotNull();

        UrlFile object = mapper.readValue(value, UrlFile.class);
        assertThat(object.type()).isEqualTo("application/pdf");
        assertThat(object.name()).isEqualTo("test.pdf");
        assertThat(object.attributes()).hasSize(2).containsEntry("size", "100").containsEntry("author", "john");
        assertThat(object.url()).isEqualTo("http://host:8080/file");
    }
}
