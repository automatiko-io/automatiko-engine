package io.automatiko.engine.addons.services.receiveemail;

import java.io.IOException;
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
public class EmailAttachmentInputConverter implements InputConverter<File> {

    @Override
    public File convert(Object input) {
        if (input instanceof MailMessage) {

            MailMessage mailMessage = (MailMessage) input;

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

                        return new io.automatiko.engine.addons.services.receiveemail.Attachment(filename, data);
                    } catch (TypeConversionException | IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        return null;
    }

}
