
package io.automatiko.engine.api.event;

import java.util.Map;

/**
 * Represents top level data event structure that can be emitted from within
 * running process, decision or rule.
 * 
 * It's main aim is to be transferred over the wire but the event itself is not
 * meant to do transformation to be "wire-friendly"
 * 
 * Main point of the event is to be compatible with cloud events specification
 * and thus comes with main fields that the spec defines.
 * 
 * Classes implementing can provide more information to be considered extensions
 * of the event - see cloud event extension elements.
 *
 * @param <T> type of the body of the event
 */
public interface DataEvent<T> {

    public static final String SPEC_VERSION = "1.0.1";

    /**
     * Returns specification version of the cloud event
     * 
     * @return specification version
     */
    String getSpecversion();

    /**
     * Returns unique id of the event
     * 
     * @return unique event id
     */
    String getId();

    /**
     * Returns type of the event this instance represents e.g. ProcessInstanceEvent
     * 
     * @return type of the event
     */
    String getType();

    /**
     * Returns source of the event that is in URI syntax
     * 
     * @return uri source
     */
    String getSource();

    /**
     * Returns returns time when the event was created
     * 
     * @return time of the event
     */
    String getTime();

    /**
     * Returns subject of the event
     * 
     * @return subject
     */
    String getSubject();

    /**
     * Returns actual body of the event
     * 
     * @return
     */
    T getData();

    /**
     * Returns all extension attributes of the event
     * 
     * @return extension attributes
     */
    Map<String, Object> getExtensions();

    /**
     * Stores extension attribute with given name and value
     * 
     * @param name name of the attribute
     * @param value value of the attribute
     */
    void addExtension(String name, Object value);

    /**
     * Returns extension attribute with given name
     * 
     * @param name name of the attribute
     * @return extension attribute value if exists otherwise null
     */
    Object getExtension(String name);
}
