package io.automatiko.engine.addons.services.receiveemail;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class EmailMessage {

    private String from;

    private String replyTo;

    private List<String> to;

    private List<String> cc;

    private List<String> bcc;

    private String subject;

    private String body;

    private List<Attachment> attachments;

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
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

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(Attachment attachment) {
        if (this.attachments == null) {
            this.attachments = new ArrayList<>();
        }
        this.attachments.add(attachment);
    }

    @Override
    public String toString() {
        return "EmailMessage [from=" + from + ", replyTo=" + replyTo + ", to=" + to + ", cc=" + cc + ", bcc=" + bcc
                + ", subject=" + subject + ", body=" + body + ", attachments=" + attachments + "]";
    }

}
