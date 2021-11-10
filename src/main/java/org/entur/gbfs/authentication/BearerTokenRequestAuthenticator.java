package org.entur.gbfs.authentication;

import java.util.Map;

public class BearerTokenRequestAuthenticator implements RequestAuthenticator {
    private final String accessToken;

    public BearerTokenRequestAuthenticator(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public void authenticateRequest(Map<String, String> httpHeaders) throws RequestAuthenticationException {
        httpHeaders.put("Authorization", String.format("Bearer %s", accessToken));
    }
}
