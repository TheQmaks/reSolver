package cli.li.resolver.ui.model;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.service.captcha.ICaptchaService;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for CAPTCHA services
 */
public class ServiceTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Service", "API Key", "Enabled", "Priority", "Balance"};
    private final List<ICaptchaService> services;
    private final ServiceManager serviceManager;

    public ServiceTableModel(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.services = new ArrayList<>();
        refreshData();
    }

    /**
     * Refresh table data
     */
    public void refreshData() {
        fireTableDataChanged();
    }

    @Override
    public void fireTableDataChanged() {
        // Update services list
        services.clear();
        services.addAll(serviceManager.getAllServicesInPriorityOrder());

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
        // Only API key and Enabled columns are editable
        return columnIndex == 1 || columnIndex == 2;
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
        }
        
        // Auto-save after each edit
        serviceManager.saveServiceConfigs();
        
        // Update the cell and possibly refresh balance for API key changes
        fireTableCellUpdated(rowIndex, columnIndex);
        
        // If API key changed, update the balance
        if (columnIndex == 1 && value != null && !((String) value).isEmpty()) {
            serviceManager.updateBalance(service);
            fireTableCellUpdated(rowIndex, 4); // Update balance cell
        }
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
    
    /**
     * Move a service to a new position in the priority order
     * @param fromIndex Source index
     * @param toIndex Target index
     */
    public void moveService(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= services.size() || 
            toIndex < 0 || toIndex >= services.size() || 
            fromIndex == toIndex) {
            return;
        }

        // Get the service to move
        ICaptchaService movedService = services.get(fromIndex);
        
        // Remove it temporarily from our list to avoid conflicts
        services.remove(fromIndex);
        
        // Add it back at the target index
        if (toIndex >= services.size()) {
            services.add(movedService);
        } else {
            services.add(toIndex, movedService);
        }
        
        // Update all priorities to match their new positions in the list
        for (int i = 0; i < services.size(); i++) {
            ICaptchaService service = services.get(i);
            int currentPriority = service.getPriority();
            int newPriority = i;
            
            if (currentPriority != newPriority) {
                service.setPriority(newPriority);
            }
        }
        
        // Save the updated configuration
        serviceManager.saveServiceConfigs();
        
        // Refresh the table to reflect the changes
        fireTableDataChanged();
    }
    
    /**
     * Get the service manager
     * @return The service manager
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
