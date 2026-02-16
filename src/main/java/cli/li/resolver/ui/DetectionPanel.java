package cli.li.resolver.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cli.li.resolver.detection.DetectedCaptcha;
import cli.li.resolver.detection.DetectionStore;
import cli.li.resolver.ui.renderer.CaptchaTypeCellRenderer;

/**
 * Panel for displaying auto-detected CAPTCHAs found in HTTP responses.
 * Shows a toolbar with filter and counter, a table with color-coded type column,
 * and action buttons for copy/delete operations.
 */
public class DetectionPanel extends BasePanel {
    private final DetectionStore detectionStore;
    private final JTable detectionTable;
    private final DetectionTableModel tableModel;
    private final JComboBox<String> filterCombo;
    private final JLabel counterLabel;
    private final JLabel statusLabel;
    private final Timer statusClearTimer;
    private final Consumer<DetectedCaptcha> storeListener;

    private static final String FILTER_ALL = "All";
    private static final String[] FILTER_OPTIONS = {
            FILTER_ALL, "reCAPTCHA v2", "reCAPTCHA v3", "hCaptcha",
            "Turnstile", "FunCaptcha", "GeeTest", "GeeTest v4", "AWS WAF",
            "MTCaptcha", "Lemin Cropped", "KeyCaptcha",
            "Friendly Captcha", "Yandex SmartCaptcha", "Tencent Captcha",
            "CaptchaFox", "Procaptcha"
    };

    private static final String[] FILTER_CODES = {
            null, "recaptchav2", "recaptchav3", "hcaptcha",
            "turnstile", "funcaptcha", "geetest", "geetestv4", "awswaf",
            "mtcaptcha", "lemin", "keycaptcha",
            "friendlycaptcha", "yandex", "tencent",
            "captchafox", "procaptcha"
    };

    private static final String[] COLUMN_HEADER_TOOLTIPS = {
            "The URL of the page where the CAPTCHA was detected",
            "The type of CAPTCHA detected (e.g. reCAPTCHA v2, hCaptcha)",
            "The site key used by the CAPTCHA on this page",
            "The date and time when the CAPTCHA was detected",
            "The placeholder string to use in requests for solving"
    };

    public DetectionPanel(DetectionStore detectionStore) {
        this.detectionStore = detectionStore;
        setLayout(new BorderLayout(0, 4));

        statusLabel = new JLabel(" ");
        statusClearTimer = createTimer(2000, e -> {
            statusLabel.setText(" ");
            ((Timer) e.getSource()).stop();
        });
        statusClearTimer.setRepeats(false);

        tableModel = new DetectionTableModel();
        detectionTable = createDetectionTable();
        filterCombo = new JComboBox<>(FILTER_OPTIONS);
        filterCombo.addActionListener(e -> applyFilter());
        counterLabel = new JLabel("0 detections");
        counterLabel.setForeground(UIHelper.getSecondaryTextColor());

        add(createToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(detectionTable), BorderLayout.CENTER);
        add(createSouthPanel(), BorderLayout.SOUTH);

        storeListener = captcha ->
                SwingUtilities.invokeLater(() -> {
                    tableModel.refreshData();
                    applyFilter();
                });
        detectionStore.addListener(storeListener);

        tableModel.refreshData();
        applyFilter();
    }

