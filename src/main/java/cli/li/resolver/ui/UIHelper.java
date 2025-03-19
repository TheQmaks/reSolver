package cli.li.resolver.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Helper class for UI operations and managing parent frames
 */
public class UIHelper {
    private static JFrame mainFrame;

    /**
     * Initialize the helper with Burp's main frame
     */
    public static void initialize() {
        if (mainFrame == null) {
            // Find Burp's main frame through the component hierarchy
            for (Frame frame : Frame.getFrames()) {
                if (frame instanceof JFrame && frame.isVisible() && 
                        frame.getTitle() != null && 
                        frame.getTitle().contains("Burp Suite")) {
                    mainFrame = (JFrame) frame;
                    break;
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
        
        // If we still couldn't find the frame, return null
        // The dialog will be centered on screen instead
        return mainFrame;
    }
}
