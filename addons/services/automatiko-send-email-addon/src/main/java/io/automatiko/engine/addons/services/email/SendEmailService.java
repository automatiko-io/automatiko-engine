package io.automatiko.engine.addons.services.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.automatiko.engine.api.workflow.ServiceExecutionError;
import io.automatiko.engine.api.workflow.files.File;
import io.automatiko.engine.services.utils.EmailUtils;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

@ApplicationScoped
@SuppressWarnings("unchecked")
public class SendEmailService {

    private Mailer mailer;
    private Engine engine;

    private Optional<String> host;

    public SendEmailService() {

    }

    @Inject
    public SendEmailService(Mailer mailer, Engine engine, @ConfigProperty(name = "quarkus.mailer.host") Optional<String> host) {
        this.mailer = mailer;
        this.engine = engine;
        this.host = host;
    }

    /**
     * Sends email to the given list of recipients with given subject and body.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param tos comma separated list of recipients
     * @param subject subject of the email
     * @param body body of the email
     * @param attachments optional attachments
     */

    public void sendSimple(String tos, String subject, String body, File<byte[]>... attachments) {
        try {
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, body);

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients with given subject and body.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param correlation correlation information to be attached to the message
     * @param tos comma separated list of recipients
     * @param subject subject of the email
     * @param body body of the email
     * @param attachments optional attachments
     */

    public void sendSimpleCorrelated(String correlation, String tos, String subject, String body, File<byte[]>... attachments) {
        try {
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, body);

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mail.addHeader("Message-ID", EmailUtils.messageIdWithCorrelation(correlation, host.orElse("localhost")));

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param tosc comma separated list of recipients
     * @param ccs comma separated list of CC recipients
     * @param subject subject of the email
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendSimpleWithCC(String tos, String ccs, String subject, String body, File<byte[]>... attachments) {
        try {
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, body);

                for (String cc : ccs.split(",")) {
                    mail.addCc(cc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param tosc comma separated list of recipients
     * @param bccs comma separated list of BCC recipients
     * @param subject subject of the email
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendSimpleWithBCC(String tos, String bccs, String subject, String body, File<byte[]>... attachments) {
        try {
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, body);

                for (String bcc : bccs.split(",")) {
                    mail.addBcc(bcc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param correlation correlation information to be attached to the message
     * @param tosc comma separated list of recipients
     * @param ccs comma separated list of CC recipients
     * @param subject subject of the email
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendSimpleCorrelatedWithCC(String correlation, String tos, String ccs, String subject, String body,
            File<byte[]>... attachments) {
        try {
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, body);

                for (String cc : ccs.split(",")) {
                    mail.addCc(cc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mail.addHeader("Message-ID", EmailUtils.messageIdWithCorrelation(correlation, host.orElse("localhost")));

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * @param correlation correlation information to be attached to the message
     * @param tosc comma separated list of recipients
     * @param bccs comma separated list of BCC recipients
     * @param subject subject of the email
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendSimpleCorrelatedWithBCC(String correlation, String tos, String bccs, String subject, String body,
            File<byte[]>... attachments) {
        try {
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, body);

                for (String bcc : bccs.split(",")) {
                    mail.addBcc(bcc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mail.addHeader("Message-ID", EmailUtils.messageIdWithCorrelation(correlation, host.orElse("localhost")));

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients with given subject and body that is created based on the given template.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * In case template cannot be found with given name a <code>ServiceExecutionError</code> will
     * be thrown with error code set to <code>emailTemplateNotFound</code>
     * 
     * @param tos comma separated list of recipients
     * @param subject subject of the email
     * @param templateName name of the email template to be used to create the body of the email message
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void send(String tos, String subject, String templateName, Object body, File<byte[]>... attachments) {
        Template template = getTemplate(templateName);
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            String content = template.instance().data(templateData).render();
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, content);
                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }
                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients with given subject and body that is created based on the given template.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * In case template cannot be found with given name a <code>ServiceExecutionError</code> will
     * be thrown with error code set to <code>emailTemplateNotFound</code>
     * 
     * @param correlation correlation information to be attached to the message
     * @param tos comma separated list of recipients
     * @param subject subject of the email
     * @param templateName name of the email template to be used to create the body of the email message
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendCorrelated(String correlation, String tos, String subject, String templateName, Object body,
            File<byte[]>... attachments) {
        Template template = getTemplate(templateName);
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            String content = template.instance().data(templateData).render();
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, content);
                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mail.addHeader("Message-ID", EmailUtils.messageIdWithCorrelation(correlation, host.orElse("localhost")));

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body that is created based on the
     * given template.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * In case template cannot be found with given name a <code>ServiceExecutionError</code> will
     * be thrown with error code set to <code>emailTemplateNotFound</code>
     * 
     * @param tos comma separated list of recipients
     * @param ccs comma separated list of CC recipients
     * @param subject subject of the email
     * @param templateName name of the email template to be used to create the body of the email message
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendWithCC(String tos, String ccs, String subject, String templateName, Object body,
            File<byte[]>... attachments) {
        Template template = getTemplate(templateName);
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            String content = template.instance().data(templateData).render();
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, content);

                for (String cc : ccs.split(",")) {
                    mail.addCc(cc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }
                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body that is created based on the
     * given template.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * In case template cannot be found with given name a <code>ServiceExecutionError</code> will
     * be thrown with error code set to <code>emailTemplateNotFound</code>
     * 
     * @param tos comma separated list of recipients
     * @param bccs comma separated list of BCC recipients
     * @param subject subject of the email
     * @param templateName name of the email template to be used to create the body of the email message
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendWithBCC(String tos, String bccs, String subject, String templateName, Object body,
            File<byte[]>... attachments) {
        Template template = getTemplate(templateName);
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            String content = template.instance().data(templateData).render();
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, content);

                for (String bcc : bccs.split(",")) {
                    mail.addBcc(bcc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }
                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body that is created based on the
     * given template.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * In case template cannot be found with given name a <code>ServiceExecutionError</code> will
     * be thrown with error code set to <code>emailTemplateNotFound</code>
     * 
     * @param correlation correlation information to be attached to the message
     * @param tos comma separated list of recipients
     * @param ccs comma separated list of CC recipients
     * @param subject subject of the email
     * @param templateName name of the email template to be used to create the body of the email message
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendCorrelatedWithCC(String correlation, String tos, String ccs, String subject, String templateName,
            Object body,
            File<byte[]>... attachments) {
        Template template = getTemplate(templateName);
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            String content = template.instance().data(templateData).render();
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, content);

                for (String cc : ccs.split(",")) {
                    mail.addCc(cc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mail.addHeader("Message-ID", EmailUtils.messageIdWithCorrelation(correlation, host.orElse("localhost")));

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /**
     * Sends email to the given list of recipients and cc recipients with given subject and body that is created based on the
     * given template.
     * Optionally given attachments will be put on the message as well
     * 
     * In case of error a <code>ServiceExecutionError</code> will be thrown with error code set to <code>sendEmailFailure</code>
     * so it can be used within workflow definition to handle it
     * 
     * In case template cannot be found with given name a <code>ServiceExecutionError</code> will
     * be thrown with error code set to <code>emailTemplateNotFound</code>
     * 
     * @param correlation correlation information to be attached to the message
     * @param tos comma separated list of recipients
     * @param bccs comma separated list of BCC recipients
     * @param subject subject of the email
     * @param templateName name of the email template to be used to create the body of the email message
     * @param body body of the email
     * @param attachments optional attachments
     */
    public void sendCorrelatedWithBCC(String correlation, String tos, String bccs, String subject, String templateName,
            Object body,
            File<byte[]>... attachments) {
        Template template = getTemplate(templateName);
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("body", body);

            String content = template.instance().data(templateData).render();
            for (String to : tos.split(",")) {
                Mail mail = Mail.withHtml(to, subject, content);

                for (String bcc : bccs.split(",")) {
                    mail.addBcc(bcc);
                }

                for (File<byte[]> attachment : attachments) {
                    if (attachment == null) {
                        continue;
                    }
                    mail.addAttachment(attachment.name(), attachment.content(), attachment.type());
                }

                mail.addHeader("Message-ID", EmailUtils.messageIdWithCorrelation(correlation, host.orElse("localhost")));

                mailer.send(mail);
            }
        } catch (Exception e) {
            throw new ServiceExecutionError("sendEmailFailure", e.getMessage(), e);
        }
    }

    /*
     * Helper methods
     */

    protected Template getTemplate(String name) {
        Template template = engine.getTemplate(name);
        if (template == null) {
            throw new ServiceExecutionError("emailTemplateNotFound", "Email template '" + name + "' not found");
        }

        return template;
    }
}
