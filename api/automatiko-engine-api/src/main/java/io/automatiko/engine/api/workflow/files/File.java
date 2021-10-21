package io.automatiko.engine.api.workflow.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public interface File<T> {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * Returns name of the file including extension if defined
     * 
     * @return full file name
     */
    String name();

    /**
     * Returns type of the file that defines how to handle the content.
     * It's expected to return MIME type based on the actual content of the file.
     * 
     * @return MIME type that represents this file
     */
    default String type() {
        return discoverType(name());
    }

    /**
     * The content of the file.
     * 
     * @return complete content of the file
     */
    T content();

    /**
     * Returns map of attributes associated with the file. Additional attributes can be set at
     * any time such as last modification date, etc
     * 
     * @return non null map of attributes
     */
    Map<String, String> attributes();

    /**
     * Returns url representing this file based on the type and content
     * 
     * @return url of the file
     */
    String url();

    public static String discoverType(String name) {
        try {
            String contentType = Files.probeContentType(Paths.get(name));
            if (contentType == null) {
                contentType = DEFAULT_CONTENT_TYPE;
            }
            return contentType;
        } catch (IOException e) {
            return DEFAULT_CONTENT_TYPE;
        }
    }
}
