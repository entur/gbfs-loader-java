package org.entur.gbfs.authentication;

import java.net.URI;
import java.util.Map;
import org.dmfs.httpessentials.client.HttpRequestExecutor;
import org.dmfs.httpessentials.httpurlconnection.HttpUrlConnectionExecutor;
import org.dmfs.oauth2.client.*;
import org.dmfs.oauth2.client.grants.ClientCredentialsGrant;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.oauth2.client.scope.EmptyScope;
import org.dmfs.rfc3986.uris.EmptyUri;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;

public class Oauth2ClientCredentialsGrantRequestAuthenticator
  implements RequestAuthenticator {

  private final HttpRequestExecutor executor = new HttpUrlConnectionExecutor();
  private final OAuth2Client client;
  private final OAuth2Scope scope;
  private OAuth2AccessToken token;

  public Oauth2ClientCredentialsGrantRequestAuthenticator(
    URI tokenUrl,
    String clientId,
    String clientPassword
  ) {
    this(tokenUrl, clientId, clientPassword, null);
  }

  public Oauth2ClientCredentialsGrantRequestAuthenticator(
    URI tokenUrl,
    String clientId,
    String clientPassword,
    String scope
  ) {
    OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
      null,
      tokenUrl,
      new Duration(1, 0, 3600)
    );

    OAuth2ClientCredentials credentials = new BasicOAuth2ClientCredentials(
      clientId,
      clientPassword
    );

    client = new BasicOAuth2Client(provider, credentials, EmptyUri.INSTANCE);

    this.scope = scope == null ? EmptyScope.INSTANCE : new BasicScope(scope);
  }

  @Override
  public void authenticateRequest(Map<String, String> httpHeaders)
    throws RequestAuthenticationException {
    try {
      if (token == null || token.expirationDate().after(DateTime.now())) {
        token = new ClientCredentialsGrant(client, scope).accessToken(executor);
      }
      httpHeaders.put("Authorization", String.format("Bearer %s", token.accessToken()));
    } catch (Exception e) {
      throw new RequestAuthenticationException(e);
    }
  }
}
