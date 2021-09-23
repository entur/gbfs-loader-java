package org.entur.gbfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.entur.gbfs.v2_2.gbfs.GBFS;
import org.entur.gbfs.v2_2.gbfs.GBFSFeed;
import org.entur.gbfs.v2_2.gbfs.GBFSFeedName;
import org.entur.gbfs.v2_2.gbfs.GBFSFeeds;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according to individual feed's
 * TTL rules.
 */
public class GbfsLoader {
    private static final Logger LOG = LoggerFactory.getLogger(GbfsLoader.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    /** One updater per feed type(?)*/
    private final Map<GBFSFeedName, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();

    private final Map<String, String> httpHeaders;

    private GBFS disoveryFileData;

    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
     * @param languageCode The language code to be used to look up feeds in the discovery file
     */
    public GbfsLoader(String url, Map<String, String> httpHeaders, String languageCode) {
        this.httpHeaders = httpHeaders;
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid url " + url);
        }

        if (!url.endsWith("gbfs.json")) {
            LOG.warn("GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.", url);
        }

        // Fetch autoconfiguration file
        disoveryFileData = fetchFeed(uri, httpHeaders, GBFS.class);
        if (disoveryFileData == null) {
            throw new RuntimeException("Could not fetch the feed auto-configuration file from " + uri);
        }

        // Pick first language if none defined
        GBFSFeeds feeds = languageCode == null
                ? disoveryFileData.getFeedsData().values().iterator().next()
                : disoveryFileData.getFeedsData().get(languageCode);
        if (feeds == null) {
            throw new RuntimeException("Language " + languageCode + " does not exist in feed " + uri);
        }

        // Create updater for each file
        for (GBFSFeed feed : feeds.getFeeds()) {
            GBFSFeedName feedName = feed.getName();
            if (feedUpdaters.containsKey(feedName)) {
                throw new RuntimeException(
                        "Feed contains duplicate url for feed " + feedName + ". " +
                        "Urls: " + feed.getUrl() + ", " + feedUpdaters.get(feedName).url
                );
            }

            // name is null, if the file is of unknown type, skip those
            if (feed.getName() != null) {
                feedUpdaters.put(feedName, new GBFSFeedUpdater<>(feed));
            }

        }
    }

    /**
     * Checks if any of the feeds should be updated base on the TTL and fetches. Returns true, if any feeds were updated.
     */
    public boolean update() {
        boolean didUpdate = false;

        for (GBFSFeedUpdater<?> updater : feedUpdaters.values()) {
            if (updater.shouldUpdate()) {
                updater.fetchData();
                didUpdate = true;
            }
        }

        return didUpdate;
    }

    public GBFS getDiscoveryFeed() {
        return disoveryFileData;
    }

    /**
     * Gets the most recent contents of the feed, which contains an object of type T.
     */
    public <T> T getFeed(Class<T> feed) {
        GBFSFeedUpdater<?> updater = feedUpdaters.get(GBFSFeedName.fromClass(feed));
        if (updater == null) { return null; }
        return feed.cast(updater.getData());
    }

    /* private static methods */

    private static <T> T fetchFeed(URI uri, Map<String, String> httpHeaders, Class<T> clazz) {
        try {
            InputStream is;

            String proto = uri.getScheme();
            if (proto.equals("http") || proto.equals("https")) {
                is = HttpUtils.getData(uri, httpHeaders);
            } else {
                // Local file probably, try standard java
                is = uri.toURL().openStream();
            }
            if (is == null) {
                LOG.warn("Failed to get data from url {}", uri);
                return null;
            }
            T data = objectMapper.readValue(is, clazz);
            is.close();
            return data;
        } catch (IllegalArgumentException e) {
            LOG.warn("Error parsing GBFS feed from {}", uri, e);
            return null;
        } catch (JsonProcessingException e) {
            LOG.warn("Error parsing (bad JSON) GBFS feed from {}", uri, e);
            return null;
        } catch (IOException e) {
            LOG.warn("Error (bad connection) reading GBFS feed from {}", uri, e);
            return null;
        }
    }

    /* private static classes */

    private class GBFSFeedUpdater<T> {

        /** URL for the individual GBFS file */
        private final URI url;

        /** To which class should the file be deserialized to */
        private final Class<T> implementingClass;

        private int nextUpdate;
        private T data;

        @SuppressWarnings("unchecked")
        private GBFSFeedUpdater(GBFSFeed feed) {
            url = feed.getUrl();
            implementingClass = (Class<T>) feed.getName().implementingClass();
        }

        private T getData() {
            return data;
        }

        private void fetchData() {
            T newData = GbfsLoader.fetchFeed(url, httpHeaders, implementingClass);
            if (newData == null) {
                LOG.error("Invalid data for {}", url);
                nextUpdate = getCurrentTimeSeconds();
                return;
            }

            data = newData;

            try {
                // Fetch lastUpdated and ttl from the resulting class. Due to type erasure we don't know the actual
                // class, and have to use introspection to get the method references, as they do not share a supertype.
                Integer lastUpdated = (Integer) implementingClass.getMethod("getLastUpdated").invoke(newData);
                Integer ttl = (Integer) implementingClass.getMethod("getTtl").invoke(newData);
                if (lastUpdated == null || ttl == null) {
                    nextUpdate = getCurrentTimeSeconds();
                } else {
                    nextUpdate = lastUpdated + ttl;
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
                LOG.error("Invalid data for {}", url);
                nextUpdate = getCurrentTimeSeconds();
            }
        }

        private boolean shouldUpdate() {
            return getCurrentTimeSeconds() >= nextUpdate;
        }

        private int getCurrentTimeSeconds() {
            return (int) (System.currentTimeMillis() / 1000);
        }
    }
}
