package io.automatiko.engine.quarkus.functionflow.gcp;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import io.automatiko.engine.api.Model;

/**
 * A dedicated JSON deserializer for pub sub of Google Cloud Platform e.g. Cloud Run
 *
 */
public abstract class PubSubModelDeserializer extends StdDeserializer<Model> {

    private static final long serialVersionUID = -6018654702544218753L;
    private ObjectMapper mapper = new ObjectMapper().setAnnotationIntrospector(IGNORE_DESERIALIZE_ANNOTATIONS);

    public PubSubModelDeserializer() {
        this(null);
    }

    public PubSubModelDeserializer(Class<?> vc) {
        super(vc);
    }

    protected abstract Class<? extends Model> type() throws Exception;

    @Override
    public Model deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.has("message")) {
            String data = node.get("message").get("data").asText();

            byte[] value = Base64.getDecoder().decode(data);

            try {
                Model d = (Model) mapper.readValue(value, type());

                return d;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                Model d = (Model) mapper.convertValue(node, type());

                return d;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final JacksonAnnotationIntrospector IGNORE_DESERIALIZE_ANNOTATIONS = new JacksonAnnotationIntrospector() {

        private static final long serialVersionUID = -6870427120397562167L;

        @Override
        protected <A extends Annotation> A _findAnnotation(final Annotated annotated, final Class<A> annoClass) {
            if (!annotated.hasAnnotation(JsonDeserialize.class)) {
                return super._findAnnotation(annotated, annoClass);
            }
            return null;
        }
    };

}
