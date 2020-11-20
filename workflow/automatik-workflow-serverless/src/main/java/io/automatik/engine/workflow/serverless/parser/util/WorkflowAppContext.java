
package io.automatik.engine.workflow.serverless.parser.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class WorkflowAppContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowAppContext.class);

    private static final String APP_PROPERTIES_FILE_NAME = "application.properties";
    private static final String DEFAULT_PROP_VALUE = "";

    private Properties applicationProperties;

    public static WorkflowAppContext ofAppResources() {
        Properties properties = new Properties();

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(APP_PROPERTIES_FILE_NAME)) {
            properties.load(is);
        } catch (Exception e) {
            LOGGER.debug("Unable to load {}", APP_PROPERTIES_FILE_NAME);
        }
        return new WorkflowAppContext(properties);
    }

    public static WorkflowAppContext ofProperties(Properties props) {
        return new WorkflowAppContext(props);
    }

    private WorkflowAppContext(Properties properties) {
        this.applicationProperties = properties;
    }

    public String getApplicationProperty(String key) {
        if (applicationProperties != null && applicationProperties.containsKey(key)) {
            return applicationProperties.getProperty(key);
        } else {
            return DEFAULT_PROP_VALUE;
        }
    }

    public Properties getApplicationProperties() {
        return this.applicationProperties;
    }

}
