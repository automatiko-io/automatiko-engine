package io.automatiko.engine.addons.services.receiveemail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;

import org.apache.camel.TypeConversionException;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.component.mail.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.automatiko.engine.api.io.InputConverter;

@SuppressWarnings({ "unchecked" })
@ApplicationScoped
public class EmailInputConverter implements InputConverter<EmailMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailInputConverter.class);

    @Override
    public EmailMessage convert(Object input) {
        if (input instanceof MailMessage) {

            MailMessage mailMessage = (MailMessage) input;
            try {
                EmailMessage email = new EmailMessage();
                email.setFrom(toStringAddress(mailMessage.getOriginalMessage().getFrom()));
                email.setReplyTo(toStringAddress(mailMessage.getOriginalMessage().getReplyTo()));
                email.setTo(toSimpleAddress(mailMessage.getOriginalMessage().getRecipients(RecipientType.TO)));
                email.setCc(toSimpleAddress(mailMessage.getOriginalMessage().getRecipients(RecipientType.CC)));
                email.setBcc(toSimpleAddress(mailMessage.getOriginalMessage().getRecipients(RecipientType.BCC)));
                email.setSubject(mailMessage.getOriginalMessage().getSubject());
                email.setBody(mailMessage.getBody(String.class));

                AttachmentMessage attachmentMessage = mailMessage.getExchange().getMessage(AttachmentMessage.class);
                if (attachmentMessage != null) {
                    Map<String, DataHandler> attachments = attachmentMessage.getAttachments();
                    if (attachments != null && attachments.size() > 0) {
                        for (String name : attachments.keySet()) {
                            DataHandler dh = attachments.get(name);
                            // get the file name
                            String filename = dh.getName();

                            // get the content and convert it to byte[]

                            try {
                                byte[] data = mailMessage.getExchange().getContext().getTypeConverter()
                                        .convertTo(byte[].class, dh.getInputStream());

                                email.addAttachment(
                                        new io.automatiko.engine.addons.services.receiveemail.Attachment(filename, data));
                            } catch (TypeConversionException | IOException e) {
                                LOGGER.warn("Unexpected excheption while reading email attachment {}", filename, e);
                            }

                        }
                    }
                }

                return email;

            } catch (Exception e) {
                LOGGER.warn("Unexpected excheption while reading email message {}", e);
            }
        }
        return null;
    }

    protected List<String> toSimpleAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return Collections.emptyList();
        }

        return Stream.of(addresses).map(a -> a.toString()).collect(Collectors.toList());
    }

    protected String toStringAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }

        return addresses[0].toString();
    }

}
