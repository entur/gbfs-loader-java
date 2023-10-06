package org.entur.gbfs.authentication;

import java.util.Map;

public class DummyRequestAuthenticator implements RequestAuthenticator {

  @Override
  public void authenticateRequest(Map<String, String> httpHeaders) {
    // Does not add any authentication to headers
  }
}
