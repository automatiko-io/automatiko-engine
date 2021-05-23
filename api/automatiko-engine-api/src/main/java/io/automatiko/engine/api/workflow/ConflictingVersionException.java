package io.automatiko.engine.api.workflow;

/**
 * An exception that is thrown when a conflict between versions of the instance
 * has been identified. Usually it refers to concurrent updates of the same instance
 * and thus to avoid overrides a conflict is flagged.
 *
 */
public class ConflictingVersionException extends RuntimeException {

    private static final long serialVersionUID = 2118235923758857689L;

    public ConflictingVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictingVersionException(String message) {
        super(message);
    }

    public ConflictingVersionException(Throwable cause) {
        super(cause);
    }

}
