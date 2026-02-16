package cli.li.resolver.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Helper class for UI operations, managing parent frames, and theme detection.
 * Colors are cached and recomputed only when the theme changes.
 */
public class UIHelper {
    private static JFrame mainFrame;

    // Color cache — recomputed only when the theme changes
    private static boolean cachedDark;
    private static boolean cacheInitialized;
    private static Color textColor;
    private static Color secondaryTextColor;
    private static Color successColor;
    private static Color warningColor;
    private static Color errorColor;
    private static Color codeBackground;
    private static Color borderColor;
    private static Color headingColor;
    private static Color tableEvenRowBackground;

    /**
     * Initialize the helper with Burp's main frame
     */
    public static void initialize() {
        if (mainFrame == null) {
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof JFrame && frame.isVisible() &&
                        frame.getTitle() != null &&
                        frame.getTitle().contains("Burp Suite")) {
                    mainFrame = (JFrame) frame;
                    break;
                }
            }
            // Fallback: accept any visible JFrame
            if (mainFrame == null) {
                for (Frame frame : Frame.getFrames()) {
                    if (frame instanceof JFrame && frame.isVisible()) {
                        mainFrame = (JFrame) frame;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get Burp's main frame to use as parent for dialogs
     * @return JFrame to use as parent
     */
    public static Component getBurpFrame() {
        if (mainFrame == null) {
            initialize();
        }
        return mainFrame;
    }

    // ---- Theme detection ----

    /**
     * Detect whether the current Look-and-Feel is a dark theme.
     */
    public static boolean isDarkTheme() {
        Color bg = javax.swing.UIManager.getColor("Panel.background");
        if (bg == null) return false;
        int luminance = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return luminance < 128;
    }

    private static void refreshCacheIfNeeded() {
        boolean dark = isDarkTheme();
        if (cacheInitialized && cachedDark == dark) {
            return;
        }
        cachedDark = dark;
        cacheInitialized = true;
        textColor = dark ? new Color(210, 210, 210) : new Color(30, 30, 30);
        secondaryTextColor = dark ? new Color(150, 150, 150) : Color.GRAY;
        successColor = dark ? new Color(80, 200, 80) : new Color(34, 139, 34);
        warningColor = dark ? new Color(230, 160, 50) : new Color(200, 130, 0);
        errorColor = dark ? new Color(230, 80, 80) : new Color(178, 34, 34);
        codeBackground = dark ? new Color(50, 50, 55) : new Color(232, 232, 232);
        borderColor = dark ? new Color(80, 80, 80) : new Color(200, 200, 200);
        headingColor = dark ? new Color(180, 200, 220) : new Color(44, 62, 80);
        tableEvenRowBackground = dark ? new Color(45, 45, 50) : new Color(245, 245, 245);
    }

    /**
     * Primary text color, adapts to theme.
     */
    public static Color getTextColor() {
        refreshCacheIfNeeded();
        return textColor;
    }

    /**
     * Secondary/muted text color, adapts to theme.
     */
    public static Color getSecondaryTextColor() {
        refreshCacheIfNeeded();
        return secondaryTextColor;
    }

    /**
     * Success color (green), adapts to theme.
     */
    public static Color getSuccessColor() {
        refreshCacheIfNeeded();
        return successColor;
    }

    /**
     * Warning color (amber), adapts to theme.
     */
    public static Color getWarningColor() {
        refreshCacheIfNeeded();
        return warningColor;
    }

    /**
     * Error color (red), adapts to theme.
     */
    public static Color getErrorColor() {
        refreshCacheIfNeeded();
        return errorColor;
    }

    /**
     * Background for code blocks / table headers, adapts to theme.
     */
    public static Color getCodeBackground() {
        refreshCacheIfNeeded();
        return codeBackground;
    }

    /**
     * Border color, adapts to theme.
     */
    public static Color getBorderColor() {
        refreshCacheIfNeeded();
        return borderColor;
    }

    /**
     * Heading text color, adapts to theme.
     */
    public static Color getHeadingColor() {
        refreshCacheIfNeeded();
        return headingColor;
    }

    /**
     * Even row background for tables, adapts to theme.
     */
    public static Color getTableEvenRowBackground() {
        refreshCacheIfNeeded();
        return tableEvenRowBackground;
    }

    /**
     * Convert a Color to a CSS hex string.
     */
    public static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
