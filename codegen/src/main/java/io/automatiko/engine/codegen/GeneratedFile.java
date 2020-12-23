
package io.automatiko.engine.codegen;

import java.nio.charset.StandardCharsets;

public class GeneratedFile {

    public enum Type {
        APPLICATION(false),
        APPLICATION_SECTION(false),
        APPLICATION_CONFIG(false),
        PROCESS(false),
        PROCESS_INSTANCE(false),
        REST(true),
        JSON_MAPPER(false),
        RULE(false),
        DECLARED_TYPE(true),
        DTO(true),
        QUERY(true),
        MODEL(true),
        CLASS(false),
        MESSAGE_CONSUMER(false),
        MESSAGE_PRODUCER(false),
        RESOURCE(false),
        JSON_SCHEMA(true);

        private final boolean customizable;

        Type(boolean customizable) {
            this.customizable = customizable;
        }

        public boolean isCustomizable() {
            return customizable;
        }
    }

    private final String relativePath;
    private final byte[] contents;
    private final Type type;

    public GeneratedFile(Type type, String relativePath, String contents) {
        this(type, relativePath, contents.getBytes(StandardCharsets.UTF_8));
    }

    public GeneratedFile(Type type, String relativePath, byte[] contents) {
        this.type = type;
        this.relativePath = relativePath;
        this.contents = contents;
    }

    public String relativePath() {
        return relativePath;
    }

    public byte[] contents() {
        return contents;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GeneratedFile other = (GeneratedFile) obj;
        if (relativePath == null) {
            if (other.relativePath != null)
                return false;
        } else if (!relativePath.equals(other.relativePath))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GeneratedFile{" + "type=" + type + ", relativePath='" + relativePath + '\'' + '}';
    }
}
