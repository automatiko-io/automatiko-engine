package io.automatiko.engine.api.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.automatiko.engine.api.auth.IdentityProvider;
import io.automatiko.engine.api.auth.SecurityPolicy;

public class InstanceMetadata {

    private String id;
    private String businessKey;
    private String description;

    private int state;

    private Set<String> tags;

    private Set<Link> links;

    public InstanceMetadata() {
    }

    private InstanceMetadata(String id, String businessKey, String description, int state, Collection<String> tags,
            Set<Link> links) {
        this.id = id;
        this.businessKey = businessKey;
        this.description = description;
        this.state = state;
        this.tags = tags == null ? Collections.emptySet() : new HashSet<>(tags);
        this.links = links == null ? Collections.emptySet() : links;
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

    public Set<Link> getLinks() {
        return links;
    }

    public void setLinks(Set<Link> links) {
        this.links = links;
    }

    public static InstanceMetadata of(ProcessInstance<?> instance) {
        Set<Link> links = new HashSet<>();
        for (WorkItem task : instance.workItems(SecurityPolicy.of(IdentityProvider.get()))) {
            Link link = new Link("task", task.getName(), task.getReferenceId(), task.getFormLink());
            links.add(link);
        }

        return new InstanceMetadata(instance.id(), instance.businessKey(), instance.description(), instance.status(),
                instance.tags().values(), links);
    }

    public static class Link {
        private String type;

        private String name;

        private String url;

        private String form;

        public Link() {

        }

        public Link(String type, String name, String url, String formLink) {
            this.type = type;
            this.name = name;
            this.url = url;
            this.form = formLink;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getForm() {
            return form;
        }

        public void setForm(String form) {
            this.form = form;
        }

        @Override
        public String toString() {
            return "Link [type=" + type + ", name=" + name + ", url=" + url + "]";
        }

    }
}
