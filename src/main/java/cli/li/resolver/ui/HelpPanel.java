package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;

/**
 * Panel for help information.
 * Provides usage instructions, placeholder format details, supported CAPTCHA types,
 * provider information, auto-detection feature details, and troubleshooting guidance.
 * Uses HTML rendering for rich text display.
 */
public class HelpPanel extends JPanel {
    public HelpPanel() {
        setLayout(new BorderLayout());

        // Create tabbed pane for different help sections
        JTabbedPane helpTabs = new JTabbedPane();

        // Usage instructions
        helpTabs.addTab("Usage", createHtmlHelpPanel(
                "<h2>Usage Instructions</h2>" +
                "<p>reSolver is an extension for automatically solving CAPTCHAs during web application testing.</p>" +
                "<p>To use the extension, you need to:</p>" +
                "<ol>" +
                "  <li>Configure at least one CAPTCHA solving provider in the <b>Services</b> tab</li>" +
                "  <li>Add CAPTCHA placeholders to your HTTP requests</li>" +
                "</ol>" +
                "<p>The extension will automatically detect and solve CAPTCHAs when requests are sent through " +
                "Burp Suite. You can monitor the CAPTCHA solving process in the <b>Logs</b> tab.</p>" +
                "<p>Additionally, reSolver features <b>Auto-Detection</b> that passively scans HTTP responses for " +
                "embedded CAPTCHAs and generates ready-to-use placeholders in the <b>Detections</b> tab.</p>"
        ));

        // Placeholder format
        helpTabs.addTab("Placeholder Format", createHtmlHelpPanel(
                "<h2>Placeholder Format</h2>" +
                "<p>CAPTCHA placeholders use the following format:</p>" +
                "<pre>{{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}</pre>" +
                "<h3>Parameters</h3>" +
                "<ul>" +
                "  <li><code>TYPE</code> &mdash; CAPTCHA type code (see Supported CAPTCHA Types tab)</li>" +
                "  <li><code>SITEKEY</code> &mdash; The site key for the CAPTCHA</li>" +
                "  <li><code>URL</code> &mdash; URL of the page containing the CAPTCHA</li>" +
                "  <li><code>OPTIONAL_PARAMS</code> &mdash; Additional parameters as key-value pairs (comma-separated)</li>" +
                "</ul>" +
                "<p><b>Note:</b> The <code>[:]</code> separator is used instead of plain <code>:</code> to avoid conflicts with colons in URLs and site keys.</p>" +
                "<h3>Examples</h3>" +
                "<pre>{{CAPTCHA[:]recaptchav2[:]6LdSzVkUAAAA...[:]https://example.com/login}}\n" +
                "{{CAPTCHA[:]recaptchav2[:]6LdSzVkUAAAA...[:]https://example.com/login[:]invisible,enterprise}}\n" +
                "{{CAPTCHA[:]recaptchav3[:]6LcW00EUAAAA...[:]https://example.com/contact[:]action=login,min_score=0.7}}\n" +
                "{{CAPTCHA[:]hcaptcha[:]a1b2c3d4-e5f6...[:]https://example.com/signup}}\n" +
                "{{CAPTCHA[:]turnstile[:]0x4AAAAAAA...[:]https://example.com/verify}}\n" +
                "{{CAPTCHA[:]funcaptcha[:]12345678-1234...[:]https://example.com/challenge}}\n" +
                "{{CAPTCHA[:]geetest[:]a]b]c]d]e]f[:]https://example.com/auth}}\n" +
                "{{CAPTCHA[:]geetestv4[:]captcha-id-here[:]https://example.com/auth}}\n" +
                "{{CAPTCHA[:]awswaf[:]AQIDAHjcYu/GjX+QlghicBg...=[:]https://example.com/protected}}</pre>"
        ));

        // Supported CAPTCHA types
        helpTabs.addTab("CAPTCHA Types", createHtmlHelpPanel(
                "<h2>Supported CAPTCHA Types</h2>" +
                "<p>reSolver supports the following <b>8</b> CAPTCHA types:</p>" +
                "<table>" +
                "<tr><th>#</th><th>Name</th><th>Code</th><th>Optional Parameters</th></tr>" +
                "<tr><td>1</td><td>reCAPTCHA v2</td><td><code>recaptchav2</code></td>" +
                "    <td><code>invisible</code>, <code>enterprise</code>, <code>timeout_seconds</code></td></tr>" +
                "<tr><td>2</td><td>reCAPTCHA v3</td><td><code>recaptchav3</code></td>" +
                "    <td><code>action</code>, <code>min_score</code>, <code>enterprise</code>, <code>timeout_seconds</code></td></tr>" +
                "<tr><td>3</td><td>hCaptcha</td><td><code>hcaptcha</code></td>" +
                "    <td><code>timeout_seconds</code></td></tr>" +
                "<tr><td>4</td><td>Cloudflare Turnstile</td><td><code>turnstile</code></td>" +
                "    <td><code>timeout_seconds</code></td></tr>" +
                "<tr><td>5</td><td>FunCaptcha (Arkose Labs)</td><td><code>funcaptcha</code></td>" +
                "    <td><code>timeout_seconds</code></td></tr>" +
                "<tr><td>6</td><td>GeeTest v3</td><td><code>geetest</code></td>" +
                "    <td><code>timeout_seconds</code></td></tr>" +
                "<tr><td>7</td><td>GeeTest v4</td><td><code>geetestv4</code></td>" +
                "    <td><code>timeout_seconds</code></td></tr>" +
                "<tr><td>8</td><td>AWS WAF CAPTCHA</td><td><code>awswaf</code></td>" +
                "    <td><code>timeout_seconds</code></td></tr>" +
                "</table>"
        ));

        // Providers
        helpTabs.addTab("Providers", createHtmlHelpPanel(
                "<h2>Supported Providers</h2>" +
                "<p>reSolver supports <b>6</b> CAPTCHA solving providers:</p>" +
                "<table>" +
                "<tr><th>#</th><th>Provider</th><th>Website</th></tr>" +
                "<tr><td>1</td><td>2Captcha</td><td>2captcha.com</td></tr>" +
                "<tr><td>2</td><td>RuCaptcha</td><td>rucaptcha.com</td></tr>" +
                "<tr><td>3</td><td>Anti-Captcha</td><td>anti-captcha.com</td></tr>" +
                "<tr><td>4</td><td>CapMonster Cloud</td><td>capmonster.cloud</td></tr>" +
                "<tr><td>5</td><td>CapSolver</td><td>capsolver.com</td></tr>" +
                "<tr><td>6</td><td>SolveCaptcha</td><td>solvecaptcha.com</td></tr>" +
                "</table>" +
                "<p>Providers are used in order of their <b>priority</b> (lower number = higher priority). " +
                "You can adjust priorities in the Services tab using the Priority Up/Down buttons. " +
                "Each provider shows which CAPTCHA types it supports in the <i>Supported Types</i> column.</p>"
        ));

        // Auto-Detection feature
        helpTabs.addTab("Auto-Detection", createHtmlHelpPanel(
                "<h2>Auto-Detection</h2>" +
                "<p>reSolver includes an Auto-Detection feature that passively scans HTTP responses " +
                "for embedded CAPTCHAs.</p>" +
                "<h3>How it works</h3>" +
                "<ul>" +
                "  <li>As you browse through Burp Suite, reSolver inspects HTTP responses for known " +
                "CAPTCHA patterns (JavaScript includes, site keys, widget containers, etc.)</li>" +
                "  <li>Detected CAPTCHAs appear automatically in the <b>Detections</b> tab</li>" +
                "  <li>Each detection includes the page URL, CAPTCHA type, extracted site key, " +
                "detection time, and a ready-to-use placeholder</li>" +
                "</ul>" +
                "<h3>Using detected CAPTCHAs</h3>" +
                "<ol>" +
                "  <li>Navigate to the <b>Detections</b> tab</li>" +
                "  <li>Select a detected CAPTCHA row</li>" +
                "  <li>Click <code>Copy Placeholder</code> to copy the placeholder to your clipboard</li>" +
                "  <li>Paste the placeholder into your request in Repeater, Intruder, or any other Burp tool</li>" +
                "</ol>" +
                "<h3>Supported detection types</h3>" +
                "<ul>" +
                "  <li>reCAPTCHA v2/v3 (Google script includes and site key extraction)</li>" +
                "  <li>hCaptcha (hCaptcha script and site key detection)</li>" +
                "  <li>Cloudflare Turnstile (Turnstile widget detection)</li>" +
                "  <li>FunCaptcha / Arkose Labs (FunCaptcha embed detection)</li>" +
                "  <li>GeeTest (GeeTest script and API detection)</li>" +
                "  <li>AWS WAF CAPTCHA (AWS WAF challenge script detection)</li>" +
                "</ul>" +
                "<p><b>Note:</b> Duplicate detections (same URL and site key) are automatically filtered out. " +
                "The store holds up to 500 detections. Use <code>Clear All</code> to reset.</p>"
        ));

        // Custom parameters
        helpTabs.addTab("Custom Parameters", createHtmlHelpPanel(
                "<h2>Custom Parameters</h2>" +
                "<p>reSolver allows configuring additional parameters for CAPTCHA solving:</p>" +
                "<h3>1. Custom Timeout</h3>" +
                "<p>Specify a custom timeout by adding the <code>timeout_seconds</code> parameter:</p>" +
                "<pre>timeout_seconds=60</pre>" +
                "<p>Valid range: 10&ndash;120 seconds. Default: 30 seconds.</p>" +
                "<h3>2. Invisible reCAPTCHA (v2 only)</h3>" +
                "<p>For invisible reCAPTCHA v2, add the <code>invisible</code> parameter:</p>" +
                "<pre>invisible</pre>" +
                "<h3>3. Enterprise reCAPTCHA (v2/v3)</h3>" +
                "<p>For enterprise versions of reCAPTCHA, add the <code>enterprise</code> parameter:</p>" +
                "<pre>enterprise</pre>" +
                "<h3>4. Action and Min Score (reCAPTCHA v3 only)</h3>" +
                "<p>Configure the action name and minimum score threshold:</p>" +
                "<pre>action=login\nmin_score=0.7</pre>" +
                "<h3>Combining parameters</h3>" +
                "<p>You can combine multiple parameters using commas:</p>" +
                "<pre>{{CAPTCHA[:]recaptchav2[:]SITEKEY[:]URL[:]invisible,enterprise,timeout_seconds=60}}</pre>"
        ));

        // Troubleshooting
        helpTabs.addTab("Troubleshooting", createHtmlHelpPanel(
                "<h2>Troubleshooting</h2>" +
                "<p>Common issues and solutions:</p>" +
                "<h3>1. Invalid API Key</h3>" +
                "<p>Make sure your API key is correct and has sufficient balance. The balance will be " +
                "automatically checked and displayed in the Services tab when you add or update an API key.</p>" +
                "<h3>2. Incorrect CAPTCHA Type</h3>" +
                "<p>Double-check that you're using the correct CAPTCHA type code in your placeholder. " +
                "Valid codes: <code>recaptchav2</code>, <code>recaptchav3</code>, <code>hcaptcha</code>, " +
                "<code>turnstile</code>, <code>funcaptcha</code>, <code>geetest</code>, <code>geetestv4</code>, " +
                "<code>awswaf</code>.</p>" +
                "<h3>3. Incorrect Site Key</h3>" +
                "<p>Verify that the site key in your placeholder matches the one on the target website. " +
                "You can use the Auto-Detection feature to extract site keys automatically.</p>" +
                "<h3>4. Unsupported Type for Provider</h3>" +
                "<p>Not all providers support all CAPTCHA types. Check the <i>Supported Types</i> column " +
                "in the Services tab to verify your provider supports the type you need.</p>" +
                "<h3>5. Timeout Issues</h3>" +
                "<p>If CAPTCHAs are timing out, try increasing the timeout value using the " +
                "<code>timeout_seconds</code> parameter, or check if the provider service is experiencing high load.</p>" +
                "<h3>6. No Provider Available</h3>" +
                "<p>Ensure at least one provider is enabled with a valid API key and positive balance. " +
                "The extension automatically skips providers with zero or negative balances.</p>" +
                "<p>For detailed logs and debugging information, check the <b>Logs</b> tab.</p>"
        ));

        // Add tabs to main panel
        add(helpTabs, BorderLayout.CENTER);
    }

