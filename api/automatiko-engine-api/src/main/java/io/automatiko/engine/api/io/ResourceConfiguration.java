
package io.automatiko.engine.api.io;

import java.util.Properties;

public interface ResourceConfiguration {

    public Properties toProperties();

    public ResourceConfiguration fromProperties(Properties prop);

}
