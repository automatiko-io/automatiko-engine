package io.automatiko.engine.addons.services.receiveemail;

import java.io.IOException;
import java.util.Map;

import org.apache.camel.TypeConversionException;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.component.mail.MailMessage;

import io.automatiko.engine.api.io.InputConverter;
import io.automatiko.engine.workflow.file.ByteArrayFile;
import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.internet.MimeUtility;

@ApplicationScoped
public class EmailAttachmentInputConverter implements InputConverter<ByteArrayFile> {

    @Override
    public ByteArrayFile convert(Object input) {
        if (input instanceof MailMessage) {

            MailMessage mailMessage = (MailMessage) input;

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
                            filename = MimeUtility.decodeText(filename);
                            byte[] data = mailMessage.getExchange().getContext().getTypeConverter()
                                    .convertTo(byte[].class, dh.getInputStream());

                            return new io.automatiko.engine.addons.services.receiveemail.Attachment(filename, data);
                        } catch (TypeConversionException | IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
        return null;
    }

}
