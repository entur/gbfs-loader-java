package org.entur.gbfs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class GBFSSubscriptionTest {

    @Test
    void testLoader() throws URISyntaxException {
        GbfsSubscriptionManager loader = new GbfsSubscriptionManager();
        GbfsSubscriptionOptions options = new GbfsSubscriptionOptions();
        options.setDiscoveryURI(new URI("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json"));
        options.setLanguageCode("nb");
        String subscriber = loader.subscribe(options, delivery -> {
            Assertions.assertEquals(6, delivery.getStationStatus().getData().getStations().size());
        });
        loader.update();
        loader.unsubscribe(subscriber);
    }
}
