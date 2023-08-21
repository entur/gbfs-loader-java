package org.entur.gbfs.authentication;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class HttpHeadersRequestAuthenticatorTest {
    @Test
    void testHttpHeadersRequestAuthenticator() {
        String HTTP_HEADER_KEY = "x-api-key";
        String HTTP_HEADER_VALUE = "xyz";
        Map<String, String> FAKE_HEADERS = Collections.singletonMap(HTTP_HEADER_KEY, HTTP_HEADER_VALUE);

        RequestAuthenticator requestAuthenticator = new HttpHeadersRequestAuthenticator(FAKE_HEADERS);
        Map<String, String> headers = new HashMap<>();
        requestAuthenticator.authenticateRequest(headers);

        Assertions.assertEquals(HTTP_HEADER_VALUE, headers.get(HTTP_HEADER_KEY));
    }
}
