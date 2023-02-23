package io.automatiko.engine.quarkus.encrypt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class EncryptionSupport {

    @DefaultBean
    @Produces
    public StoredDataCodec noopCodec() {
        return StoredDataCodec.NO_OP_CODEC;
    }
}
