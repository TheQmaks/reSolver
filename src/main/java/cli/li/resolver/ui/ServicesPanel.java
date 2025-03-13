package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.service.captcha.ICaptchaService;

/**
 * Panel for CAPTCHA service management
 */
public class ServicesPanel extends JPanel {
    private final ServiceManager serviceManager;
    private final JTable servicesTable;
    private final ServiceTableModel tableModel;

    public ServicesPanel(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;

        setLayout(new BorderLayout());

        // Create table model and table
        tableModel = new ServiceTableModel();
        servicesTable = new JTable(tableModel);

        // Configure table columns
        servicesTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        servicesTable.getColumnModel().getColumn(1).setPreferredWidth(300);
        servicesTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        servicesTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        servicesTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Set custom renderers
        servicesTable.getColumnModel().getColumn(2).setCellRenderer(new BooleanCellRenderer());
        servicesTable.getColumnModel().getColumn(4).setCellRenderer(new BalanceCellRenderer());

        // Create scroll pane for table
        JScrollPane scrollPane = new JScrollPane(servicesTable);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton refreshButton = new JButton("Refresh Balances");
        JButton priorityUpButton = new JButton("Priority Up");
        JButton priorityDownButton = new JButton("Priority Down");

        buttonPanel.add(saveButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(priorityUpButton);
        buttonPanel.add(priorityDownButton);

        // Add action listeners
        saveButton.addActionListener(e -> {
            serviceManager.saveServiceConfigs();
            JOptionPane.showMessageDialog(this, "Service configurations saved", "Save", JOptionPane.INFORMATION_MESSAGE);
        });

        refreshButton.addActionListener(e -> {
            serviceManager.updateAllBalances();
            refreshData();
        });

        priorityUpButton.addActionListener(e -> {
            int selectedRow = servicesTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                ICaptchaService service = tableModel.getServiceAt(selectedRow);
                service.setPriority(service.getPriority() - 1);
                refreshData();
            }
        });

        priorityDownButton.addActionListener(e -> {
            int selectedRow = servicesTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                ICaptchaService service = tableModel.getServiceAt(selectedRow);
                service.setPriority(service.getPriority() + 1);
                refreshData();
            }
        });

        // Add components to panel
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Initial data refresh
        refreshData();
    }

    /**
     * Refresh table data
     */
    public void refreshData() {
        if(tableModel != null) tableModel.fireTableDataChanged();
    }

    /**
     * Table model for services
     */
    private class ServiceTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Service", "API Key", "Enabled", "Priority", "Balance"};
        private final List<ICaptchaService> services;

        public ServiceTableModel() {
            services = new ArrayList<>();
            refreshData();
        }

        @Override
        public void fireTableDataChanged() {
            // Update services list
            services.clear();
            services.addAll(serviceManager.getServicesInPriorityOrder());

            super.fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return services.size();
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
            ICaptchaService service = services.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> service.getName();
                case 1 -> service.getApiKey();
                case 2 -> service.isEnabled();
                case 3 -> service.getPriority();
                case 4 -> service.getBalance();
                default -> null;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1 || columnIndex == 2 || columnIndex == 3;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            ICaptchaService service = services.get(rowIndex);
            switch (columnIndex) {
                case 1:
                    service.setApiKey((String) value);
                    break;
                case 2:
                    service.setEnabled((Boolean) value);
                    break;
                case 3:
                    service.setPriority((Integer) value);
                    break;
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 2 -> Boolean.class;
                case 3 -> Integer.class;
                case 4 -> BigDecimal.class;
                default -> String.class;
            };
        }

        /**
         * Get the service at a specific row
         * @param rowIndex Row index
         * @return Service at the row
         */
        public ICaptchaService getServiceAt(int rowIndex) {
            return services.get(rowIndex);
        }
    }

    /**
     * Renderer for balance cells
     */
    private class BalanceCellRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat format = new DecimalFormat("0.00");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof BigDecimal) {
                value = format.format(value);
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Renderer for boolean cells
     */
    private class BooleanCellRenderer extends JCheckBox implements TableCellRenderer {
        public BooleanCellRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            setSelected((Boolean) value);
            return this;
        }
    }
}