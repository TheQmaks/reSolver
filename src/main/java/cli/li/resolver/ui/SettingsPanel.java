package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;

import cli.li.resolver.settings.SettingsManager;
import cli.li.resolver.thread.CaptchaSolverThreadManager;

/**
 * Panel for extension settings
 */
public class SettingsPanel extends JPanel {
    private final SettingsManager settingsManager;
    private final CaptchaSolverThreadManager threadManager;

    private JSpinner threadPoolSizeSpinner;
    private JSpinner queueSizeSpinner;
    private JSpinner highLoadThresholdSpinner;

    public SettingsPanel(SettingsManager settingsManager, CaptchaSolverThreadManager threadManager) {
        this.settingsManager = settingsManager;
        this.threadManager = threadManager;

        setLayout(new BorderLayout());

        // Create settings panel
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        // Thread pool size
        c.gridx = 0;
        c.gridy = 0;
        settingsPanel.add(new JLabel("Thread Pool Size:"), c);

        c.gridx = 1;
        threadPoolSizeSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getThreadPoolSize(), 1, 100, 1));
        settingsPanel.add(threadPoolSizeSpinner, c);

        // Queue size
        c.gridx = 0;
        c.gridy = 1;
        settingsPanel.add(new JLabel("Queue Size:"), c);

        c.gridx = 1;
        queueSizeSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getQueueSize(), 1, 1000, 10));
        settingsPanel.add(queueSizeSpinner, c);

        // High load threshold
        c.gridx = 0;
        c.gridy = 2;
        settingsPanel.add(new JLabel("High Load Threshold (requests/min):"), c);

        c.gridx = 1;
        highLoadThresholdSpinner = new JSpinner(new SpinnerNumberModel(
                settingsManager.getHighLoadThreshold(), 1, 1000, 10));
        settingsPanel.add(highLoadThresholdSpinner, c);

        // Thread monitoring panel
        JPanel monitoringPanel = new JPanel(new BorderLayout());
        monitoringPanel.setBorder(BorderFactory.createTitledBorder("Thread Monitoring"));

        // Active threads label
        JLabel activeThreadsLabel = new JLabel("Active Threads: 0/" + threadManager.threadPoolManager().getPoolSize());
        monitoringPanel.add(activeThreadsLabel, BorderLayout.NORTH);

        // Queue status label
        JLabel queueStatusLabel = new JLabel("Queue Status: 0 items in queue");
        monitoringPanel.add(queueStatusLabel, BorderLayout.CENTER);

        // Load status label
        JLabel loadStatusLabel = new JLabel("Current Load: 0 requests/min");
        monitoringPanel.add(loadStatusLabel, BorderLayout.SOUTH);

        // Start monitoring timer
        Timer timer = new Timer(1000, e -> {
            activeThreadsLabel.setText("Active Threads: " +
                    threadManager.threadPoolManager().getActiveThreadCount() + "/" +
                    threadManager.threadPoolManager().getPoolSize());

            queueStatusLabel.setText("Queue Status: " +
                    threadManager.queueManager().getQueueSize() + " items in queue");

            loadStatusLabel.setText("Current Load: " +
                    threadManager.highLoadDetector().getRequestsInLastMinute() + " requests/min" +
                    (threadManager.highLoadDetector().isHighLoad() ? " (HIGH LOAD)" : ""));
        });
        timer.start();

        // Button panel
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save Settings");
        buttonPanel.add(saveButton);

        saveButton.addActionListener(e -> {
            saveSettings();
            JOptionPane.showMessageDialog(this, "Settings saved", "Save", JOptionPane.INFORMATION_MESSAGE);
        });

        // Add panels to main panel
        add(settingsPanel, BorderLayout.NORTH);
        add(monitoringPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Save settings
     */
    private void saveSettings() {
        settingsManager.setThreadPoolSize((Integer) threadPoolSizeSpinner.getValue());
        settingsManager.setQueueSize((Integer) queueSizeSpinner.getValue());
        settingsManager.setHighLoadThreshold((Integer) highLoadThresholdSpinner.getValue());
    }
}