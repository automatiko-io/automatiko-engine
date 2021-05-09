package io.automatiko.engine.addons.persistence.db;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;

public class JacksonObjectMarshallingStrategy
        extends io.automatiko.engine.addons.persistence.common.JacksonObjectMarshallingStrategy {

    @Override
    public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {
        if (object instanceof Collection) {
            return log(mapper.writeValueAsBytes(new ArrayList<>((Collection<?>) object)));
        }
        return log(mapper.writeValueAsBytes(object));
    }

}
