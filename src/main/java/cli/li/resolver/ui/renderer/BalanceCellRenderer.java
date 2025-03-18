package cli.li.resolver.ui.renderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Renderer for balance cells
 */
public class BalanceCellRenderer extends DefaultTableCellRenderer {
    private final DecimalFormat format = new DecimalFormat("0.00");

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof BigDecimal) {
            value = format.format(value);
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
