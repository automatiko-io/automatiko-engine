package io.automatiko.engine.api.auth;

/**
 * Access denied exception is thrown when<code>AccessPolicy</code> has been violated
 */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = -7845918881133007279L;

    public AccessDeniedException(String message) {
        super(message);
    }

}