    private JTable createDetectionTable() {
        JTable table = new JTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row >= 0 && row < getRowCount() && (col == 0 || col == 2 || col == 4)) {
                    Object value = getValueAt(row, col);
                    if (value != null) {
                        String text = value.toString();
                        if (!text.isEmpty()) {
                            return text;
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(400);

        table.getColumnModel().getColumn(1).setCellRenderer(new CaptchaTypeCellRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setDefaultRenderer(new ToolTipHeaderRenderer(tableHeader.getDefaultRenderer()));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < tableModel.getRowCount()) {
                        String placeholder = (String) tableModel.getValueAt(row, 4);
                        if (placeholder != null && !placeholder.isEmpty()) {
                            copyToClipboard(placeholder);
                            showStatus("Copied!");
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }

            private void handlePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < tableModel.getRowCount()) {
                        table.setRowSelectionInterval(row, row);
                        showContextMenu(e, row);
                    }
                }
            }
        });

        return table;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.add(new JLabel("Filter:"));
        toolbar.add(filterCombo);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(counterLabel);
        return toolbar;
    }

    private JPanel createSouthPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));

        JButton copyPlaceholderButton = new JButton("Copy Placeholder");
        copyPlaceholderButton.setToolTipText("Copy the selected row's placeholder to the clipboard");
        copyPlaceholderButton.addActionListener(e -> copySelectedPlaceholder());

        JButton copyAllButton = new JButton("Copy All Placeholders");
        copyAllButton.setToolTipText("Copy all visible placeholders to the clipboard");
        copyAllButton.addActionListener(e -> copyAllPlaceholders());

        JButton deleteSelectedButton = new JButton("Delete Selected");
        deleteSelectedButton.setToolTipText("Delete the selected detection");
        deleteSelectedButton.addActionListener(e -> deleteSelected());

        JButton clearAllButton = new JButton("Clear All");
        clearAllButton.setToolTipText("Clear all detected CAPTCHAs");
        clearAllButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(UIHelper.getBurpFrame(),
                    "Are you sure you want to clear all detections?",
                    "Clear All Detections",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                detectionStore.clear();
                tableModel.refreshData();
                applyFilter();
            }
        });

        buttonPanel.add(copyPlaceholderButton);
        buttonPanel.add(copyAllButton);
        buttonPanel.add(deleteSelectedButton);
        buttonPanel.add(clearAllButton);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusPanel.add(statusLabel);

        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(statusPanel, BorderLayout.SOUTH);
        return southPanel;
    }

    /**
     * Show a temporary status message that auto-clears after 2 seconds.
     */
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusClearTimer.restart();
    }

    /**
     * Show a right-click context menu for the given row.
     */
    private void showContextMenu(MouseEvent e, int row) {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem copyPlaceholderItem = new JMenuItem("Copy Placeholder");
        copyPlaceholderItem.addActionListener(ev -> {
            String placeholder = (String) tableModel.getValueAt(row, 4);
            if (placeholder != null && !placeholder.isEmpty()) {
                copyToClipboard(placeholder);
                showStatus("Copied!");
            }
        });

        JMenuItem copySiteKeyItem = new JMenuItem("Copy Site Key");
        copySiteKeyItem.addActionListener(ev -> {
            String siteKey = (String) tableModel.getValueAt(row, 2);
            if (siteKey != null && !siteKey.isEmpty()) {
                copyToClipboard(siteKey);
                showStatus("Copied!");
            }
        });

        JMenuItem copyUrlItem = new JMenuItem("Copy URL");
        copyUrlItem.addActionListener(ev -> {
            String url = (String) tableModel.getValueAt(row, 0);
            if (url != null && !url.isEmpty()) {
                copyToClipboard(url);
                showStatus("Copied!");
            }
        });

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(ev -> {
            DetectedCaptcha captcha = tableModel.getDetectionAt(row);
            if (captcha != null) {
                detectionStore.remove(captcha);
                tableModel.refreshData();
                applyFilter();
                showStatus("Deleted.");
            }
        });

        contextMenu.add(copyPlaceholderItem);
        contextMenu.add(copySiteKeyItem);
        contextMenu.add(copyUrlItem);
        contextMenu.addSeparator();
        contextMenu.add(deleteItem);

        contextMenu.show(detectionTable, e.getX(), e.getY());
    }

    /**
     * Apply the current filter selection and update counter.
     */
    private void applyFilter() {
        int selectedIndex = filterCombo.getSelectedIndex();
        String filterCode = (selectedIndex >= 0 && selectedIndex < FILTER_CODES.length) ? FILTER_CODES[selectedIndex] : null;
        tableModel.applyFilter(filterCode);

        int count = tableModel.getRowCount();
        int total = tableModel.getTotalCount();
        if (filterCode == null) {
            counterLabel.setText(total + " detections");
        } else {
            counterLabel.setText(count + " of " + total + " detections");
        }
    }

    /**
     * Copy the placeholder from the currently selected row to the system clipboard.
     */
    private void copySelectedPlaceholder() {
        int selectedRow = detectionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            showStatus("No selection.");
            return;
        }

        String placeholder = (String) tableModel.getValueAt(selectedRow, 4);
        if (placeholder != null && !placeholder.isEmpty()) {
            copyToClipboard(placeholder);
            showStatus("Copied!");
        }
    }

    /**
     * Copy all visible placeholders to the clipboard, one per line.
     */
    private void copyAllPlaceholders() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            showStatus("No detections to copy.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            String placeholder = (String) tableModel.getValueAt(i, 4);
            if (placeholder != null && !placeholder.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(placeholder);
            }
        }

        copyToClipboard(sb.toString());
        showStatus(rowCount + " placeholder(s) copied.");
    }

    /**
     * Delete the selected detection from the store.
     */
    private void deleteSelected() {
        int selectedRow = detectionTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            showStatus("No selection.");
            return;
        }

        DetectedCaptcha captcha = tableModel.getDetectionAt(selectedRow);
        if (captcha != null) {
            detectionStore.remove(captcha);
            tableModel.refreshData();
            applyFilter();
            showStatus("Deleted.");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        detectionStore.removeListener(storeListener);
    }

    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    /**
     * Header renderer that adds tooltips to column headers.
     */
    private static class ToolTipHeaderRenderer implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer delegate;

        ToolTipHeaderRenderer(javax.swing.table.TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (comp instanceof JComponent) {
                int modelColumn = table.convertColumnIndexToModel(column);
                if (modelColumn >= 0 && modelColumn < COLUMN_HEADER_TOOLTIPS.length) {
                    ((JComponent) comp).setToolTipText(COLUMN_HEADER_TOOLTIPS[modelColumn]);
                }
            }
            return comp;
        }
    }

    /**
     * Table model for the detected CAPTCHAs table with filtering support.
     * Columns: URL, Type, Site Key, Detected At, Placeholder.
     */
    private class DetectionTableModel extends AbstractTableModel {
        private static final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        private final String[] columnNames = {"URL", "Type", "Site Key", "Detected At", "Placeholder"};
        private List<DetectedCaptcha> allDetections = new ArrayList<>();
        private List<DetectedCaptcha> filteredDetections = new ArrayList<>();

        @Override
        public int getRowCount() {
            return filteredDetections.size();
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
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DetectedCaptcha captcha = filteredDetections.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> captcha.pageUrl();
                case 1 -> captcha.type();
                case 2 -> captcha.siteKey();
                case 3 -> DATE_FORMATTER.format(captcha.detectedAt());
                case 4 -> captcha.toPlaceholder();
                default -> null;
            };
        }

        /**
         * Get the DetectedCaptcha at the given filtered row index.
         */
        public DetectedCaptcha getDetectionAt(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < filteredDetections.size()) {
                return filteredDetections.get(rowIndex);
            }
            return null;
        }

        /**
         * Get total (unfiltered) count.
         */
        public int getTotalCount() {
            return allDetections.size();
        }

        /**
         * Refresh the full data set from the DetectionStore.
         */
        public void refreshData() {
            allDetections = new ArrayList<>(detectionStore.getAll());
        }

        /**
         * Apply a filter by CAPTCHA type code. Null means show all.
         */
        public void applyFilter(String typeCode) {
            if (typeCode == null) {
                filteredDetections = new ArrayList<>(allDetections);
            } else {
                filteredDetections = allDetections.stream()
                        .filter(d -> typeCode.equalsIgnoreCase(d.type()))
                        .collect(Collectors.toList());
            }
            fireTableDataChanged();
        }
    }
}
