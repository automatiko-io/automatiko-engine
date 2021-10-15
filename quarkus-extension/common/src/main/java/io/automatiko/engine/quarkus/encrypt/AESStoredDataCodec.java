package io.automatiko.engine.quarkus.encrypt;

import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.workflow.encrypt.StoredDataCodec;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "quarkus.automatiko.persistence.encryption", stringValue = "aes")
@ApplicationScoped
public class AESStoredDataCodec implements StoredDataCodec {

    private final Cipher encodeCipher;

    private final Cipher decodeCipher;

    public AESStoredDataCodec(@ConfigProperty(name = "automatiko.encryption.aes.key") String key) {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");

            encodeCipher = Cipher.getInstance("AES");
            decodeCipher = Cipher.getInstance("AES");

            encodeCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            decodeCipher.init(Cipher.DECRYPT_MODE, aesKey);
        } catch (Exception e) {
            throw new RuntimeException("Unable to intialize ciphers", e);
        }
    }

    @Override
    public byte[] encode(byte[] data) {
        try {
            return encodeCipher.doFinal(data);

        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Unable to use AES encryption to encode data", e);
        }
    }

    @Override
    public byte[] decode(byte[] data) {
        try {
            return decodeCipher.doFinal(data);

        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException("Unable to use AES encryption to decode data", e);
        }
    }

}
