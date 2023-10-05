package org.entur.gbfs.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

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
    String TEST_USER = "foo";
    String TEST_PASSWORD = "bar";

    stubFor(
      post("/token")
        .withRequestBody(equalTo("grant_type=client_credentials&scope=test-scope"))
        .withBasicAuth(TEST_USER, TEST_PASSWORD)
        .willReturn(okJson("{\"access_token\":\"fake_token\"}"))
    );
    Oauth2ClientCredentialsGrantRequestAuthenticator authenticator =
      new Oauth2ClientCredentialsGrantRequestAuthenticator(
        URI.create(TEST_TOKEN_URL),
        TEST_USER,
        TEST_PASSWORD,
        "test-scope"
      );
    Map<String, String> headers = new HashMap<>();
    authenticator.authenticateRequest(headers);
    Assertions.assertEquals("Bearer fake_token", headers.get("Authorization"));
  }
}
