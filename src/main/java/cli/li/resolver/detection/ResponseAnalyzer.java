package cli.li.resolver.detection;

import java.util.ArrayList;
import java.util.List;

import cli.li.resolver.detection.impl.AwsWafDetector;
import cli.li.resolver.detection.impl.CaptchaFoxDetector;
import cli.li.resolver.detection.impl.FriendlyCaptchaDetector;
import cli.li.resolver.detection.impl.FunCaptchaDetector;
import cli.li.resolver.detection.impl.GeeTestDetector;
import cli.li.resolver.detection.impl.HCaptchaDetector;
import cli.li.resolver.detection.impl.KeyCaptchaDetector;
import cli.li.resolver.detection.impl.LeminDetector;
import cli.li.resolver.detection.impl.MtCaptchaDetector;
import cli.li.resolver.detection.impl.ProcaptchaDetector;
import cli.li.resolver.detection.impl.RecaptchaDetector;
import cli.li.resolver.detection.impl.TencentCaptchaDetector;
import cli.li.resolver.detection.impl.TurnstileDetector;
import cli.li.resolver.detection.impl.YandexSmartCaptchaDetector;

/**
 * Analyzes HTTP responses using all registered CAPTCHA detectors.
 * Results are stored in the provided DetectionStore.
 */
public class ResponseAnalyzer {

    private final List<CaptchaDetector> detectors;
    private final DetectionStore store;

    /**
     * Create a new ResponseAnalyzer with all built-in detectors.
     *
     * @param store the detection store to add results to
     */
    public ResponseAnalyzer(DetectionStore store) {
        this.store = store;
        this.detectors = new ArrayList<>();
        this.detectors.add(new RecaptchaDetector());
        this.detectors.add(new HCaptchaDetector());
        this.detectors.add(new TurnstileDetector());
        this.detectors.add(new FunCaptchaDetector());
        this.detectors.add(new GeeTestDetector());
        this.detectors.add(new AwsWafDetector());
        this.detectors.add(new MtCaptchaDetector());
        this.detectors.add(new LeminDetector());
        this.detectors.add(new KeyCaptchaDetector());
        this.detectors.add(new FriendlyCaptchaDetector());
        this.detectors.add(new YandexSmartCaptchaDetector());
        this.detectors.add(new TencentCaptchaDetector());
        this.detectors.add(new CaptchaFoxDetector());
        this.detectors.add(new ProcaptchaDetector());
    }

    /**
     * Analyze an HTTP response body for CAPTCHA presence.
     * Runs all detectors and adds any found CAPTCHAs to the store.
     *
     * @param url  the URL of the page
     * @param body the HTTP response body to analyze
     */
    public void analyze(String url, String body) {
        if (url == null || body == null || body.isEmpty()) {
            return;
        }

        for (CaptchaDetector detector : detectors) {
            List<DetectedCaptcha> results = detector.detect(url, body);
            for (DetectedCaptcha captcha : results) {
                store.add(captcha);
            }
        }
    }
}
