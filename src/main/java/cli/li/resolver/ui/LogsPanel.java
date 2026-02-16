package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.logger.LoggerService.LogEntry;
import cli.li.resolver.logger.LoggerService.LogLevel;

/**
 * Panel for displaying logs
 */
public class LogsPanel extends BasePanel {
    private final LoggerService loggerService;
    private final JTextPane logTextPane;
    private final JComboBox<LogLevel> logLevelFilter;
    private final JTextField sourceFilter;
    private final DateTimeFormatter dateFormatter;
    private final StyleContext styleContext;
    private final StyledDocument document;
    private final JCheckBox autoScrollCheckbox;
    private final Consumer<LogEntry> logListener;

    public LogsPanel() {
        this.loggerService = LoggerService.getInstance();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        setLayout(new BorderLayout());

        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        document = logTextPane.getStyledDocument();
        styleContext = StyleContext.getDefaultStyleContext();
        setupTextStyles();

        logLevelFilter = new JComboBox<>(LogLevel.values());
        logLevelFilter.setSelectedItem(LogLevel.INFO);
        logLevelFilter.addActionListener(e -> refreshLogs());
        sourceFilter = new JTextField(15);
        sourceFilter.addActionListener(e -> refreshLogs());
        autoScrollCheckbox = new JCheckBox("Auto-scroll", true);

        add(createControlPanel(), BorderLayout.NORTH);
        add(new JScrollPane(logTextPane), BorderLayout.CENTER);

        logListener = createLogListener();
        loggerService.addListener(logListener);

        loggerService.info("LogsPanel", "Logs panel initialized");
        refreshLogs();
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        controlPanel.add(new JLabel("Log Level:"));
        controlPanel.add(logLevelFilter);
        controlPanel.add(new JLabel("Source:"));
        controlPanel.add(sourceFilter);
        controlPanel.add(autoScrollCheckbox);

        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> {
            loggerService.clearLogs();
            try {
                document.remove(0, document.getLength());
            } catch (BadLocationException ex) {
                // Ignore
            }
        });
        controlPanel.add(clearButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshLogs());
        controlPanel.add(refreshButton);

        JButton exportButton = new JButton("Export Logs");
        exportButton.addActionListener(e -> exportLogs());
        controlPanel.add(exportButton);

        return controlPanel;
    }

    private Consumer<LogEntry> createLogListener() {
        return entry -> {
            if (entry == null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        document.remove(0, document.getLength());
                    } catch (BadLocationException ex) {
                        // Ignore
                    }
                });
                return;
            }

            SwingUtilities.invokeLater(() -> {
                LogLevel selectedLevel = (LogLevel) logLevelFilter.getSelectedItem();
                String selectedSource = sourceFilter.getText();

                if (entry.getLevel().ordinal() >= selectedLevel.ordinal()
                        && (selectedSource.isEmpty() || entry.getSource().contains(selectedSource))) {
                    appendLog(entry);
                }
            });
        };
    }

    @Override
    public void dispose() {
        super.dispose();
        loggerService.removeListener(logListener);
    }

    /**
     * Set up text styles for different log levels
     */
    private void setupTextStyles() {
        // Debug - secondary/muted color
        Style debugStyle = styleContext.addStyle("DEBUG", null);
        StyleConstants.setForeground(debugStyle, UIHelper.getSecondaryTextColor());

        // Info - primary text color
        Style infoStyle = styleContext.addStyle("INFO", null);
        StyleConstants.setForeground(infoStyle, UIHelper.getTextColor());

        // Warning - theme-aware amber/orange
        Style warningStyle = styleContext.addStyle("WARNING", null);
        StyleConstants.setForeground(warningStyle, UIHelper.getWarningColor());
        StyleConstants.setBold(warningStyle, true);

        // Error - theme-aware red
        Style errorStyle = styleContext.addStyle("ERROR", null);
        StyleConstants.setForeground(errorStyle, UIHelper.getErrorColor());
        StyleConstants.setBold(errorStyle, true);
    }

    /**
     * Append a log entry to the text area
     * @param entry Log entry to append
     */
    private void appendLog(LogEntry entry) {
        try {
            String timeStr = entry.getTimestamp().format(dateFormatter);
            String logLine = timeStr + " [" + entry.getLevel() + "] [" + entry.getSource() + "] " + entry.getMessage() + "\n";

            Style style = styleContext.getStyle(entry.getLevel().toString());
            document.insertString(document.getLength(), logLine, style);

            // Add exception details if present
            if (entry.getException() != null) {
                String exceptionStr = "Exception: " + entry.getException().getMessage() + "\n";
                document.insertString(document.getLength(), exceptionStr, style);

                // Add stack trace
                for (StackTraceElement element : entry.getException().getStackTrace()) {
                    document.insertString(document.getLength(), "  at " + element.toString() + "\n", style);
                }
                document.insertString(document.getLength(), "\n", null);
            }

            // Auto-scroll to the bottom
            if (autoScrollCheckbox.isSelected()) {
                logTextPane.setCaretPosition(document.getLength());
            }
        } catch (BadLocationException e) {
            // Should never happen
            e.printStackTrace();
        }
    }

    /**
     * Refresh the logs display
     */
    private void refreshLogs() {
        try {
            // Clear the document
            document.remove(0, document.getLength());

            // Get selected filters
            LogLevel selectedLevel = (LogLevel) logLevelFilter.getSelectedItem();
            String selectedSource = sourceFilter.getText();

            // Display filtered logs
            for (LogEntry entry : loggerService.getLogs()) {
                if (entry.getLevel().ordinal() >= selectedLevel.ordinal() &&
                        (selectedSource.isEmpty() || entry.getSource().contains(selectedSource))) {
                    appendLog(entry);
                }
            }
        } catch (BadLocationException e) {
            // Should never happen
            e.printStackTrace();
        }
    }

    /**
     * Export logs to a file
     */
    private void exportLogs() {
        // Create file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Logs");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        // Show save dialog
        int result = fileChooser.showSaveDialog(UIHelper.getBurpFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            try (java.io.PrintWriter writer = new java.io.PrintWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
                // Get selected filters
                LogLevel selectedLevel = (LogLevel) logLevelFilter.getSelectedItem();
                String selectedSource = sourceFilter.getText();

                // Write filtered logs to file
                for (LogEntry entry : loggerService.getLogs()) {
                    if (entry.getLevel().ordinal() >= selectedLevel.ordinal() &&
                            (selectedSource.isEmpty() || entry.getSource().contains(selectedSource))) {
                        writer.println(entry.toString());
                    }
                }

                JOptionPane.showMessageDialog(UIHelper.getBurpFrame(),
                        "Logs exported successfully to " + file.getAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(UIHelper.getBurpFrame(),
                        "Error exporting logs: " + e.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
