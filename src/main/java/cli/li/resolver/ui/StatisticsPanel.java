package cli.li.resolver.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import cli.li.resolver.captcha.model.CaptchaType;
import cli.li.resolver.provider.ProviderService;
import cli.li.resolver.provider.ProviderStatistics;
import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.stats.StatisticsCollector;
import cli.li.resolver.ui.renderer.SuccessRateCellRenderer;

/**
 * Panel for statistics with visual cards and color-coded table.
 */
public class StatisticsPanel extends JPanel {
    private final StatisticsCollector statisticsCollector;
    private final ServiceManager serviceManager;

    // Summary card components
    private JLabel totalAttemptsValue;
    private JLabel totalAttemptsSubtext;
    private JLabel successRateValue;
    private JProgressBar successRateBar;
    private JLabel avgTimeValue;
    private JLabel avgTimeSubtext;

    private JTable typeStatsTable;
    private TypeStatsTableModel typeStatsTableModel;

    private JTable providerStatsTable;
    private ProviderStatsTableModel providerStatsTableModel;

    public StatisticsPanel(StatisticsCollector statisticsCollector, ServiceManager serviceManager) {
        this.statisticsCollector = statisticsCollector;
        this.serviceManager = serviceManager;

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        summaryPanel.add(createTotalAttemptsCard());
        summaryPanel.add(createSuccessRateCard());
        summaryPanel.add(createAvgTimeCard());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                createTypeStatsPanel(), createProviderStatsPanel());
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);

        add(summaryPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshData();
    }

    private JPanel createTypeStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("CAPTCHA Type Statistics"));

        typeStatsTableModel = new TypeStatsTableModel();
        typeStatsTable = new JTable(typeStatsTableModel);

        typeStatsTable.getColumnModel().getColumn(3).setCellRenderer(new SuccessRateCellRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        typeStatsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        typeStatsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        typeStatsTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        panel.add(new JScrollPane(typeStatsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProviderStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Provider Statistics"));

        providerStatsTableModel = new ProviderStatsTableModel();
        providerStatsTable = new JTable(providerStatsTableModel);

        providerStatsTable.getColumnModel().getColumn(4).setCellRenderer(new SuccessRateCellRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        providerStatsTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        providerStatsTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        providerStatsTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        providerStatsTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        panel.add(new JScrollPane(providerStatsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        JButton resetButton = new JButton("Reset Statistics");
        JButton refreshButton = new JButton("Refresh");

        buttonPanel.add(resetButton);
        buttonPanel.add(refreshButton);

        resetButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(UIHelper.getBurpFrame(),
                    "Are you sure you want to reset all statistics?",
                    "Reset Statistics",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                statisticsCollector.reset();
                refreshData();
            }
        });

        refreshButton.addActionListener(e -> refreshData());
        return buttonPanel;
    }

    /**
     * Create card for Total Attempts.
     */
    private JPanel createTotalAttemptsCard() {
        JPanel card = createCardPanel();

        JLabel titleLabel = new JLabel("Total Attempts");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        titleLabel.setForeground(UIHelper.getSecondaryTextColor());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        totalAttemptsValue = new JLabel("0");
        totalAttemptsValue.setFont(totalAttemptsValue.getFont().deriveFont(Font.BOLD, 24f));
        totalAttemptsValue.setHorizontalAlignment(SwingConstants.CENTER);

        totalAttemptsSubtext = new JLabel("0 successful");
        totalAttemptsSubtext.setFont(totalAttemptsSubtext.getFont().deriveFont(Font.PLAIN, 11f));
        totalAttemptsSubtext.setForeground(UIHelper.getSecondaryTextColor());
        totalAttemptsSubtext.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(totalAttemptsValue, BorderLayout.CENTER);
        card.add(totalAttemptsSubtext, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Create card for Success Rate with a progress bar.
     */
    private JPanel createSuccessRateCard() {
        JPanel card = createCardPanel();

        JLabel titleLabel = new JLabel("Success Rate");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        titleLabel.setForeground(UIHelper.getSecondaryTextColor());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        successRateValue = new JLabel("0.0%");
        successRateValue.setFont(successRateValue.getFont().deriveFont(Font.BOLD, 24f));
        successRateValue.setHorizontalAlignment(SwingConstants.CENTER);

        successRateBar = new JProgressBar(0, 100);
        successRateBar.setValue(0);
        successRateBar.setStringPainted(false);
        successRateBar.setPreferredSize(new Dimension(0, 8));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 0, 12));
        bottomPanel.add(successRateBar, BorderLayout.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(successRateValue, BorderLayout.CENTER);
        card.add(bottomPanel, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Create card for Average Solving Time.
     */
    private JPanel createAvgTimeCard() {
        JPanel card = createCardPanel();

        JLabel titleLabel = new JLabel("Avg Solving Time");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 11f));
        titleLabel.setForeground(UIHelper.getSecondaryTextColor());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        avgTimeValue = new JLabel("0 ms");
        avgTimeValue.setFont(avgTimeValue.getFont().deriveFont(Font.BOLD, 24f));
        avgTimeValue.setHorizontalAlignment(SwingConstants.CENTER);

        avgTimeSubtext = new JLabel("per solve");
        avgTimeSubtext.setFont(avgTimeSubtext.getFont().deriveFont(Font.PLAIN, 11f));
        avgTimeSubtext.setForeground(UIHelper.getSecondaryTextColor());
        avgTimeSubtext.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(avgTimeValue, BorderLayout.CENTER);
        card.add(avgTimeSubtext, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Create a styled card panel with border and padding.
     */
    private JPanel createCardPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.getBorderColor()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return card;
    }

    /**
     * Refresh statistics data.
     */
    public void refreshData() {
        int totalAttempts = statisticsCollector.getTotalAttempts();
        int successfulAttempts = statisticsCollector.getSuccessfulAttempts();
        double successRate = statisticsCollector.getSuccessRate() * 100;
        double avgTime = statisticsCollector.getAverageSolvingTimeMs();

        // Update Total Attempts card
        totalAttemptsValue.setText(String.valueOf(totalAttempts));
        totalAttemptsSubtext.setText(successfulAttempts + " successful");

        // Update Success Rate card
        if (totalAttempts == 0) {
            successRateValue.setText("N/A");
            successRateValue.setForeground(UIHelper.getSecondaryTextColor());
            successRateBar.setValue(0);
            successRateBar.setForeground(UIHelper.getSecondaryTextColor());
        } else {
            successRateValue.setText(String.format("%.1f%%", successRate));
            int rateInt = (int) Math.round(successRate);
            successRateBar.setValue(rateInt);

            // Color code the progress bar and value
            Color rateColor;
            if (successRate >= 70.0) {
                rateColor = UIHelper.getSuccessColor();
            } else if (successRate >= 40.0) {
                rateColor = UIHelper.getWarningColor();
            } else {
                rateColor = UIHelper.getErrorColor();
            }
            successRateValue.setForeground(rateColor);
            successRateBar.setForeground(rateColor);
        }

        // Update Avg Time card
        if (totalAttempts == 0) {
            avgTimeValue.setText("N/A");
            avgTimeValue.setForeground(UIHelper.getSecondaryTextColor());
        } else {
            avgTimeValue.setForeground(UIHelper.getTextColor());
            if (avgTime >= 1000) {
                avgTimeValue.setText(String.format("%.1f s", avgTime / 1000.0));
            } else {
                avgTimeValue.setText(String.format("%.0f ms", avgTime));
            }
        }

        // Update type statistics table
        typeStatsTableModel.refreshData();

        // Update provider statistics table
        providerStatsTableModel.refreshData();
    }

    /**
     * Table model for CAPTCHA type statistics.
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
                case 0 -> stat.type.getDisplayName();
                case 1 -> stat.attempts;
                case 2 -> stat.successful;
                case 3 -> String.format("%.1f%%", stat.successRate * 100);
                case 4 -> String.format("%.0f", stat.avgTimeMs);
                default -> null;
            };
        }

        /**
         * Refresh table data.
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

        private class TypeStats {
            private final CaptchaType type;
            private final int attempts;
            private final int successful;
            private final double successRate;
            private final double avgTimeMs;

            TypeStats(CaptchaType type, int attempts, int successful, double successRate, double avgTimeMs) {
                this.type = type;
                this.attempts = attempts;
                this.successful = successful;
                this.successRate = successRate;
                this.avgTimeMs = avgTimeMs;
            }
        }
    }

    /**
     * Table model for per-provider statistics.
     */
    private class ProviderStatsTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Provider", "Requests", "Successful", "Failed", "Success Rate", "Avg Time (ms)"};
        private final List<ProviderStatsRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            ProviderStatsRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.providerName;
                case 1 -> row.totalRequests;
                case 2 -> row.successful;
                case 3 -> row.failed;
                case 4 -> row.totalRequests == 0 ? "N/A" : String.format("%.1f%%", row.successRate);
                case 5 -> row.totalRequests == 0 ? "N/A" : String.format("%.0f", row.avgTimeMs);
                default -> null;
            };
        }

        /**
         * Refresh table data from service manager.
         */
        public void refreshData() {
            rows.clear();

            for (ProviderService ps : serviceManager.getAllProviderServices()) {
                ProviderStatistics stats = ps.getStatistics();
                rows.add(new ProviderStatsRow(
                        ps.getDisplayName(),
                        stats.getTotalRequests(),
                        stats.getSuccessfulRequests(),
                        stats.getFailedRequests(),
                        stats.getSuccessRate(),
                        stats.getAvgSolveTimeMs()
                ));
            }

            fireTableDataChanged();
        }

        private class ProviderStatsRow {
            private final String providerName;
            private final int totalRequests;
            private final int successful;
            private final int failed;
            private final double successRate;
            private final double avgTimeMs;

            ProviderStatsRow(String providerName, int totalRequests, int successful, int failed,
                             double successRate, double avgTimeMs) {
                this.providerName = providerName;
                this.totalRequests = totalRequests;
                this.successful = successful;
                this.failed = failed;
                this.successRate = successRate;
                this.avgTimeMs = avgTimeMs;
            }
        }
    }
}
