package cli.li.resolver.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.service.captcha.ICaptchaService;
import cli.li.resolver.ui.model.ServiceTableModel;
import cli.li.resolver.ui.renderer.ApiKeyCellRenderer;
import cli.li.resolver.ui.renderer.BalanceCellRenderer;
import cli.li.resolver.ui.renderer.BooleanCellRenderer;

/**
 * Panel for CAPTCHA service management
 */
public class ServicesPanel extends JPanel {
    private final JTable servicesTable;
    private final ServiceTableModel tableModel;

    public ServicesPanel(ServiceManager serviceManager) {
        setLayout(new BorderLayout());

        // Create table model and table
        tableModel = new ServiceTableModel(serviceManager);
        servicesTable = new JTable(tableModel);

        // Allow only single row selection
        servicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Configure table columns
        servicesTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Service
        servicesTable.getColumnModel().getColumn(1).setPreferredWidth(300); // API Key
        servicesTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Enabled
        servicesTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Priority
        servicesTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Balance

        // Set custom renderers
        servicesTable.getColumnModel().getColumn(1).setCellRenderer(new ApiKeyCellRenderer());
        servicesTable.getColumnModel().getColumn(2).setCellRenderer(new BooleanCellRenderer());
        
        // Center-align the Priority column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        servicesTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        
        servicesTable.getColumnModel().getColumn(4).setCellRenderer(new BalanceCellRenderer());

        // Add table header tooltip
        JTableHeader header = servicesTable.getTableHeader();
        header.setToolTipText("Click on columns to sort");

        // Create scroll pane for table
        JScrollPane scrollPane = new JScrollPane(servicesTable);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton priorityUpButton = new JButton("Priority Up");
        JButton priorityDownButton = new JButton("Priority Down");

        buttonPanel.add(priorityUpButton);
        buttonPanel.add(priorityDownButton);

        // Add action listeners
        priorityUpButton.addActionListener(e -> {
            int selectedRow = servicesTable.getSelectedRow();
            if (selectedRow > 0 && selectedRow < tableModel.getRowCount()) {
                ICaptchaService service = tableModel.getServiceAt(selectedRow);
                ICaptchaService aboveService = tableModel.getServiceAt(selectedRow - 1);
                
                // Swap priorities
                int tempPriority = service.getPriority();
                service.setPriority(aboveService.getPriority());
                aboveService.setPriority(tempPriority);
                
                // Auto-save after changes
                tableModel.getServiceManager().saveServiceConfigs();
                refreshData();
                
                // Maintain selection on the moved service
                servicesTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
            }
        });

        priorityDownButton.addActionListener(e -> {
            int selectedRow = servicesTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount() - 1) {
                ICaptchaService service = tableModel.getServiceAt(selectedRow);
                ICaptchaService belowService = tableModel.getServiceAt(selectedRow + 1);
                
                // Swap priorities
                int tempPriority = service.getPriority();
                service.setPriority(belowService.getPriority());
                belowService.setPriority(tempPriority);
                
                // Auto-save after changes
                tableModel.getServiceManager().saveServiceConfigs();
                refreshData();
                
                // Maintain selection on the moved service
                servicesTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
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
        if(tableModel != null) tableModel.refreshData();
    }
}