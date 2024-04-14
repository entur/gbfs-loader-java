# gbfs-loader-java

Manage loading of GBFS feeds and updating them based on their
`ttl` and `last_updated` fields.

The loader uses the generated GBFS Java model (http://github.com/entur/gbfs-java-model)
to deserialize the files.

## Usage

### GbfsLoader directly

You may use the GbfsLoader directly. You will need to create one instance
per GBFS feed (system). The loader will use the discovery file to fetch all other available
files.

The GbfsLoader class has two factory methods, one for GBFS v2 and one for GBFS v3:

        GbfsV2Loader loader = GbfsLoader.gbfsV2Loader(
                "http://example.com/gbfs.json",
                Map.of(),
                "en",
                null, // optional RequestAuthenticator
                null // optional timeout override
        )

        // Same as above except language code is dropped
        GbfsV3Loader v3loader = GbfsLoader.gbfsV3Loader(
                "http://example.com/gbfs.json",
                Map.of(),
                null, // optional RequestAuthenticator
                null // optional timeout override
        )


        // If the update fetched any files
        if (loader.update()) {

            // Get the data for a specific fiile
            GbfsStationStatus stationStatus = loader.getData(GbfsStationStatus.class);
        }

### Use subscriptions

For convenience, a set of classes is provided to encapsulate the loader into
a subscription for each feed, and with a delivery class that contains all files
for each feed.

The main entrypoint, GbfsSubscriptionManager, has two subscribe methods, one for
GBFS v2 and one for GBFS v3. I.e., when you subscribe to a GBFS feed, you must know in
advance if it is version 2.x or version 3.x.

        GbfsSubscriptionManager subscriptions = new GbfsSubscriptionManager();

        // Subscribe to as many feeds as you want
        GbfsSubscriptionOptions options = new GbfsSubscriptionOptions();
        options.setDiscoveryURI(new URI("file:src/test/resources/gbfs/lillestrombysykkel/gbfs.json"));
        options.setLanguageCode("nb");

        String subscriber = subscriptions.subscribeV2(options, delivery -> {
            // Consume an update on the subscription
            Assertions.assertEquals(6, delivery.getStationStatus().getData().getStations().size());
        });

        // Use your own scheduler to update the subscriptions
        subscriptions.update();

### Authentication

The `GbfsLoader` constructor, as well as the `GbfsSubscriptionOptions` have an optional
`RequestAuthenticator` parameter. Use this with GBFS feeds that require authentication.

The following implementations are provided in this library: `Oauth2ClientCredentialsGrantRequestAuthenticator`, 
`BearerTokenRequestAuthenticator` and `HttpHeadersRequestAuthenticator`. You can also implement the
`RequestAuthenticator` interface to provide use custom authentication schemes.


## Maven central
This project is available in the central maven repository.
See https://search.maven.org/search?q=g:org.entur.gbfs
