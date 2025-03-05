package io.automatiko.engine.addons.services.receiveemail;

import org.apache.camel.component.mail.MailMessage;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.services.utils.EmailUtils;
import jakarta.mail.MessagingException;

public class ReceiveEmailFunctions implements Functions {

    public static String replyMessageId(Message<?> message) {
        org.apache.camel.Message camelMessage = ((io.smallrye.reactive.messaging.camel.CamelMessage<?>) message).getExchange()
                .getIn();

        MailMessage mailMesage = null;

        if (camelMessage instanceof MailMessage) {
            mailMesage = (MailMessage) camelMessage;
        } else {
            mailMesage = (MailMessage) camelMessage.copy();
        }

        String inReplyMessageId = mailMesage.getMessageId();
        try {
            if (mailMesage.getOriginalMessage().getHeader("In-Reply-To") != null) {
                inReplyMessageId = mailMesage.getOriginalMessage().getHeader("In-Reply-To")[0];
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return EmailUtils.correlationFromMessageId(inReplyMessageId);
    }
}
