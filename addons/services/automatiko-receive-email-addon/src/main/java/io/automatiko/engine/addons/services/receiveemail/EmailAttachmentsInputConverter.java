package io.automatiko.engine.addons.services.receiveemail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Message;
import org.apache.camel.TypeConversionException;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.component.mail.MailMessage;

import io.automatiko.engine.api.io.InputConverter;
import io.automatiko.engine.workflow.file.ByteArrayFile;
import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.internet.MimeUtility;

@ApplicationScoped
public class EmailAttachmentsInputConverter implements InputConverter<List<ByteArrayFile>> {

    @Override
    public List<ByteArrayFile> convert(Object input) {
        List<ByteArrayFile> files = new ArrayList<ByteArrayFile>();
        MailMessage mailMessage = null;

        if (input instanceof MailMessage) {
            mailMessage = (MailMessage) input;
        } else {
            mailMessage = (MailMessage) ((Message) input).copy();
        }

        if (mailMessage instanceof MailMessage) {

            mailMessage.getMessageId();

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

                            files.add(new io.automatiko.engine.addons.services.receiveemail.Attachment(filename, data));
                        } catch (TypeConversionException | IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
        return files;
    }

}
