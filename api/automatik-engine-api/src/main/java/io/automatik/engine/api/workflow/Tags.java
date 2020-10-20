package io.automatik.engine.api.workflow;

import java.util.Collection;

public interface Tags {

    /**
     * Returns current collection of tag values.
     * 
     * @return collection of tag values
     */
    Collection<String> values();

    /**
     * Adds new tag
     * 
     * @param tag new tag to be added
     */
    void add(String value);

    /**
     * Returns current collection of tags.
     * 
     * @return collection of tags
     */
    Collection<Tag> get();

    /**
     * Retrieves tag with given id or null if no tag with given id exists
     * 
     * @param id identifier of the tag
     * @return tag associated with given id or null
     */
    Tag get(String id);

    /**
     * Removes the tag associated with given id
     * 
     * @param id identifier of the tag
     */
    boolean remove(String id);
}
