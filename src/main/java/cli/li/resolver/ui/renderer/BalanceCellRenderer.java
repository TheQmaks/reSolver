package cli.li.resolver.ui.renderer;

import java.awt.*;
import javax.swing.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javax.swing.table.DefaultTableCellRenderer;

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