    /**
     * Create an HTML help panel with a JEditorPane inside a scroll pane.
     *
     * @param html the HTML body content
     * @return panel containing the rendered HTML
     */
    private JPanel createHtmlHelpPanel(String html) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JEditorPane editorPane = new JEditorPane();
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(javax.swing.UIManager.getFont("Label.font"));
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setText(wrapHtml(html));
        editorPane.setCaretPosition(0);
        editorPane.setBackground(panel.getBackground());

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Wrap HTML body content with a full HTML document including shared CSS styles.
     *
     * @param body the HTML body content
     * @return complete HTML document string
     */
    private String wrapHtml(String body) {
        Color bg = getBackground();
        String bgHex = String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue());

        String textColor = UIHelper.toHex(UIHelper.getTextColor());
        String headingColor = UIHelper.toHex(UIHelper.getHeadingColor());
        String codeBg = UIHelper.toHex(UIHelper.getCodeBackground());
        String borderColor = UIHelper.toHex(UIHelper.getBorderColor());

        return "<html><head><style>" +
                "body { font-family: SansSerif, Arial, Helvetica; font-size: 11pt; margin: 6px 10px; " +
                "       background-color: " + bgHex + "; color: " + textColor + "; }" +
                "h2 { font-size: 14pt; margin-top: 4px; margin-bottom: 8px; color: " + headingColor + "; }" +
                "h3 { font-size: 11pt; margin-top: 16px; margin-bottom: 6px; color: " + headingColor + "; }" +
                "code { background-color: " + codeBg + "; padding: 2px 4px; font-family: Monospaced, Consolas, monospace; font-size: 10pt; }" +
                "pre { background-color: " + codeBg + "; padding: 8px 10px; font-family: Monospaced, Consolas, monospace; " +
                "      font-size: 10pt; border: 1px solid " + borderColor + "; }" +
                "ul, ol { margin-top: 6px; margin-bottom: 6px; padding-left: 24px; }" +
                "li { margin-bottom: 4px; }" +
                "p { margin-top: 6px; margin-bottom: 8px; }" +
                "table { border-collapse: collapse; margin: 10px 0; width: 100%; }" +
                "th { background-color: " + codeBg + "; padding: 6px 10px; text-align: left; border: 1px solid " + borderColor + "; font-size: 10pt; }" +
                "td { padding: 5px 10px; border: 1px solid " + borderColor + "; font-size: 10pt; }" +
                "b { color: " + headingColor + "; }" +
                "</style></head><body>" + body + "</body></html>";
    }
}
