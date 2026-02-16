package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.ui.model.ServiceTableModel;
import cli.li.resolver.ui.renderer.ApiKeyCellRenderer;
import cli.li.resolver.ui.renderer.BalanceCellRenderer;
import cli.li.resolver.ui.renderer.BooleanCellRenderer;
import cli.li.resolver.provider.ProviderService;

/**
 * Panel for CAPTCHA service management.
 * Displays all registered provider services in a table with controls
 * for adjusting priority order.
 */
public class ServicesPanel extends JPanel {
    private final ServiceManager serviceManager;
    private final JTable servicesTable;
    private final ServiceTableModel tableModel;

    public ServicesPanel(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        setLayout(new BorderLayout());

        tableModel = new ServiceTableModel(serviceManager);
        servicesTable = createServicesTable();

        add(new JScrollPane(servicesTable), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshData();
    }

    private JTable createServicesTable() {
        JTable table = new JTable(tableModel) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    Object value = getValueAt(row, col);
                    if (value != null) {
                        String text = value.toString();
                        if (text.length() > 40) {
                            return text;
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(250);

        table.getColumnModel().getColumn(1).setCellRenderer(new ApiKeyCellRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new BooleanCellRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(4).setCellRenderer(new BalanceCellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new TooltipHeaderRenderer(header.getDefaultRenderer(),
                new String[]{
                        "CAPTCHA solving provider name",
                        "API key (click cell to edit)",
                        "Enable or disable this provider",
                        "Priority order (lower = tried first)",
                        "Account balance (auto-refreshed)",
                        "CAPTCHA types this provider can solve"
                }));

        return table;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton priorityUpButton = new JButton("Priority Up");
        JButton priorityDownButton = new JButton("Priority Down");
        JButton refreshBalancesButton = new JButton("Refresh Balances");
        refreshBalancesButton.setToolTipText("Manually refresh balances for all enabled providers");

        buttonPanel.add(priorityUpButton);
        buttonPanel.add(priorityDownButton);
        buttonPanel.add(Box.createHorizontalStrut(16));
        buttonPanel.add(refreshBalancesButton);

        priorityUpButton.addActionListener(e -> {
            int selectedRow = servicesTable.getSelectedRow();
            if (selectedRow > 0 && selectedRow < tableModel.getRowCount()) {
                ProviderService service = tableModel.getServiceAt(selectedRow);
                ProviderService aboveService = tableModel.getServiceAt(selectedRow - 1);

                int tempPriority = service.getPriority();
                service.setPriority(aboveService.getPriority());
                aboveService.setPriority(tempPriority);

                tableModel.getServiceManager().saveServiceConfigs();
                refreshData();
                servicesTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
            }
        });

        priorityDownButton.addActionListener(e -> {
            int selectedRow = servicesTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount() - 1) {
                ProviderService service = tableModel.getServiceAt(selectedRow);
                ProviderService belowService = tableModel.getServiceAt(selectedRow + 1);

                int tempPriority = service.getPriority();
                service.setPriority(belowService.getPriority());
                belowService.setPriority(tempPriority);

                tableModel.getServiceManager().saveServiceConfigs();
                refreshData();
                servicesTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
            }
        });

        refreshBalancesButton.addActionListener(e -> {
            serviceManager.refreshAllBalances();
            refreshBalancesButton.setEnabled(false);
            Timer reEnableTimer = new Timer(3000, ev -> refreshBalancesButton.setEnabled(true));
            reEnableTimer.setRepeats(false);
            reEnableTimer.start();
        });

        return buttonPanel;
    }

    /**
     * Refresh table data
     */
    public void refreshData() {
        if (tableModel != null) tableModel.refreshData();
    }

    /**
     * Custom header renderer that adds per-column tooltips.
     */
    private static class TooltipHeaderRenderer implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer delegate;
        private final String[] tooltips;

        TooltipHeaderRenderer(javax.swing.table.TableCellRenderer delegate, String[] tooltips) {
            this.delegate = delegate;
            this.tooltips = tooltips;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JComponent jc && column >= 0 && column < tooltips.length) {
                jc.setToolTipText(tooltips[column]);
            }
            return c;
        }
    }
}
