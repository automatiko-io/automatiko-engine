package io.automatiko.engine.services.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EmailUtilsTest {

    @Test
    public void testCorrelationToMessageRoundTrip() {
        String id = EmailUtils.messageIdWithCorrelation("12345", "smtp.gmail.com");
        assertEquals("<12345@smtp.gmail.com>", id);

        String c = EmailUtils.correlationFromMessageId(id);
        assertEquals("12345", c);
    }

}
