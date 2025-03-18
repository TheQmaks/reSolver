package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.function.Consumer;
import java.time.format.DateTimeFormatter;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.logger.LoggerService.LogEntry;
import cli.li.resolver.logger.LoggerService.LogLevel;

/**
 * Panel for displaying logs
 */
public class LogsPanel extends JPanel {
    private final LoggerService loggerService;
    private final JTextPane logTextPane;
    private final JComboBox<LogLevel> logLevelFilter;
    private final JTextField sourceFilter;
    private final DateTimeFormatter dateFormatter;
    private final StyleContext styleContext;
    private final StyledDocument document;
    private final JCheckBox autoScrollCheckbox;

    public LogsPanel() {
        this.loggerService = LoggerService.getInstance();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        setLayout(new BorderLayout());

        // Create text area for logs
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        document = logTextPane.getStyledDocument();
        styleContext = StyleContext.getDefaultStyleContext();

        // Set up text styles for different log levels
        setupTextStyles();

        JScrollPane scrollPane = new JScrollPane(logTextPane);

        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Log level filter
        controlPanel.add(new JLabel("Log Level:"));
        logLevelFilter = new JComboBox<>(LogLevel.values());
        logLevelFilter.setSelectedItem(LogLevel.INFO);
        logLevelFilter.addActionListener(e -> refreshLogs());
        controlPanel.add(logLevelFilter);

        // Source filter
        controlPanel.add(new JLabel("Source:"));
        sourceFilter = new JTextField(15);
        sourceFilter.addActionListener(e -> refreshLogs());
        controlPanel.add(sourceFilter);

        // Auto-scroll checkbox
        autoScrollCheckbox = new JCheckBox("Auto-scroll", true);
        controlPanel.add(autoScrollCheckbox);

        // Clear button
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

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshLogs());
        controlPanel.add(refreshButton);

        // Export button
        JButton exportButton = new JButton("Export Logs");
        exportButton.addActionListener(e -> exportLogs());
        controlPanel.add(exportButton);

        // Add components to main panel
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Register as a log listener
        loggerService.addListener(new Consumer<LogEntry>() {
            @Override
            public void accept(LogEntry entry) {
                if (entry == null) {
                    // Clear logs signal
                    SwingUtilities.invokeLater(() -> {
                        try {
                            document.remove(0, document.getLength());
                        } catch (BadLocationException ex) {
                            // Ignore
                        }
                    });
                    return;
                }

                LogLevel selectedLevel = (LogLevel) logLevelFilter.getSelectedItem();
                String selectedSource = sourceFilter.getText();

                if (entry.getLevel().ordinal() >= selectedLevel.ordinal() &&
                        (selectedSource.isEmpty() || entry.getSource().contains(selectedSource))) {
                    SwingUtilities.invokeLater(() -> appendLog(entry));
                }
            }
        });

        // Add some initial content
        loggerService.info("LogsPanel", "Logs panel initialized");

        // Initial log display
        refreshLogs();
    }

    /**
     * Set up text styles for different log levels
     */
    private void setupTextStyles() {
        // Debug - Gray
        Style debugStyle = styleContext.addStyle("DEBUG", null);
        StyleConstants.setForeground(debugStyle, Color.GRAY);

        // Info - Black
        Style infoStyle = styleContext.addStyle("INFO", null);
        StyleConstants.setForeground(infoStyle, Color.BLACK);

        // Warning - Orange
        Style warningStyle = styleContext.addStyle("WARNING", null);
        StyleConstants.setForeground(warningStyle, Color.ORANGE);
        StyleConstants.setBold(warningStyle, true);

        // Error - Red
        Style errorStyle = styleContext.addStyle("ERROR", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Logs");
        fileChooser.setSelectedFile(new java.io.File("resolver_logs.txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
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

                JOptionPane.showMessageDialog(this,
                        "Logs exported successfully to " + file.getAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting logs: " + e.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}