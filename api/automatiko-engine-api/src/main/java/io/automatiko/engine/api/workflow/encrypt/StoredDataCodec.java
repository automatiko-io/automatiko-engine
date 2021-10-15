package io.automatiko.engine.api.workflow.encrypt;

public interface StoredDataCodec {

    static final StoredDataCodec NO_OP_CODEC = new NoOpStoredDataCodec();

    /**
     * Performs encoding of given data
     * 
     * @param data data to be encoded
     * @return returns data encoded based on internal implementation
     */
    byte[] encode(byte[] data);

    /**
     * Performs decoding of given data (that was previously encoded with this codec)
     * 
     * @param data data to be decoded
     * @return returns data decoded based on internal implementation
     */
    byte[] decode(byte[] data);

    class NoOpStoredDataCodec implements StoredDataCodec {

        @Override
        public byte[] encode(byte[] data) {
            return data;
        }

        @Override
        public byte[] decode(byte[] data) {
            return data;
        }

    }
}
