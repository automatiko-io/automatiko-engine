package io.automatiko.engine.workflow;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import io.automatiko.engine.api.workflow.HandledServiceExecutionError;
import io.automatiko.engine.api.workflow.workitem.WorkItemExecutionError;

public class ErrorMapper implements Function<Throwable, Throwable> {

    private Set<String> handledCodes = new HashSet<>();

    public ErrorMapper(String... codes) {
        Stream.of(codes).forEach(code -> handledCodes.add(code));
    }

    public Throwable apply(Throwable error) {

        if (error instanceof WorkItemExecutionError) {
            String errorCode = ((WorkItemExecutionError) error).getErrorCode();

            if (handledCodes.contains(errorCode)) {
                return new HandledServiceExecutionError((WorkItemExecutionError) error);
            }
        }

        return error;
    }

    public static RuntimeException wrap(RuntimeException e, String... codes) {
        if (e instanceof WorkItemExecutionError) {
            String errorCode = ((WorkItemExecutionError) e).getErrorCode();
            if (errorCode != null) {
                for (String code : codes) {

                    if (code.equals(errorCode)) {
                        return new HandledServiceExecutionError((WorkItemExecutionError) e);
                    }
                }
            }
        }

        return e;
    }
}