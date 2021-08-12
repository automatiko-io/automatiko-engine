package io.automatiko.engine.services.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailUtils {

    private static Pattern EXTRACT_PATTERN = Pattern.compile("<([\\S&&[^\\}]]+)>");

    public static String messageIdWithCorrelation(String correlation, String domain) {
        return "<" + correlation + "@" + domain + ">";
    }

    public static String correlationFromMessageId(String messageId) {
        String value = null;

        if (messageId != null) {

            Matcher matcher = EXTRACT_PATTERN.matcher(messageId);
            while (matcher.find()) {
                value = matcher.group(1).split("@")[0];

            }
        }

        return value;

    }
}
