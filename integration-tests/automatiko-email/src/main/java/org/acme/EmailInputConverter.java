package org.acme;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.enterprise.context.ApplicationScoped;
import javax.mail.Address;
import javax.mail.MessagingException;

import org.apache.camel.TypeConversionException;
import org.apache.camel.attachment.Attachment;
import org.apache.camel.component.mail.MailMessage;

import io.automatiko.engine.api.io.InputConverter;

@ApplicationScoped
public class EmailInputConverter implements InputConverter<EmailMessage> {

    @SuppressWarnings("unchecked")
    @Override
    public EmailMessage convert(Object input) {
        if (input instanceof MailMessage) {

            MailMessage mailMessage = (MailMessage) input;

            try {
                EmailMessage email = new EmailMessage(toSimpleAddress(mailMessage.getOriginalMessage().getFrom()),
                        toSimpleAddress(mailMessage.getOriginalMessage().getAllRecipients()),
                        mailMessage.getOriginalMessage().getSubject(),
                        mailMessage.getBody(String.class));

                Map<String, Attachment> attachments = mailMessage.getExchange().getProperty("CamelAttachmentObjects",
                        Map.class);
                if (attachments != null && attachments.size() > 0) {
                    for (String name : attachments.keySet()) {
                        DataHandler dh = attachments.get(name).getDataHandler();
                        // get the file name
                        String filename = dh.getName();

                        // get the content and convert it to byte[]

                        try {
                            byte[] data = mailMessage.getExchange().getContext().getTypeConverter()
                                    .convertTo(byte[].class, dh.getInputStream());

                            email.getAttachments().put(filename, data);
                        } catch (TypeConversionException | IOException e) {
                            e.printStackTrace();
                        }

                    }
                }

                return email;
            } catch (MessagingException e) {
                throw new RuntimeException("Error while converting mail message", e);
            }

        }
        return null;
    }

    protected String toSimpleAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }

        return Stream.of(addresses).map(a -> a.toString()).collect(Collectors.joining(","));
    }
}
