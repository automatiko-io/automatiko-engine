package org.acme;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmailMessage {

    private String from;
    private String to;
    private String subject;
    private String body;

    private Map<String, byte[]> attachments = new LinkedHashMap<String, byte[]>();

    public EmailMessage() {

    }

    public EmailMessage(String from, String to, String subject, String body) {
        super();
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, byte[]> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, byte[]> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "EmailMessage [from=" + from + ", to=" + to + ", subject=" + subject + ", body=" + body + ", attachments="
                + attachments.size() + "]";
    }

}
