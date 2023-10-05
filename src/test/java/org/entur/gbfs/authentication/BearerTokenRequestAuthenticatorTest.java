package org.entur.gbfs.authentication;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BearerTokenRequestAuthenticatorTest {

  @Test
  void testBearerTokenRequestAuthenticator() {
    String FAKE_TOKEN = "fake_token";

    RequestAuthenticator requestAuthenticator = new BearerTokenRequestAuthenticator(
      FAKE_TOKEN
    );

    Map<String, String> headers = new HashMap<>();

    requestAuthenticator.authenticateRequest(headers);

    Assertions.assertEquals("Bearer " + FAKE_TOKEN, headers.get("Authorization"));
  }
}
