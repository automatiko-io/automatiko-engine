package io.automatiko.engine.api.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InstanceMetadata {

    private String id;
    private String businessKey;
    private String description;

    private int state;

    private Set<String> tags;

    public InstanceMetadata() {
    }

    private InstanceMetadata(String id, String businessKey, String description, int state, Collection<String> tags) {
        this.id = id;
        this.businessKey = businessKey;
        this.description = description;
        this.state = state;
        this.tags = tags == null ? Collections.emptySet() : new HashSet<>(tags);
    }

    public String getId() {
        return id;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getDescription() {
        return description;
    }

    public int getState() {
        return state;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public static InstanceMetadata of(ProcessInstance<?> instance) {
        return new InstanceMetadata(instance.id(), instance.businessKey(), instance.description(), instance.status(),
                instance.tags().values());
    }
}
