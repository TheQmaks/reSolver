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
                        "The extension will automatically detect and solve CAPTCHAs when requests are sent through Burp Suite."
        );

        // Placeholder format - ИЗМЕНЕНО для формата [:]
        JPanel placeholderPanel = createHelpPanel(
                "Placeholder Format",
                "CAPTCHA placeholders используют следующий формат:\n\n" +
                        "{{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}\n\n" +
                        "Где:\n" +
                        "- TYPE: Тип CAPTCHA (recaptcha2, recaptcha3, hcaptcha, funcaptcha)\n" +
                        "- SITEKEY: Ключ сайта для CAPTCHA\n" +
                        "- URL: URL страницы, содержащей CAPTCHA\n" +
                        "- OPTIONAL_PARAMS: Дополнительные параметры, необходимые для конкретных типов CAPTCHA\n\n" +
                        "Примеры:\n" +
                        "{{CAPTCHA[:]recaptcha2[:]6LdSzVkUAAAAAOVH1ZRLfnzCQzYX-PZWdZjWVI1k[:]https://example.com/login}}\n" +
                        "{{CAPTCHA[:]recaptcha3[:]6LcW00EUAAAAAOBBDw0eO0XlGT6Ixk3RqQ_qrb6X[:]https://example.com/contact[:]action=login,min_score=0.7}}\n" +
                        "{{CAPTCHA[:]hcaptcha[:]a5f74b19-9e45-40e0-b45d-47ff91b7a6c2[:]https://example.com/register}}"
        );

        // Supported CAPTCHA types
        JPanel captchaTypesPanel = createHelpPanel(
                "Supported CAPTCHA Types",
                "reSolver supports the following CAPTCHA types:\n\n" +
                        "- reCAPTCHA v2: Standard checkbox CAPTCHA\n" +
                        "  Code: recaptcha2\n\n" +
                        "- reCAPTCHA v3: Invisible CAPTCHA based on behavior analysis\n" +
                        "  Code: recaptcha3\n" +
                        "  Optional parameters: action, min_score\n\n" +
                        "- hCaptcha: Alternative CAPTCHA system\n" +
                        "  Code: hcaptcha\n\n" +
                        "- FunCaptcha: Interactive CAPTCHA with puzzles\n" +
                        "  Code: funcaptcha"
        );

        // Troubleshooting
        JPanel troubleshootingPanel = createHelpPanel(
                "Troubleshooting",
                "Common issues and solutions:\n\n" +
                        "1. CAPTCHAs are not being solved\n" +
                        "   - Check if you've configured at least one service in the Services tab\n" +
                        "   - Verify that your API keys are valid\n" +
                        "   - Ensure your placeholder format is correct\n" +
                        "   - Check if the service has sufficient balance\n\n" +
                        "2. High load handling\n" +
                        "   - If you're sending many requests with CAPTCHAs, adjust the thread pool size and queue settings\n" +
                        "   - Consider increasing the high load threshold if needed\n\n" +
                        "3. Specific CAPTCHA type issues\n" +
                        "   - reCAPTCHA v3: Ensure you've specified the correct action parameter\n" +
                        "   - FunCaptcha: May require additional parameters for some implementations"
        );

        // Add panels to tabs
        helpTabs.addTab("Usage", usagePanel);
        helpTabs.addTab("Placeholder Format", placeholderPanel);
        helpTabs.addTab("CAPTCHA Types", captchaTypesPanel);
        helpTabs.addTab("Troubleshooting", troubleshootingPanel);

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