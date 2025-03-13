package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.stats.StatisticsCollector;

/**
 * Panel for statistics
 */
public class StatisticsPanel extends JPanel {
    private final StatisticsCollector statisticsCollector;

    private JLabel totalAttemptsLabel;
    private JLabel successRateLabel;
    private JLabel avgTimeLabel;
    private JTable typeStatsTable;
    private TypeStatsTableModel typeStatsTableModel;

    public StatisticsPanel(StatisticsCollector statisticsCollector) {
        this.statisticsCollector = statisticsCollector;

        setLayout(new BorderLayout());

        // Create summary panel
        JPanel summaryPanel = new JPanel(new GridLayout(3, 1));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));

        totalAttemptsLabel = new JLabel("Total Attempts: 0 (0 successful)");
        successRateLabel = new JLabel("Success Rate: 0%");
        avgTimeLabel = new JLabel("Average Solving Time: 0 ms");

        summaryPanel.add(totalAttemptsLabel);
        summaryPanel.add(successRateLabel);
        summaryPanel.add(avgTimeLabel);

        // Create type statistics panel
        JPanel typeStatsPanel = new JPanel(new BorderLayout());
        typeStatsPanel.setBorder(BorderFactory.createTitledBorder("CAPTCHA Type Statistics"));

        typeStatsTableModel = new TypeStatsTableModel();
        typeStatsTable = new JTable(typeStatsTableModel);

        JScrollPane typeStatsScrollPane = new JScrollPane(typeStatsTable);
        typeStatsPanel.add(typeStatsScrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton resetButton = new JButton("Reset Statistics");
        JButton refreshButton = new JButton("Refresh");

        buttonPanel.add(resetButton);
        buttonPanel.add(refreshButton);

        resetButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to reset all statistics?",
                    "Reset Statistics",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                statisticsCollector.reset();
                refreshData();
            }
        });

        refreshButton.addActionListener(e -> refreshData());

        // Add panels to main panel
        add(summaryPanel, BorderLayout.NORTH);
        add(typeStatsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Initial data refresh
        refreshData();
    }

    /**
     * Refresh statistics data
     */
    public void refreshData() {
        // Update summary labels
        int totalAttempts = statisticsCollector.getTotalAttempts();
        int successfulAttempts = statisticsCollector.getSuccessfulAttempts();
        double successRate = statisticsCollector.getSuccessRate() * 100;
        double avgTime = statisticsCollector.getAverageSolvingTimeMs();

        totalAttemptsLabel.setText(String.format("Total Attempts: %d (%d successful)",
                totalAttempts, successfulAttempts));
        successRateLabel.setText(String.format("Success Rate: %.1f%%", successRate));
        avgTimeLabel.setText(String.format("Average Solving Time: %.0f ms", avgTime));

        // Update type statistics table
        typeStatsTableModel.refreshData();
    }

    /**
     * Table model for CAPTCHA type statistics
     */
    private class TypeStatsTableModel extends AbstractTableModel {
        private final String[] columnNames = {"CAPTCHA Type", "Attempts", "Successful", "Success Rate", "Avg. Time (ms)"};
        private final List<TypeStats> stats = new ArrayList<>();

        @Override
        public int getRowCount() {
            return stats.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TypeStats stat = stats.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> stat.type;
                case 1 -> stat.attempts;
                case 2 -> stat.successful;
                case 3 -> String.format("%.1f%%", stat.successRate * 100);
                case 4 -> String.format("%.0f", stat.avgTimeMs);
                default -> null;
            };
        }

        /**
         * Refresh table data
         */
        public void refreshData() {
            stats.clear();

            Map<CaptchaType, StatisticsCollector.TypeStats> typeStats = statisticsCollector.getTypeStats();
            for (Map.Entry<CaptchaType, StatisticsCollector.TypeStats> entry : typeStats.entrySet()) {
                CaptchaType type = entry.getKey();
                StatisticsCollector.TypeStats typeStat = entry.getValue();

                stats.add(new TypeStats(
                        type,
                        typeStat.getAttempts(),
                        typeStat.getSuccessful(),
                        typeStat.getSuccessRate(),
                        typeStat.getAverageSolvingTimeMs()
                ));
            }

            fireTableDataChanged();
        }

        /**
         * Type statistics for table
         */
        private class TypeStats {
            private final CaptchaType type;
            private final int attempts;
            private final int successful;
            private final double successRate;
            private final double avgTimeMs;

            public TypeStats(CaptchaType type, int attempts, int successful, double successRate, double avgTimeMs) {
                this.type = type;
                this.attempts = attempts;
                this.successful = successful;
                this.successRate = successRate;
                this.avgTimeMs = avgTimeMs;
            }
        }
    }
}