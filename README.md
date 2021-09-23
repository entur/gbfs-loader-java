# gbfs-loader-java

Manage loading of GBFS feeds and updating them based on their
`ttl` and `last_updated` fields.

The loader uses the generated GBFS Java model (http://github.com/entur/gbfs-java-model)
to deserialize the files.

## Usage

### GbfsLoader directly

You may use the GbfsLoader class directly. You will need to create one instance
per GBFS feed (system). The loader will use the discovery file to fetch all other available
files.

        Gbfsloader loader = new GbfsLoader(
                "http://example.com/gbfs.json",
                Map.of(),
                "en"
        );

        // If the update fetched any files
        if (loader.update()) {

            // Get the data for a specific fiile
            GbfsStationStatus stationStatus = loader.getData(GbfsStationStatus.class);
        }

### Use subscriptions

For convenience, a set of classes is provided to encapsulate the loader into
a subscription for each feed, and with a delivery class that contains all files
for each feed.

        GbfsSubscriptionManager subscriptions = new GbfsSubscriptionManager();

        // Subscribe to as many feeds as you want
        GbfsSubscriptionOptions options = new GbfsSubscriptionOptions();
        options.setDiscoveryURI(new URI("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json"));
        options.setLanguageCode("nb");

        String subscriber = loader.subscribe(options, delivery -> {
            // Consume an update on the subscription
            Assertions.assertEquals(6, delivery.getStationStatus().getData().getStations().size());
        });

        // Use your own scheduler to update the subscriptions
        subscriptions.update();
