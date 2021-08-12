package io.automatiko.engine.addons.services.receiveemail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.TypeConversionException;
import org.apache.camel.attachment.Attachment;
import org.apache.camel.component.mail.MailMessage;

import io.automatiko.engine.api.io.InputConverter;
import io.automatiko.engine.api.workflow.files.File;

@SuppressWarnings({ "unchecked", "rawtypes" })
@ApplicationScoped
public class EmailAttachmentsInputConverter implements InputConverter<List<File>> {

    @Override
    public List<File> convert(Object input) {
        List<File> files = new ArrayList<File>();
        if (input instanceof MailMessage) {

            MailMessage mailMessage = (MailMessage) input;

            mailMessage.getMessageId();

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

                        files.add(new io.automatiko.engine.addons.services.receiveemail.Attachment(filename, data));
                    } catch (TypeConversionException | IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        return files;
    }

}
