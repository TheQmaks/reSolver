package cli.li.resolver.ui;

import javax.swing.*;
import java.awt.*;

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

        // Create panel for thread status information
        JPanel threadStatusPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        // Active threads label
        JLabel activeThreadsLabel = new JLabel("Active Threads: 0/" + threadManager.threadPoolManager().getPoolSize());
        threadStatusPanel.add(activeThreadsLabel);

        // Queue status label
        JLabel queueStatusLabel = new JLabel("Queue Status: 0 items in queue");
        threadStatusPanel.add(queueStatusLabel);

        // Load status label
        JLabel loadStatusLabel = new JLabel("Current Load: 0 requests/min");
        threadStatusPanel.add(loadStatusLabel);
        
        monitoringPanel.add(threadStatusPanel, BorderLayout.CENTER);
        
        // Add cancel button directly to monitoring panel
        JPanel threadControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelAllTasksButton = new JButton("Cancel All Tasks");
        cancelAllTasksButton.setToolTipText("Cancels all current CAPTCHA solving tasks");
        cancelAllTasksButton.addActionListener(e -> cancelAllTasks());
        threadControlPanel.add(cancelAllTasksButton);
        
        monitoringPanel.add(threadControlPanel, BorderLayout.SOUTH);

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
            JOptionPane.showMessageDialog(UIHelper.getBurpFrame(), "Settings saved", "Save", JOptionPane.INFORMATION_MESSAGE);
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
    
    /**
     * Cancel all current CAPTCHA solving tasks
     */
    private void cancelAllTasks() {
        try {
            int cancelled = threadManager.cancelAllTasks();
            
            if (cancelled > 0) {
                String message = "Cancelled " + cancelled + " tasks";
                JOptionPane.showMessageDialog(
                    UIHelper.getBurpFrame(),
                    message,
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