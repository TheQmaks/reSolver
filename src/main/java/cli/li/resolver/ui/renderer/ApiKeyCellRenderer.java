package cli.li.resolver.ui.renderer;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import cli.li.resolver.util.ApiKeyUtils;

/**
 * Renderer for API key cells that masks the actual key
 */
public class ApiKeyCellRenderer extends DefaultTableCellRenderer {

    private Font italicFont;
    private Font plainFont;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String apiKey = (String) value;

        if (apiKey == null || apiKey.isEmpty()) {
            Component c = super.getTableCellRendererComponent(table, "(not set)", isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground(Color.GRAY);
            }
            c.setFont(getItalicFont(c));
            return c;
        }

        Component c = super.getTableCellRendererComponent(table, ApiKeyUtils.maskApiKey(apiKey), isSelected, hasFocus, row, column);
        if (!isSelected) {
            c.setForeground(table.getForeground());
        }
        c.setFont(getPlainFont(c));
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
