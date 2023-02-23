package io.automatiko.engine.quarkus.encrypt;

import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;

import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "quarkus.automatiko.persistence.encryption", stringValue = "base64")
@ApplicationScoped
public class Base64StoredDataCodec implements StoredDataCodec {

    @Override
    public byte[] encode(byte[] data) {
        return Base64.getEncoder().encode(data);
    }

    @Override
    public byte[] decode(byte[] data) {
        return Base64.getDecoder().decode(data);
    }

}
