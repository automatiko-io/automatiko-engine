package io.automatiko.engine.addons.services.receiveemail;

import javax.mail.MessagingException;

import org.apache.camel.component.mail.MailMessage;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.automatiko.engine.api.Functions;
import io.automatiko.engine.services.utils.EmailUtils;

public class ReceiveEmailFunctions implements Functions {

    public static String replyMessageId(Message<?> message) {
        MailMessage mailMesage = (MailMessage) ((io.smallrye.reactive.messaging.camel.CamelMessage<?>) message).getExchange()
                .getIn();

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
