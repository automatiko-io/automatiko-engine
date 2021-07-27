package io.automatiko.engine.addons.services.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AttachmentSeralizationTest {

    @Test
    public void testAttachmentSerialization() throws JsonProcessingException {

        Attachment attachment = new Attachment("test.pdf", "content".getBytes());
        attachment.attributes().put("author", "john");
        attachment.attributes().put("size", "100");

        ObjectMapper mapper = new ObjectMapper();

        String value = mapper.writeValueAsString(attachment);

        assertThat(value).isNotNull();

        Attachment object = mapper.readValue(value, Attachment.class);
        assertThat(object.type()).isEqualTo("application/pdf");
        assertThat(object.name()).isEqualTo("test.pdf");
        assertThat(object.attributes()).hasSize(2).containsEntry("size", "100").containsEntry("author", "john");
    }
}
