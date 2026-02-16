package cli.li.resolver.ui.renderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer for CAPTCHA type cells with color-coded markers.
 * Each CAPTCHA type has a distinct color for quick visual identification.
 */
public class CaptchaTypeCellRenderer extends DefaultTableCellRenderer {

    private static final Map<String, Color> TYPE_COLORS = Map.ofEntries(
            Map.entry("recaptchav2", new Color(30, 100, 200)),     // Blue
            Map.entry("recaptchav3", new Color(34, 139, 34)),       // Green
            Map.entry("hcaptcha", new Color(0, 163, 163)),          // Teal
            Map.entry("turnstile", new Color(217, 119, 6)),         // Orange
            Map.entry("funcaptcha", new Color(139, 60, 183)),       // Purple
            Map.entry("geetest", new Color(185, 28, 28)),           // Red
            Map.entry("geetestv4", new Color(185, 28, 28)),         // Red
            Map.entry("awswaf", new Color(180, 130, 0)),            // Amber
            Map.entry("mtcaptcha", new Color(60, 130, 180)),        // Steel Blue
            Map.entry("lemin", new Color(0, 150, 80)),              // Emerald
            Map.entry("keycaptcha", new Color(160, 80, 40)),        // Brown
            Map.entry("friendlycaptcha", new Color(70, 160, 70)),   // Lime Green
            Map.entry("yandex", new Color(255, 204, 0)),            // Yandex Yellow
            Map.entry("tencent", new Color(7, 118, 211)),           // Tencent Blue
            Map.entry("captchafox", new Color(220, 90, 50)),        // Fox Orange
            Map.entry("procaptcha", new Color(100, 60, 160))        // Indigo
    );

    private static final int MARKER_SIZE = 10;
    private static final int MARKER_GAP = 6;

    private final Map<Color, Icon> iconCache = new HashMap<>();

    public CaptchaTypeCellRenderer() {
        setHorizontalAlignment(SwingConstants.LEFT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String typeCode = value != null ? value.toString().toLowerCase().trim() : "";
        Color markerColor = TYPE_COLORS.getOrDefault(typeCode, Color.GRAY);

        setIcon(iconCache.computeIfAbsent(markerColor, c -> new ColorDotIcon(c, MARKER_SIZE)));
        setIconTextGap(MARKER_GAP);

        return this;
    }

    /**
     * Simple colored circle icon for use as a type marker.
     */
    private static class ColorDotIcon implements Icon {
        private final Color color;
        private final int size;

        ColorDotIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, size, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
