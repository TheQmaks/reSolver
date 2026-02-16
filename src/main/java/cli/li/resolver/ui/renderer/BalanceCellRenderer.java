package cli.li.resolver.ui.renderer;

import java.awt.*;
import javax.swing.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javax.swing.table.DefaultTableCellRenderer;

import cli.li.resolver.ui.UIHelper;

/**
 * Renderer for balance cells
 */
public class BalanceCellRenderer extends DefaultTableCellRenderer {
    private final DecimalFormat format = new DecimalFormat("0.00");
    private Font italicFont;
    private Font plainFont;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // Check if the provider has an API key configured (column 1)
        Object apiKeyValue = table.getModel().getValueAt(row, 1);
        boolean hasApiKey = apiKeyValue instanceof String s && !s.isEmpty();

        if (!hasApiKey) {
            Component c = super.getTableCellRendererComponent(table, "N/A", isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground(Color.GRAY);
            }
            c.setFont(getItalicFont(c));
            setHorizontalAlignment(JLabel.CENTER);
            return c;
        }

        if (value == null) {
            Component c = super.getTableCellRendererComponent(table, "Loading...", isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground(UIHelper.getSecondaryTextColor());
            }
            c.setFont(getItalicFont(c));
            setHorizontalAlignment(JLabel.CENTER);
            return c;
        }

        if (value instanceof BigDecimal) {
            value = format.format(value);
        }

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
            c.setForeground(table.getForeground());
        }
        c.setFont(getPlainFont(c));
        setHorizontalAlignment(JLabel.RIGHT);
        return c;
    }

    private Font getItalicFont(Component c) {
        Font base = c.getFont();
        if (italicFont == null || italicFont.getSize() != base.getSize()
                || !italicFont.getFamily().equals(base.getFamily())) {
            italicFont = base.deriveFont(Font.ITALIC);
        }
        return italicFont;
    }

    private Font getPlainFont(Component c) {
        Font base = c.getFont();
        if (plainFont == null || plainFont.getSize() != base.getSize()
                || !plainFont.getFamily().equals(base.getFamily())) {
            plainFont = base.deriveFont(Font.PLAIN);
        }
        return plainFont;
    }
}
