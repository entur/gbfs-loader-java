/*
 *
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.gbfs;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.entur.gbfs.authentication.RequestAuthenticator;

/**
 * Options for a Gbfs Subscription
 */
public class GbfsSubscriptionOptions {

  private URI discoveryURI;
  private String languageCode;
  private long minimumTtl;
  private Map<String, String> headers = new HashMap<>();
  private RequestAuthenticator requestAuthenticator;
  private Long timeout;
  private boolean enableValidation;

  public URI getDiscoveryURI() {
    return discoveryURI;
  }

  public void setDiscoveryURI(URI discoveryURI) {
    this.discoveryURI = discoveryURI;
  }

  public String getLanguageCode() {
    return languageCode;
  }

  public void setLanguageCode(String languageCode) {
    this.languageCode = languageCode;
  }

  public long getMinimumTtl() {
    return minimumTtl;
  }

  public void setMinimumTtl(long minimumTtl) {
    this.minimumTtl = minimumTtl;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public RequestAuthenticator getRequestAuthenticator() {
    return requestAuthenticator;
  }

  public void setRequestAuthenticator(RequestAuthenticator requestAuthenticator) {
    this.requestAuthenticator = requestAuthenticator;
  }

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }

  public boolean isEnableValidation() {
    return enableValidation;
  }

  public void setEnableValidation(boolean enableValidation) {
    this.enableValidation = enableValidation;
  }
}
