package org.entur.gbfs.authentication;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@WireMockTest
class Oauth2ClientCredentialsGrantRequestAuthenticatorTest {

  @Test
  void testOauth2ClientCredentialsGrantRequestAuthenticator(
    WireMockRuntimeInfo runtimeInfo
  ) throws InterruptedException {
    String TEST_TOKEN_URL = "http://localhost:" + runtimeInfo.getHttpPort() + "/token";
    String TEST_USER = "foo";
    String TEST_PASSWORD = "bar";

    stubFor(
      post("/token")
        .withRequestBody(equalTo("grant_type=client_credentials&scope=test-scope"))
        .withBasicAuth(TEST_USER, TEST_PASSWORD)
        .willReturn(okJson("{\"access_token\":\"fake_token\", \"expires_in\":3600}"))
    );

    Oauth2ClientCredentialsGrantRequestAuthenticator authenticator =
      new Oauth2ClientCredentialsGrantRequestAuthenticator(
        URI.create(TEST_TOKEN_URL),
        TEST_USER,
        TEST_PASSWORD,
        "test-scope"
      );

    CountDownLatch latch = new CountDownLatch(2);

    Thread thread1 = new Thread(() -> assertAuthenticateRequest(authenticator, latch));
    Thread thread2 = new Thread(() -> assertAuthenticateRequest(authenticator, latch));
    thread1.start();
    thread2.start();

    latch.await();

    verify(exactly(1), postRequestedFor(urlEqualTo("/token")));
  }

  private void assertAuthenticateRequest(
    RequestAuthenticator authenticator,
    CountDownLatch latch
  ) {
    try {
      Map<String, String> headers = new HashMap<>();
      authenticator.authenticateRequest(headers);
      Assertions.assertEquals("Bearer fake_token", headers.get("Authorization"));
    } catch (RequestAuthenticationException e) {
      Assertions.fail(e.getMessage());
    } finally {
      latch.countDown();
    }
  }
}
