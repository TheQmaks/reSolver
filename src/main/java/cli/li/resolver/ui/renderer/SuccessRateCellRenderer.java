package cli.li.resolver.ui.renderer;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer for success rate cells with color coding.
 * Green for >= 70%, orange for 40-70%, red for < 40%.
 */
public class SuccessRateCellRenderer extends DefaultTableCellRenderer {

    private static final Color GREEN = new Color(34, 139, 34);
    private static final Color ORANGE = new Color(204, 120, 0);
    private static final Color RED = new Color(178, 34, 34);

    public SuccessRateCellRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected && value instanceof String strValue) {
            double rate = parseRate(strValue);
            if (rate >= 70.0) {
                c.setForeground(GREEN);
            } else if (rate >= 40.0) {
                c.setForeground(ORANGE);
            } else {
                c.setForeground(RED);
            }
        }

        return c;
    }

    private double parseRate(String value) {
        try {
            String cleaned = value.replace("%", "").replace(",", ".").trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
