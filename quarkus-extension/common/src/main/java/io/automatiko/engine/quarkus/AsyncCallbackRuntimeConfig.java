package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface AsyncCallbackRuntimeConfig {

    /**
     * Specifies authorization type to be used when callbacks are invoked
     */
    Optional<String> authType();

    /**
     * Specifies basic authorization string, expected user name and password encrypted with Base64 but without
     * <code>Basic </code> prefix
     */
    Optional<String> authBasic();

    /**
     * Specifies user name to be used for basic authentication
     */
    Optional<String> authUser();

    /**
     * Specifies password to be used for basic authentication
     */
    Optional<String> authPassword();

    /**
     * Specifies complete access token to be used as bearer token on the callback call
     */
    Optional<String> authAccessToken();

    /**
     * Specifies client id to be used to obtain OAuth token
     */
    Optional<String> authClientId();

    /**
     * Specifies client secret to be used to obtain OAuth token
     */
    Optional<String> authClientSecret();

    /**
     * Specifies refresh token to be used to automatically refresh access token
     */
    Optional<String> authRefreshToken();

    /**
     * Specifies refresh token endpoint
     */
    Optional<String> authRefreshUrl();

    /**
     * Specifies scopes to be set when obtaining token
     */
    Optional<String> authScope();

    /**
     * Specifies name of HTTP header to be set on the callback call
     */
    Optional<String> authCustomName();

    /**
     * Specifies custom value to be set on the callback call
     */
    Optional<String> authCustomValue();

    /**
     * Specifies name of the header to be taken from request headers that acts like the "on behalf" information
     */
    Optional<String> authOnBehalfName();
}
