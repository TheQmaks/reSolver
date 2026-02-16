package cli.li.resolver.ui.model;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import javax.swing.table.AbstractTableModel;

import cli.li.resolver.service.ServiceManager;
import cli.li.resolver.provider.ProviderService;

/**
 * Table model for CAPTCHA provider services.
 * Columns: Provider, API Key, Enabled, Priority, Balance, Supported Types.
 */
public class ServiceTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Provider", "API Key", "Enabled", "Priority", "Balance", "Supported Types"};
    private final List<ProviderService> services;
    private final ServiceManager serviceManager;

    public ServiceTableModel(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.services = new ArrayList<>();
        reloadServices();
    }

    /**
     * Refresh table data from ServiceManager and trigger async balance fetches.
     */
    public void refreshData() {
        // Trigger async balance fetches
        for (ProviderService service : services) {
            service.refreshBalance();
        }
        // Notify table that cell values may have changed (without rebuilding the list)
        if (!services.isEmpty()) {
            fireTableRowsUpdated(0, services.size() - 1);
        }
    }

    /**
     * Rebuild the services list from ServiceManager. Called on initial load
     * and when the list structure changes (e.g. priority reorder).
     */
    public void reloadServices() {
        services.clear();
        services.addAll(serviceManager.getAllProviderServices());
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
        ProviderService service = services.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> service.getDisplayName();
            case 1 -> service.getApiKey();
            case 2 -> service.isEnabled();
            case 3 -> service.getPriority();
            case 4 -> service.getCachedBalance();
            case 5 -> String.join(", ", service.getSupportedTypes());
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Only API Key and Enabled columns are editable
        return columnIndex == 1 || columnIndex == 2;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        ProviderService service = services.get(rowIndex);
        switch (columnIndex) {
            case 1 -> service.setApiKey((String) value);
            case 2 -> service.setEnabled((Boolean) value);
            default -> { }
        }

        // Auto-save after each edit
        serviceManager.saveServiceConfigs();

        // Update the cell and possibly refresh balance for API key changes
        fireTableCellUpdated(rowIndex, columnIndex);

        // If API key changed, force-refresh the balance immediately
        if (columnIndex == 1 && value != null && !((String) value).isEmpty()) {
            service.forceRefreshBalance();
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
     * Get the provider service at a specific row
     * @param rowIndex Row index
     * @return ProviderService at the row
     */
    public ProviderService getServiceAt(int rowIndex) {
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
        ProviderService movedService = services.get(fromIndex);

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
            ProviderService service = services.get(i);
            int currentPriority = service.getPriority();
            int newPriority = i;

            if (currentPriority != newPriority) {
                service.setPriority(newPriority);
            }
        }

        // Save the updated configuration
        serviceManager.saveServiceConfigs();

        // Refresh the table to reflect the changes
        reloadServices();
    }

    /**
     * Get the service manager
     * @return The service manager
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
