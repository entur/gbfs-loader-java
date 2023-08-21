package org.entur.gbfs.authentication;

import java.util.HashMap;
import java.util.Map;

public class HttpHeadersRequestAuthenticator implements RequestAuthenticator {
    private final Map<String, String> headersToSet;

    public HttpHeadersRequestAuthenticator(Map<String, String> headers) {
        this.headersToSet = new HashMap(headers);
    }

    @Override
    public void authenticateRequest(Map<String, String> httpHeaders) throws RequestAuthenticationException {
        for (String key : headersToSet.keySet()) {
            httpHeaders.put(key, headersToSet.get(key));
        }
    }
}
