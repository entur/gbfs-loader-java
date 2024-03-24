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
import java.util.Map;
import org.entur.gbfs.authentication.RequestAuthenticator;

/**
 * Options for a Gbfs Subscription
 */
public record GbfsSubscriptionOptions(
  URI discoveryURI,
  String languageCode,
  Long minimumTtl,
  Map<String, String> headers,
  RequestAuthenticator requestAuthenticator,
  Long timeout,
  Boolean enableValidation
) {}
