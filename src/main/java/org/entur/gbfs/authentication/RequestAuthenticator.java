package org.entur.gbfs.authentication;

import java.util.Map;

/**
 * A RequestAuthenticator can be passed to the GbfsLoader to add authentication
 * details to http headers. A dummy implementation is used by default, which adds no
 * authentication.
 *
 * Implementations provided:
 * @see Oauth2ClientCredentialsGrantRequestAuthenticator
 * @see BearerTokenRequestAuthenticator
 *
 * You may create your own implementation for custom authentication schemes
 */
public interface RequestAuthenticator {

    /**
     * This method is called before each request is made
     *
     * @param httpHeaders The configured http headers used by the loader
     */
    void authenticateRequest(Map<String, String> httpHeaders) throws RequestAuthenticationException;
}
