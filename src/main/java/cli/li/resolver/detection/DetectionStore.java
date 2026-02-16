package cli.li.resolver.detection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe store for detected CAPTCHA instances.
 * Supports listeners for real-time notification when new CAPTCHAs are detected.
 */
public class DetectionStore {

    private static final int MAX_DETECTIONS = 500;

    private final CopyOnWriteArrayList<DetectedCaptcha> detections = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<DetectedCaptcha>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Add a detected CAPTCHA to the store.
     * Duplicates (same pageUrl and siteKey) are ignored.
     * If the store exceeds MAX_DETECTIONS, the oldest entry is removed.
     * All registered listeners are notified of the new detection.
     *
     * @param captcha the detected CAPTCHA to add
     */
    public void add(DetectedCaptcha captcha) {
        // Deduplicate by pageUrl + siteKey
        for (DetectedCaptcha existing : detections) {
            if (existing.pageUrl().equals(captcha.pageUrl())
                    && existing.siteKey().equals(captcha.siteKey())) {
                return;
            }
        }

        detections.add(captcha);

        // Enforce maximum size by removing oldest entries
        while (detections.size() > MAX_DETECTIONS) {
            detections.remove(0);
        }

        // Notify listeners
        for (Consumer<DetectedCaptcha> listener : listeners) {
            listener.accept(captcha);
        }
    }

    /**
     * Get a copy of all detected CAPTCHAs.
     *
     * @return list of all detections
     */
    public List<DetectedCaptcha> getAll() {
        return List.copyOf(detections);
    }

    /**
     * Remove a specific detected CAPTCHA from the store.
     *
     * @param captcha the detected CAPTCHA to remove
     * @return true if the captcha was found and removed
     */
    public boolean remove(DetectedCaptcha captcha) {
        return detections.remove(captcha);
    }

    /**
     * Clear all detected CAPTCHAs from the store.
     */
    public void clear() {
        detections.clear();
    }

    /**
     * Register a listener to be notified when new CAPTCHAs are detected.
     *
     * @param listener the callback to invoke on each new detection
     */
    public void addListener(Consumer<DetectedCaptcha> listener) {
        listeners.add(listener);
    }

    /**
     * Remove a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(Consumer<DetectedCaptcha> listener) {
        listeners.remove(listener);
    }
}
