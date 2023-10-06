package org.entur.gbfs.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@WireMockTest
class Oauth2ClientCredentialsGrantRequestAuthenticatorTest {

  @Test
  void testOauth2ClientCredentialsGrantRequestAuthenticator(
    WireMockRuntimeInfo runtimeInfo
  ) {
    String TEST_TOKEN_URL = "http://localhost:" + runtimeInfo.getHttpPort() + "/token";
    stubFor(post("/token").willReturn(okJson("{\"access_token\":\"fake_token\"}")));
    Oauth2ClientCredentialsGrantRequestAuthenticator authenticator =
      new Oauth2ClientCredentialsGrantRequestAuthenticator(
        URI.create(TEST_TOKEN_URL),
        "foo",
        "bar"
      );
    Map<String, String> headers = new HashMap<>();
    authenticator.authenticateRequest(headers);
    Assertions.assertEquals("Bearer fake_token", headers.get("Authorization"));
  }
}
