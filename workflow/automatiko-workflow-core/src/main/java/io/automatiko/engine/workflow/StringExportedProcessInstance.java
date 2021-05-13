package io.automatiko.engine.workflow;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.automatiko.engine.api.workflow.ExportedProcessInstance;

public class StringExportedProcessInstance extends ExportedProcessInstance<String> {

    private Function<String, List<Map<String, String>>> converter;

    protected StringExportedProcessInstance(String header, String instance, String timers,
            Function<String, List<Map<String, String>>> converter) {
        super(header, instance, timers);
        this.converter = converter;
    }

    public static StringExportedProcessInstance of(String header, String content, String timers,
            Function<String, List<Map<String, String>>> converter) {
        return new StringExportedProcessInstance(header, content, timers, converter);
    }

    @Override
    public List<Map<String, String>> convertTimers() {
        if (converter == null) {
            return Collections.emptyList();
        }
        return converter.apply(getTimers());
    }

    @Override
    public String toString() {
        return "[header=" + getHeader() + ", instance=" + getInstance()
                + ", timers=" + getTimers() + "]";
    }

    @Override
    public byte[] data() {
        return toString().getBytes(StandardCharsets.UTF_8);
    }
}
