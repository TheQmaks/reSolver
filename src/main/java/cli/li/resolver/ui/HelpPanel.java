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
                "reSolver is an extension for automatically solving CAPTCHAs during web application testing. " +
                        "To use the extension, you need to:\n\n" +
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
                        "- TYPE: CAPTCHA type (recaptcha2, recaptcha3)\n" +
                        "- SITEKEY: The site key for the CAPTCHA\n" +
                        "- URL: URL of the page containing the CAPTCHA\n" +
                        "- OPTIONAL_PARAMS: Additional parameters required for specific CAPTCHA types\n\n" +
                        "Examples:\n" +
                        "{{CAPTCHA[:]recaptcha2[:]6LdSzVkUAAAAAOVH1ZRLfnzCQzYX-PZWdZjWVI1k[:]https://example.com/login}}\n" +
                        "{{CAPTCHA[:]recaptcha2[:]6LdSzVkUAAAAAOVH1ZRLfnzCQzYX-PZWdZjWVI1k[:]https://example.com/login[:]is_invisible=true}}\n" +
                        "{{CAPTCHA[:]recaptcha3[:]6LcW00EUAAAAAOBBDw0eO0XlGT6Ixk3RqQ_qrb6X[:]https://example.com/contact[:]action=login,min_score=0.7}}"
        );

        // Supported CAPTCHA types
        JPanel captchaTypesPanel = createHelpPanel(
                "Supported CAPTCHA Types",
                "reSolver currently supports the following CAPTCHA types:\n\n" +
                        "- reCAPTCHA v2: Standard checkbox CAPTCHA\n" +
                        "  Code: recaptcha2\n" +
                        "  Optional parameters: is_invisible (true/false)\n\n" +
                        "- reCAPTCHA v3: Invisible CAPTCHA based on behavior analysis\n" +
                        "  Code: recaptcha3\n" +
                        "  Optional parameters: action, min_score\n\n" +
                        "reSolver uses 3 supported CAPTCHA solving services:\n" +
                        "- 2Captcha (Default priority: 0)\n" +
                        "- Anti-Captcha (Default priority: 1)\n" +
                        "- CapMonster (Default priority: 2)\n\n" +
                        "Services are used in order of their priority (lower number = higher priority)."
        );

        // Troubleshooting
        JPanel troubleshootingPanel = createHelpPanel(
                "Troubleshooting",
                "Common issues and solutions:\n\n" +
                        "1. CAPTCHAs are not being solved\n" +
                        "   - Check if you've configured at least one service in the Services tab\n" +
                        "   - Verify that your API keys are valid\n" +
                        "   - Ensure your placeholder format is correct\n" +
                        "   - Check if the service has sufficient balance\n" +
                        "   - Check the Logs tab for error messages\n\n" +
                        "2. High load handling\n" +
                        "   - If you're sending many requests with CAPTCHAs, adjust the thread pool size and queue settings\n" +
                        "   - Consider increasing the high load threshold if needed\n\n" +
                        "3. Specific CAPTCHA type issues\n" +
                        "   - reCAPTCHA v2 (invisible): Add is_invisible=true parameter\n" +
                        "   - reCAPTCHA v3: Ensure you've specified the correct action parameter"
        );

        // Logs
        JPanel logsPanel = createHelpPanel(
                "Logs",
                "The Logs tab provides real-time information about the extension's operation:\n\n" +
                        "1. Log Levels\n" +
                        "   - DEBUG: Detailed information for debugging purposes\n" +
                        "   - INFO: General information about normal operation\n" +
                        "   - WARNING: Potential issues that don't prevent operation\n" +
                        "   - ERROR: Serious problems that may affect functionality\n\n" +
                        "2. Log Filtering\n" +
                        "   - Filter by log level using the dropdown menu\n" +
                        "   - Filter by source by typing in the source filter field\n\n" +
                        "3. Log Export\n" +
                        "   - Click the 'Export Logs' button to save logs to a file\n" +
                        "   - Useful for troubleshooting and reporting issues\n\n" +
                        "4. Auto-scroll\n" +
                        "   - Enable/disable auto-scrolling to the latest log entry"
        );

        // Add panels to tabs
        helpTabs.addTab("Usage", usagePanel);
        helpTabs.addTab("Placeholder Format", placeholderPanel);
        helpTabs.addTab("CAPTCHA Types", captchaTypesPanel);
        helpTabs.addTab("Troubleshooting", troubleshootingPanel);
        helpTabs.addTab("Logs", logsPanel);

        // Add tabs to main panel
        add(helpTabs, BorderLayout.CENTER);
    }

    /**
     * Create a help panel with title and text
     * @param title Panel title
     * @param text Help text
     * @return Help panel
     */
    private JPanel createHelpPanel(String title, String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}