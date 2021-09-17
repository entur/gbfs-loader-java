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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GBFSLoader {
    private final Map<String, GBFSSubscriber> subscribers = new HashMap<>();

    /**
     * Start a subscription on a GBFS feed delivery
     *
     * @param options Options
     * @param consumer A consumer that will handle receiving updates from the loader
     * @return A string identifier
     */
    public String register(GBFSLoaderOptions options, Consumer<GBFSFeedDelivery> consumer) {
        String id = UUID.randomUUID().toString();
        var subscriber = new GBFSSubscriber(options, consumer);
        subscribers.put(id, subscriber);
        subscriber.start();
        return id;
    }

    public void update() {
        subscribers.values().forEach(GBFSSubscriber::update);
    }

    /**
     * Stop a subscription on a GBFS feed delivery
     *
     * @param identifier An identifier returned by register method.
     */
    public void deregister(String identifier) {
        subscribers.remove(identifier);
    }
}
