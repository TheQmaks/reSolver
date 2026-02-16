package cli.li.resolver.detection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DetectionStore")
class DetectionStoreTest {

    private DetectionStore store;

    @BeforeEach
    void setUp() {
        store = new DetectionStore();
    }

    @Test
    @DisplayName("add() stores a detection and getAll() retrieves it")
    void addAndGetAll() {
        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "sitekey1", Instant.now(), "evidence");

        store.add(captcha);

        List<DetectedCaptcha> results = store.getAll();
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(captcha);
    }

    @Test
    @DisplayName("deduplicates entries with same pageUrl and siteKey")
    void deduplicatesSamePageUrlAndSiteKey() {
        DetectedCaptcha first = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "KEY_A", Instant.now(), "evidence1");
        DetectedCaptcha duplicate = new DetectedCaptcha(
                "https://example.com", "hcaptcha", "KEY_A", Instant.now(), "evidence2");

        store.add(first);
        store.add(duplicate);

        assertThat(store.getAll()).hasSize(1);
        assertThat(store.getAll().get(0)).isEqualTo(first);
    }

    @Test
    @DisplayName("stores both when different pageUrl but same siteKey")
    void storesBothForDifferentPageUrlSameSiteKey() {
        DetectedCaptcha captcha1 = new DetectedCaptcha(
                "https://page1.com", "recaptchav2", "SAME_KEY", Instant.now(), "evidence1");
        DetectedCaptcha captcha2 = new DetectedCaptcha(
                "https://page2.com", "recaptchav2", "SAME_KEY", Instant.now(), "evidence2");

        store.add(captcha1);
        store.add(captcha2);

        assertThat(store.getAll()).hasSize(2);
    }

    @Test
    @DisplayName("clear() removes all detections from the store")
    void clearEmptiesStore() {
        store.add(new DetectedCaptcha(
                "https://example.com", "hcaptcha", "KEY1", Instant.now(), "evidence"));
        store.add(new DetectedCaptcha(
                "https://other.com", "turnstile", "KEY2", Instant.now(), "evidence"));

        store.clear();

        assertThat(store.getAll()).isEmpty();
    }

    @Test
    @DisplayName("notifies registered listeners when a new detection is added")
    void notifiesListenersOnAdd() {
        List<DetectedCaptcha> received = new ArrayList<>();
        store.addListener(received::add);

        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "LISTENER_KEY", Instant.now(), "evidence");
        store.add(captcha);

        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isEqualTo(captcha);
    }

    @Test
    @DisplayName("does not notify listeners when a duplicate is added")
    void doesNotNotifyOnDuplicate() {
        List<DetectedCaptcha> received = new ArrayList<>();
        store.addListener(received::add);

        DetectedCaptcha captcha = new DetectedCaptcha(
                "https://example.com", "recaptchav2", "DUP_KEY", Instant.now(), "evidence");
        store.add(captcha);
        store.add(new DetectedCaptcha(
                "https://example.com", "recaptchav2", "DUP_KEY", Instant.now(), "evidence2"));

        assertThat(received).hasSize(1);
    }

    @Test
    @DisplayName("enforces MAX_DETECTIONS limit of 500 by removing oldest entries")
    void enforcesMaxDetectionsLimit() {
        for (int i = 0; i < 501; i++) {
            store.add(new DetectedCaptcha(
                    "https://example.com/page" + i, "recaptchav2", "key" + i,
                    Instant.now(), "evidence"));
        }

        List<DetectedCaptcha> all = store.getAll();
        assertThat(all).hasSize(500);

        // The oldest entry (page0 / key0) should have been removed
        assertThat(all).extracting(DetectedCaptcha::pageUrl)
                .doesNotContain("https://example.com/page0");

        // The newest entry should still be present
        assertThat(all).extracting(DetectedCaptcha::pageUrl)
                .contains("https://example.com/page500");
    }

    @Test
    @DisplayName("getAll() returns an immutable copy of the detections")
    void getAllReturnsImmutableCopy() {
        store.add(new DetectedCaptcha(
                "https://example.com", "hcaptcha", "KEY", Instant.now(), "evidence"));

        List<DetectedCaptcha> results = store.getAll();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> results.add(new DetectedCaptcha(
                        "https://other.com", "turnstile", "KEY2", Instant.now(), "evidence"))
        );
    }
}
