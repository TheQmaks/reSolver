package cli.li.resolver.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import cli.li.resolver.logger.LoggerService;
import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.thread.CaptchaSolverThreadManager;

/**
 * Panel for extension settings.
 * Provides monitoring cards, preset selection, and configuration controls
 * for thread pool, solving, and detection settings.
 */
public class SettingsPanel extends BasePanel {
    private final SettingsManager settingsManager;
    private final CaptchaSolverThreadManager threadManager;

    // Monitoring card components
    private JLabel threadUsageValue;
    private JProgressBar threadUsageBar;
    private JLabel currentLoadValue;
    private JLabel currentLoadStatus;
    private JLabel tasksRunningValue;
    private JLabel tasksRunningSubtext;

    // Preset
    private JComboBox<String> presetCombo;
    private boolean updatingFromPreset = false;

    // Performance spinners
    private JSpinner threadPoolSizeSpinner;
    private JSpinner highLoadThresholdSpinner;

    // Solving spinners
    private JSpinner solveTimeoutSpinner;
    private JSpinner maxRetriesSpinner;

    // Detection controls
    private JCheckBox autoDetectionCheckbox;
    private JComboBox<String> logLevelCombo;

    // Unsaved changes label
    private JLabel unsavedLabel;

    public SettingsPanel(SettingsManager settingsManager, CaptchaSolverThreadManager threadManager) {
        this.settingsManager = settingsManager;
        this.threadManager = threadManager;

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Initialize spinners and controls
        initializeControls();

        add(createMonitoringCards(), BorderLayout.NORTH);
        add(createSettingsSections(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        // Start monitoring timer
        Timer timer = createTimer(1000, e -> updateMonitoringCards());
        timer.start();
    }

    private void initializeControls() {
        // Performance
        threadPoolSizeSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getThreadPoolSize(), 1, 100, 1));
        highLoadThresholdSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getHighLoadThreshold(), 1, 1000, 10));

        // Solving
        solveTimeoutSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getSolveTimeout(), 10, 600, 10));
        maxRetriesSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getMaxRetries(), 0, 10, 1));

        // Detection
        autoDetectionCheckbox = new JCheckBox("Enable automatic CAPTCHA detection");
        autoDetectionCheckbox.setSelected(settingsManager.isAutoDetectionEnabled());

        logLevelCombo = new JComboBox<>(new String[]{"DEBUG", "INFO", "WARNING", "ERROR"});
        logLevelCombo.setSelectedItem(settingsManager.getLogLevel());

        // Presets
        presetCombo = new JComboBox<>(new String[]{
                "Custom",
                "Low Load (5 threads)",
                "Balanced (10 threads)",
                "High Performance (30 threads)"
        });
        presetCombo.setSelectedItem(detectCurrentPreset());
        presetCombo.addActionListener(e -> applyPreset());
    }

    // ---- Monitoring Cards (NORTH) ----

    private JPanel createMonitoringCards() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));

        panel.add(createThreadUsageCard());
        panel.add(createCurrentLoadCard());
        panel.add(createTasksRunningCard());

        return panel;
    }

    private JPanel createThreadUsageCard() {
        JPanel card = createCardPanel();

        JLabel titleLabel = createCardTitle("Thread Usage");

        int active = threadManager.threadPoolManager().getActiveThreadCount();
        int total = threadManager.threadPoolManager().getPoolSize();
        threadUsageValue = new JLabel(active + " / " + total);
        threadUsageValue.setFont(threadUsageValue.getFont().deriveFont(Font.BOLD, 24f));
        threadUsageValue.setHorizontalAlignment(SwingConstants.CENTER);

        threadUsageBar = new JProgressBar(0, 100);
        int pct = total > 0 ? (active * 100 / total) : 0;
        threadUsageBar.setValue(pct);
        threadUsageBar.setStringPainted(false);
        threadUsageBar.setPreferredSize(new Dimension(0, 8));
        applyThreadUsageColor(pct);

        JPanel barWrapper = new JPanel(new BorderLayout());
        barWrapper.setOpaque(false);
        barWrapper.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 12));
        barWrapper.add(threadUsageBar, BorderLayout.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(threadUsageValue, BorderLayout.CENTER);
        card.add(barWrapper, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCurrentLoadCard() {
        JPanel card = createCardPanel();

        JLabel titleLabel = createCardTitle("Current Load");

        int rpm = threadManager.highLoadDetector().getRequestsInLastMinute();
        currentLoadValue = new JLabel(rpm + " req/min");
        currentLoadValue.setFont(currentLoadValue.getFont().deriveFont(Font.BOLD, 24f));
        currentLoadValue.setHorizontalAlignment(SwingConstants.CENTER);

        boolean highLoad = threadManager.highLoadDetector().isHighLoad();
        currentLoadStatus = new JLabel(highLoad ? "HIGH LOAD" : "Normal");
        currentLoadStatus.setFont(currentLoadStatus.getFont().deriveFont(Font.BOLD, 11f));
        currentLoadStatus.setForeground(highLoad ? UIHelper.getErrorColor() : UIHelper.getSuccessColor());
        currentLoadStatus.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(currentLoadValue, BorderLayout.CENTER);
        card.add(currentLoadStatus, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createTasksRunningCard() {
        JPanel card = createCardPanel();

        JLabel titleLabel = createCardTitle("Tasks Running");

        int active = threadManager.threadPoolManager().getActiveThreadCount();
        tasksRunningValue = new JLabel(String.valueOf(active));
        tasksRunningValue.setFont(tasksRunningValue.getFont().deriveFont(Font.BOLD, 24f));
        tasksRunningValue.setHorizontalAlignment(SwingConstants.CENTER);

        tasksRunningSubtext = new JLabel("active");
        tasksRunningSubtext.setFont(tasksRunningSubtext.getFont().deriveFont(Font.PLAIN, 11f));
        tasksRunningSubtext.setForeground(UIHelper.getSecondaryTextColor());
        tasksRunningSubtext.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(tasksRunningValue, BorderLayout.CENTER);
        card.add(tasksRunningSubtext, BorderLayout.SOUTH);

        return card;
    }

    private void updateMonitoringCards() {
        int active = threadManager.threadPoolManager().getActiveThreadCount();
        int total = threadManager.threadPoolManager().getPoolSize();
        int pct = total > 0 ? (active * 100 / total) : 0;

        threadUsageValue.setText(active + " / " + total);
        threadUsageBar.setValue(pct);
        applyThreadUsageColor(pct);

        int rpm = threadManager.highLoadDetector().getRequestsInLastMinute();
        boolean highLoad = threadManager.highLoadDetector().isHighLoad();
        currentLoadValue.setText(rpm + " req/min");
        currentLoadStatus.setText(highLoad ? "HIGH LOAD" : "Normal");
        currentLoadStatus.setForeground(highLoad ? UIHelper.getErrorColor() : UIHelper.getSuccessColor());

        tasksRunningValue.setText(String.valueOf(active));
        tasksRunningSubtext.setText("active");
    }

    private void applyThreadUsageColor(int pct) {
        Color color;
        if (pct < 60) {
            color = UIHelper.getSuccessColor();
        } else if (pct <= 85) {
            color = UIHelper.getWarningColor();
        } else {
            color = UIHelper.getErrorColor();
        }
        threadUsageBar.setForeground(color);
    }

    // ---- Settings Sections (CENTER) ----

    private JScrollPane createSettingsSections() {
        JPanel sectionsPanel = new JPanel();
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));

        sectionsPanel.add(createPresetsSection());
        sectionsPanel.add(Box.createVerticalStrut(8));
        sectionsPanel.add(createPerformanceSection());
        sectionsPanel.add(Box.createVerticalStrut(8));
        sectionsPanel.add(createSolvingSection());
        sectionsPanel.add(Box.createVerticalStrut(8));
        sectionsPanel.add(createDetectionSection());
        sectionsPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(sectionsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createPresetsSection() {
        JPanel section = new JPanel(new BorderLayout(8, 4));
        section.setBorder(createSectionBorder("Presets"));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inner.add(new JLabel("Configuration Preset:"));
        inner.add(presetCombo);

        section.add(inner, BorderLayout.CENTER);
        return section;
    }

    private JPanel createPerformanceSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(createSectionBorder("Performance"));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        // Thread Pool Size
        addSettingRow(section, c, 0, "Thread Pool Size:", threadPoolSizeSpinner,
                "Number of concurrent CAPTCHA solving threads");

        // High Load Threshold
        addSettingRow(section, c, 1, "High Load Threshold:", highLoadThresholdSpinner,
                "Requests per minute to trigger high load mode");

        // Track changes for preset detection and unsaved indicator
        threadPoolSizeSpinner.addChangeListener(e -> {
            markUnsaved();
            if (!updatingFromPreset) {
                presetCombo.setSelectedItem("Custom");
            }
        });
        highLoadThresholdSpinner.addChangeListener(e -> {
            markUnsaved();
            if (!updatingFromPreset) {
                presetCombo.setSelectedItem("Custom");
            }
        });

        return section;
    }

    private JPanel createSolvingSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(createSectionBorder("Solving"));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        addSettingRow(section, c, 0, "Solve Timeout (sec):", solveTimeoutSpinner,
                "Maximum time to wait for CAPTCHA solution");

        addSettingRow(section, c, 1, "Max Retries:", maxRetriesSpinner,
                "Number of retry attempts on solve failure");

        solveTimeoutSpinner.addChangeListener(e -> markUnsaved());
        maxRetriesSpinner.addChangeListener(e -> markUnsaved());

        return section;
    }

    private JPanel createDetectionSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(createSectionBorder("Detection"));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        // Auto-Detection checkbox
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        section.add(autoDetectionCheckbox, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        JLabel descLabel = new JLabel("Automatically detect CAPTCHAs in HTTP responses");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 10f));
        descLabel.setForeground(UIHelper.getSecondaryTextColor());
        section.add(descLabel, c);

        // Log Level
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        section.add(new JLabel("Log Level:"), c);

        c.gridx = 1;
        c.gridy = 2;
        section.add(logLevelCombo, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        JLabel logDesc = new JLabel("Minimum log level to display");
        logDesc.setFont(logDesc.getFont().deriveFont(Font.PLAIN, 10f));
        logDesc.setForeground(UIHelper.getSecondaryTextColor());
        section.add(logDesc, c);

        // Fill remaining space
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        section.add(Box.createHorizontalGlue(), c);

        autoDetectionCheckbox.addActionListener(e -> markUnsaved());
        logLevelCombo.addActionListener(e -> markUnsaved());

        return section;
    }

    // ---- Button Panel (SOUTH) ----

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));

        JButton saveButton = new JButton("Save Settings");
        unsavedLabel = new JLabel("");
        unsavedLabel.setForeground(UIHelper.getWarningColor());

        JButton resetButton = new JButton("Reset to Defaults");

        JButton cancelAllButton = new JButton("Cancel All Tasks");
        cancelAllButton.setForeground(UIHelper.getErrorColor());
        cancelAllButton.setToolTipText("Cancels all current CAPTCHA solving tasks");

        buttonPanel.add(saveButton);
        buttonPanel.add(unsavedLabel);
        buttonPanel.add(resetButton);
        buttonPanel.add(cancelAllButton);

        saveButton.addActionListener(e -> {
            saveSettings();
            unsavedLabel.setText("");
            JOptionPane.showMessageDialog(UIHelper.getBurpFrame(), "Settings saved",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
        });

        resetButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(UIHelper.getBurpFrame(),
                    "Reset all settings to default values?",
                    "Reset to Defaults", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                resetToDefaults();
            }
        });

        cancelAllButton.addActionListener(e -> cancelAllTasks());

        return buttonPanel;
    }

    // ---- Helpers ----

    private JPanel createCardPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.getBorderColor()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return card;
    }

    private JLabel createCardTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(UIHelper.getSecondaryTextColor());
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private TitledBorder createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIHelper.getBorderColor()), title);
    }

    private void addSettingRow(JPanel panel, GridBagConstraints c, int row,
                               String labelText, JComponent control, String description) {
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;

        c.gridx = 0;
        c.gridy = row * 2;
        panel.add(new JLabel(labelText), c);

        c.gridx = 1;
        panel.add(control, c);

        c.gridx = 0;
        c.gridy = row * 2 + 1;
        c.gridwidth = 2;
        JLabel desc = new JLabel(description);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 10f));
        desc.setForeground(UIHelper.getSecondaryTextColor());
        panel.add(desc, c);

        // Spacer column to push content left
        c.gridx = 2;
        c.gridy = row * 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
    }

    private void markUnsaved() {
        if (unsavedLabel != null) {
            unsavedLabel.setText("Unsaved changes");
        }
    }

    // ---- Presets ----

    private String detectCurrentPreset() {
        int poolSize = settingsManager.getThreadPoolSize();
        int threshold = settingsManager.getHighLoadThreshold();

        if (poolSize == 5 && threshold == 20) return "Low Load (5 threads)";
        if (poolSize == 10 && threshold == 50) return "Balanced (10 threads)";
        if (poolSize == 30 && threshold == 100) return "High Performance (30 threads)";
        return "Custom";
    }

    private void applyPreset() {
        String preset = (String) presetCombo.getSelectedItem();
        if (preset == null || "Custom".equals(preset)) return;

        updatingFromPreset = true;
        try {
            switch (preset) {
                case "Low Load (5 threads)" -> {
                    threadPoolSizeSpinner.setValue(5);
                    highLoadThresholdSpinner.setValue(20);
                }
                case "Balanced (10 threads)" -> {
                    threadPoolSizeSpinner.setValue(10);
                    highLoadThresholdSpinner.setValue(50);
                }
                case "High Performance (30 threads)" -> {
                    threadPoolSizeSpinner.setValue(30);
                    highLoadThresholdSpinner.setValue(100);
                }
                default -> { /* Custom or unknown preset - no action */ }
            }
            markUnsaved();
        } finally {
            updatingFromPreset = false;
        }
    }

    // ---- Save / Reset / Cancel ----

    private void saveSettings() {
        settingsManager.setThreadPoolSize((Integer) threadPoolSizeSpinner.getValue());
        settingsManager.setHighLoadThreshold((Integer) highLoadThresholdSpinner.getValue());
        settingsManager.setSolveTimeout((Integer) solveTimeoutSpinner.getValue());
        settingsManager.setMaxRetries((Integer) maxRetriesSpinner.getValue());
        settingsManager.setAutoDetectionEnabled(autoDetectionCheckbox.isSelected());
        settingsManager.setLogLevel((String) logLevelCombo.getSelectedItem());

        // Apply log level immediately
        try {
            LoggerService.LogLevel level = LoggerService.LogLevel.valueOf(
                    (String) logLevelCombo.getSelectedItem());
            LoggerService.getInstance().setMinLevel(level);
        } catch (IllegalArgumentException ignored) {
            // Invalid level name, keep current
        }
    }

    private void resetToDefaults() {
        threadPoolSizeSpinner.setValue(10);
        highLoadThresholdSpinner.setValue(50);
        solveTimeoutSpinner.setValue(120);
        maxRetriesSpinner.setValue(2);
        autoDetectionCheckbox.setSelected(true);
        logLevelCombo.setSelectedItem("INFO");
        presetCombo.setSelectedItem("Balanced (10 threads)");

        saveSettings();
        unsavedLabel.setText("");
        JOptionPane.showMessageDialog(UIHelper.getBurpFrame(), "Settings reset to defaults",
                "Reset", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelAllTasks() {
        try {
            int cancelled = threadManager.cancelAllTasks();

            if (cancelled > 0) {
                JOptionPane.showMessageDialog(
                        UIHelper.getBurpFrame(),
                        "Cancelled " + cancelled + " tasks",
                        "Task Cancellation",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        UIHelper.getBurpFrame(),
                        "No active tasks to cancel",
                        "Task Cancellation",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    UIHelper.getBurpFrame(),
                    "Error cancelling tasks: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
