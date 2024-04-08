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
import org.jetbrains.annotations.Nullable;

/**
 * Options for a Gbfs Subscription
 * @param discoveryURI The URI of the GBFS discovery file (gbfs.json)
 * @param languageCode The language code used as a key to look up GBFS files
 *                     in gbfs.json. For v3.x feeds this can be null, but is
 *                     required for v2.x feeds
 * @param minimumTtl Override GBFS files' ttl values with a minimum value.
 * @param headers Headers that will be added to http requests when fetching
 *                GBFS files
 * @param requestAuthenticator An instance of {@link org.entur.gbfs.authentication.RequestAuthenticator}
 *                             for authentication of http requests for GBFS files.
 * @param timeout Custom timeout value for http requests
 * @param enableValidation Will perform validation on all fetched data. The validation
 *                         result can be read in the consumer
 */
public record GbfsSubscriptionOptions(
  URI discoveryURI,
  @Nullable String languageCode,
  @Nullable Long minimumTtl,
  @Nullable Map<String, String> headers,
  @Nullable RequestAuthenticator requestAuthenticator,
  @Nullable Long timeout,
  @Nullable Boolean enableValidation
) {}
