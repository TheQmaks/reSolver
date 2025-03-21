package cli.li.resolver.ui.renderer;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import cli.li.resolver.util.ApiKeyUtils;

/**
 * Renderer for API key cells that masks the actual key
 */
public class ApiKeyCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String apiKey = (String) value;
        String maskedApiKey = ApiKeyUtils.maskApiKey(apiKey);
        return super.getTableCellRendererComponent(table, maskedApiKey, isSelected, hasFocus, row, column);
    }
}
