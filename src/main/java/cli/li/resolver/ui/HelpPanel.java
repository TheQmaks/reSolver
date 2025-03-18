package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;

/**
 * Panel for help information
 */
public class HelpPanel extends JPanel {
    public HelpPanel() {
        setLayout(new BorderLayout());

        // Create tabbed pane for different help sections
        JTabbedPane helpTabs = new JTabbedPane();

        // Usage instructions
        JPanel usagePanel = createHelpPanel(
                "Usage Instructions",
                "reSolver is an extension for automatically solving CAPTCHAs during web application testing.\n\n" +
                        "To use the extension, you need to:\n" +
                        "1. Configure at least one CAPTCHA solving service in the Services tab\n" +
                        "2. Add CAPTCHA placeholders to your HTTP requests\n\n" +
                        "The extension will automatically detect and solve CAPTCHAs when requests are sent through Burp Suite. " +
                        "You can monitor the CAPTCHA solving process in the Logs tab."
        );

        // Placeholder format
        JPanel placeholderPanel = createHelpPanel(
                "Placeholder Format",
                "CAPTCHA placeholders use the following format:\n\n" +
                        "{{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}\n\n" +
                        "Where:\n" +
                        "- TYPE: CAPTCHA type (recaptchav2, recaptchav3)\n" +
                        "- SITEKEY: The site key for the CAPTCHA\n" +
                        "- URL: URL of the page containing the CAPTCHA\n" +
                        "- OPTIONAL_PARAMS: Additional parameters as key-value pairs (comma-separated)\n\n" +
                        "Examples:\n" +
                        "{{CAPTCHA[:]recaptchav2[:]6LdSzVkUAAAAAOVH1ZRLfnzCQzYX-PZWdZjWVI1k[:]https://example.com/login}}\n" +
                        "{{CAPTCHA[:]recaptchav2[:]6LdSzVkUAAAAAOVH1ZRLfnzCQzYX-PZWdZjWVI1k[:]https://example.com/login[:]timeout_seconds=60}}\n" +
                        "{{CAPTCHA[:]recaptchav2[:]6LdSzVkUAAAAAOVH1ZRLfnzCQzYX-PZWdZjWVI1k[:]https://example.com/login[:]invisible,enterprise}}\n" +
                        "{{CAPTCHA[:]recaptchav3[:]6LcW00EUAAAAAOBBDw0eO0XlGT6Ixk3RqQ_qrb6X[:]https://example.com/contact[:]action=login,min_score=0.7}}\n" +
                        "{{CAPTCHA[:]recaptchav3[:]6LcW00EUAAAAAOBBDw0eO0XlGT6Ixk3RqQ_qrb6X[:]https://example.com/contact[:]enterprise,action=login}}"
        );

        // Supported CAPTCHA types
        JPanel captchaTypesPanel = createHelpPanel(
                "Supported CAPTCHA Types",
                "reSolver currently supports the following CAPTCHA types:\n\n" +
                        "- reCAPTCHA v2: Standard checkbox CAPTCHA\n" +
                        "  Code: recaptchav2\n" +
                        "  Alternative codes: recaptcha2, recaptcha_v2\n" +
                        "  Optional parameters:\n" +
                        "    - invisible: Marks the CAPTCHA as invisible type\n" +
                        "    - enterprise: Marks the CAPTCHA as enterprise version\n" +
                        "    - timeout_seconds: Custom timeout (10-120)\n\n" +
                        "- reCAPTCHA v3: Invisible CAPTCHA based on behavior analysis\n" +
                        "  Code: recaptchav3\n" +
                        "  Alternative codes: recaptcha3, recaptcha_v3\n" +
                        "  Optional parameters:\n" +
                        "    - action: Action name (default: verify)\n" +
                        "    - min_score: Minimum score threshold\n" +
                        "    - enterprise: Marks the CAPTCHA as enterprise version\n" +
                        "    - timeout_seconds: Custom timeout (10-120)\n\n" +
                        "reSolver uses 3 supported CAPTCHA solving services:\n" +
                        "- 2Captcha (Default priority: 0)\n" +
                        "- Anti-Captcha (Default priority: 1)\n" +
                        "- CapMonster (Default priority: 2)\n\n" +
                        "Services are used in order of their priority (lower number = higher priority)."
        );

        // Timeout parameter
        JPanel timeoutPanel = createHelpPanel(
                "Custom Parameters",
                "reSolver allows configuring additional parameters for CAPTCHA solving:\n\n" +
                        "1. Custom Timeout\n" +
                        "   You can specify a custom timeout for CAPTCHA solving by adding the 'timeout_seconds' parameter to your placeholder:\n" +
                        "   Example: timeout_seconds=60\n\n" +
                        "   The valid range for timeout is 10-120 seconds. If not specified, the default timeout is 30 seconds.\n\n" +
                        "2. Invisible reCAPTCHA\n" +
                        "   For invisible reCAPTCHA v2, add the 'invisible' parameter to your placeholder:\n" +
                        "   Example: invisible\n\n" +
                        "3. Enterprise reCAPTCHA\n" +
                        "   For enterprise versions of reCAPTCHA, add the 'enterprise' parameter to your placeholder:\n" +
                        "   Example: enterprise\n\n" +
                        "4. Action and Min Score (reCAPTCHA v3 only)\n" +
                        "   Configure the action name and minimum score threshold for reCAPTCHA v3:\n" +
                        "   Examples: action=login, min_score=0.7\n\n" +
                        "You can combine multiple parameters using commas:\n" +
                        "Example: {{CAPTCHA[:]recaptchav2[:]SITEKEY[:]URL[:]invisible,enterprise,timeout_seconds=60}}"
        );

        // Troubleshooting
        JPanel troubleshootingPanel = createHelpPanel(
                "Troubleshooting",
                "Common issues and solutions:\n\n" +
                        "1. Invalid API Key\n" +
                        "   Make sure your API key is correct and has sufficient balance. The balance will be automatically checked and displayed in the Services tab when you add or update an API key.\n\n" +
                        "2. Incorrect CAPTCHA Type\n" +
                        "   Double-check that you're using the correct CAPTCHA type in your placeholder.\n\n" +
                        "3. Incorrect Site Key\n" +
                        "   Verify that the site key in your placeholder matches the one on the target website.\n\n" +
                        "4. Timeout Issues\n" +
                        "   If CAPTCHAs are timing out, try increasing the timeout value or check if the CAPTCHA service is experiencing high load.\n\n" +
                        "5. Enterprise and Invisible CAPTCHAs\n" +
                        "   Make sure to add the appropriate parameters ('invisible', 'enterprise') for special CAPTCHA types.\n\n" +
                        "For detailed logs and debugging information, check the Logs tab."
        );

        // Add panels to tabs
        helpTabs.addTab("Usage", usagePanel);
        helpTabs.addTab("Placeholder Format", placeholderPanel);
        helpTabs.addTab("CAPTCHA Types", captchaTypesPanel);
        helpTabs.addTab("Custom Parameters", timeoutPanel);
        helpTabs.addTab("Troubleshooting", troubleshootingPanel);

        // Add tabs to main panel
        add(helpTabs, BorderLayout.CENTER);
    }

    private JPanel createHelpPanel(String title, String content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Content
        JTextArea contentArea = new JTextArea(content);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBackground(panel.getBackground());
        contentArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}