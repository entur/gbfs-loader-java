import org.entur.gbfs.GbfsLoaderSubscriber;
import org.entur.gbfs.GbfsLoaderOptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class GBFSLoaderTest {

    @Test
    void testLoader() throws URISyntaxException, InterruptedException {
        var loader = new GbfsLoaderSubscriber();
        var options = new GbfsLoaderOptions();
        options.setDiscoveryURI(new URI("https://api.dev.entur.io/mobility/v2/gbfs/limeoslo/gbfs.json"));
        options.setLanguageCode("nb");
        var subscriber = loader.subscribe(options, delivery -> {
           System.out.println(System.currentTimeMillis() + " " + delivery.getFreeBikeStatus().getData().getBikes().size());
        });

        int iterations = 0;

        while(iterations < 10) {
            Thread.sleep(1000);
            loader.update();
            iterations++;
        }

        loader.unsubscribe(subscriber);
    }

}
