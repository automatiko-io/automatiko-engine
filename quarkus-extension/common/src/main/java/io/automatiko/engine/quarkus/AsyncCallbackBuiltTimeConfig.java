package io.automatiko.engine.quarkus;

import java.util.Optional;

import io.automatiko.engine.api.config.AsyncCallbackConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AsyncCallbackBuiltTimeConfig extends AsyncCallbackConfig {

    /**
     * Specifies authorization type to be used when callbacks are invoked
     */
    @ConfigItem
    public Optional<String> authType;

    /**
     * Specifies basic authorization string, expected user name and password encrypted with Base64 but without
     * <code>Basic </code> prefix
     */
    @ConfigItem
    public Optional<String> authBasic;

    /**
     * Specifies user name to be used for basic authentication
     */
    @ConfigItem
    public Optional<String> authUser;

    /**
     * Specifies password to be used for basic authentication
     */
    @ConfigItem
    public Optional<String> authPassword;

    /**
     * Specifies complete access token to be used as bearer token on the callback call
     */
    @ConfigItem
    public Optional<String> authAccessToken;

    /**
     * Specifies client id to be used to obtain OAuth token
     */
    @ConfigItem
    public Optional<String> authClientId;

    /**
     * Specifies client secret to be used to obtain OAuth token
     */
    @ConfigItem
    public Optional<String> authClientSecret;

    /**
     * Specifies refresh token to be used to automatically refresh access token
     */
    @ConfigItem
    public Optional<String> authRefreshToken;

    /**
     * Specifies refresh token endpoint
     */
    @ConfigItem
    public Optional<String> authRefreshUrl;

    /**
     * Specifies scopes to be set when obtaining token
     */
    @ConfigItem
    public Optional<String> authScope;

    /**
     * Specifies name of HTTP header to be set on the callback call
     */
    @ConfigItem
    public Optional<String> authCustomName;

    /**
     * Specifies custom value to be set on the callback call
     */
    @ConfigItem
    public Optional<String> authCustomValue;

    /**
     * Specifies name of the header to be taken from request headers that acts like the "on behalf" information
     */
    @ConfigItem
    public Optional<String> authOnBehalfName;

    @Override
    public Optional<String> authType() {

        return authType;
    }

    @Override
    public Optional<String> authBasic() {

        return authBasic;
    }

    @Override
    public Optional<String> authUser() {

        return authUser;
    }

    @Override
    public Optional<String> authPassword() {

        return authPassword;
    }

    @Override
    public Optional<String> authAccessToken() {

        return authAccessToken;
    }

    @Override
    public Optional<String> authClientId() {

        return authClientId;
    }

    @Override
    public Optional<String> authClientSecret() {

        return authClientSecret;
    }

    @Override
    public Optional<String> authRefreshToken() {

        return authRefreshToken;
    }

    @Override
    public Optional<String> authRefreshUrl() {

        return authRefreshUrl;
    }

    @Override
    public Optional<String> authScope() {

        return authScope;
    }

    @Override
    public Optional<String> authCustomName() {

        return authCustomName;
    }

    @Override
    public Optional<String> authCustomValue() {

        return authCustomValue;
    }

    @Override
    public Optional<String> authOnBehalfName() {

        return authOnBehalfName;
    }
}
