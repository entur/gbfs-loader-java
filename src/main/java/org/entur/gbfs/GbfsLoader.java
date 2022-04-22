package org.entur.gbfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entur.gbfs.authentication.RequestAuthenticationException;
import org.entur.gbfs.authentication.RequestAuthenticator;
import org.entur.gbfs.authentication.DummyRequestAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.entur.gbfs.v2_3.gbfs.GBFS;
import org.entur.gbfs.v2_3.gbfs.GBFSFeed;
import org.entur.gbfs.v2_3.gbfs.GBFSFeedName;
import org.entur.gbfs.v2_3.gbfs.GBFSFeeds;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private final String url;

    private final Map<String, String> httpHeaders;

    private final String languageCode;

    private final RequestAuthenticator requestAuthenticator;

    private GBFS disoveryFileData;

    private final AtomicBoolean setupComplete = new AtomicBoolean(false);

    private final Lock updateLock = new ReentrantLock();
    
    private final Long timeoutConnection;
    
    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     */
    public GbfsLoader(String url) {
        this(url, new HashMap<>(), null);
    }

    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     * @param languageCode The language code to be used to look up feeds in the discovery file
     */
    public GbfsLoader(String url, String languageCode) {
        this(url, new HashMap<>(), languageCode);
    }

    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
     * @param languageCode The language code to be used to look up feeds in the discovery file
     */
    public GbfsLoader(String url, Map<String, String> httpHeaders, String languageCode) {
        this(url, httpHeaders, languageCode, null, null);
    }

    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     * @param languageCode The language code to be used to look up feeds in the discovery file
     * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
     *                             each request
     */
    public GbfsLoader(String url, String languageCode, RequestAuthenticator requestAuthenticator) {
        this(url, new HashMap<>(), languageCode, requestAuthenticator, null);
    }

    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     * @param languageCode The language code to be used to look up feeds in the discovery file
     * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
     *            each request.
     * @param timeoutConnection The timeout connection value.
     */
    public GbfsLoader(String url, String languageCode, RequestAuthenticator requestAuthenticator,
            Long timeoutConnection) {
        this(url, new HashMap<>(), languageCode, requestAuthenticator, timeoutConnection);
    }
    
    /**
     * Create a new GbfsLoader
     *
     * @param url The URL to the GBFS discovery file
     * @param httpHeaders Additional HTTP headers to be used in requests (e.g. auth headers)
     * @param languageCode The language code to be used to look up feeds in the discovery file
     * @param requestAuthenticator An instance of RequestAuthenticator to provide authentication strategy for
     *            each request.
     * @param timeoutConnection The optional timeout connection value, by default, the static value is applied.
     */
    public GbfsLoader(String url, Map<String, String> httpHeaders, String languageCode,
                      RequestAuthenticator requestAuthenticator, Long timeoutConnection) {
        if (requestAuthenticator == null) {
            this.requestAuthenticator = new DummyRequestAuthenticator();
        } else {
            this.requestAuthenticator = requestAuthenticator;
        }

        this.url = url;
        this.httpHeaders = httpHeaders;
        this.languageCode = languageCode;
        this.timeoutConnection = timeoutConnection;
        
        init();
    }

    private synchronized void init() {
        if (setupComplete.get()) {
            return;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid url " + url);
        }

        if (!url.endsWith("gbfs.json")) {
            LOG.warn("GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.", url);
        }

        if (authenticateRequest()) {
            byte[] rawFeed = fetchFeed(uri, httpHeaders, timeoutConnection);
            try {
                disoveryFileData = objectMapper.readValue(rawFeed, GBFS.class);
            } catch (IOException e) {
                LOG.warn("Error unmarshalling discovery feed", e);
            }

            if (disoveryFileData != null) {
                createUpdaters();
                setupComplete.set(true);
            } else {
                LOG.warn("Could not fetch the feed auto-configuration file from {}", uri);
            }
        }
    }

    private void createUpdaters() {
        // Pick first language if none defined
        GBFSFeeds feeds = languageCode == null
                ? disoveryFileData.getFeedsData().values().iterator().next()
                : disoveryFileData.getFeedsData().get(languageCode);
        if (feeds == null) {
            throw new RuntimeException("Language " + languageCode + " does not exist in feed " + url);
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

    private boolean authenticateRequest() {
        try {
            this.requestAuthenticator.authenticateRequest(httpHeaders);
            return true;
        } catch (RequestAuthenticationException e) {
            LOG.warn("Unable to authenticate request: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if any of the feeds should be updated base on the TTL and fetches. Returns true, if any feeds were updated.
     */
    public boolean update() {
        if (!setupComplete.get()) {
            init();
        }

        boolean didUpdate = false;
        if (updateLock.tryLock()) {
            for (GBFSFeedUpdater<?> updater : feedUpdaters.values()) {
                if (updater.shouldUpdate()) {
                    updater.fetchData();
                    didUpdate = true;
                }
            }
            updateLock.unlock();
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

    public byte[] getRawFeed(GBFSFeedName feedName) {
        GBFSFeedUpdater<?> updater = feedUpdaters.get(feedName);
        if (updater == null) { return null; }
        return updater.getRawData();
    }

    /* private static methods */
    private static byte[] fetchFeed(URI uri, Map<String, String> httpHeaders) {
        return fetchFeed(uri, httpHeaders, null);
    }
    
    private static byte[] fetchFeed(URI uri, Map<String, String> httpHeaders, Long timeout) {
        try {
            InputStream is;

            String proto = uri.getScheme();
            if (proto.equals("http") || proto.equals("https")) {
                is = HttpUtils.getData(uri, timeout, httpHeaders);
            } else {
                // Local file probably, try standard java
                is = uri.toURL().openStream();
            }
            if (is == null) {
                LOG.warn("Failed to get data from url {}", uri);
                return null;
            }

            byte[] asBytes = is.readAllBytes();
            is.close();

            return asBytes;
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
        private byte[] rawData;

        @SuppressWarnings("unchecked")
        private GBFSFeedUpdater(GBFSFeed feed) {
            url = feed.getUrl();
            implementingClass = (Class<T>) feed.getName().implementingClass();
        }

        private T getData() {
            return data;
        }

        public byte[] getRawData() {
            return rawData;
        }

        private void fetchData() {
            if (!authenticateRequest()) {
                 return;
            }

            rawData = GbfsLoader.fetchFeed(url, httpHeaders);

            if (rawData == null) {
                LOG.warn("Invalid data for {}", url);
                nextUpdate = getCurrentTimeSeconds();
                return;
            }

            try {
                data = objectMapper.readValue(rawData, implementingClass);
            } catch (IOException e) {
                LOG.warn("Error unmarshalling feed", e);
                data = null;
            }

            try {
                // Fetch lastUpdated and ttl from the resulting class. Due to type erasure we don't know the actual
                // class, and have to use introspection to get the method references, as they do not share a supertype.
                Integer lastUpdated = (Integer) implementingClass.getMethod("getLastUpdated").invoke(data);
                Integer ttl = (Integer) implementingClass.getMethod("getTtl").invoke(data);
                if (lastUpdated == null || ttl == null) {
                    nextUpdate = getCurrentTimeSeconds();
                } else {
                    nextUpdate = lastUpdated + ttl;
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
                LOG.warn("Invalid data for {}", url);
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
