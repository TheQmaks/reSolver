package cli.li.resolver.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base panel that manages Timer lifecycle.
 * Subclasses should use {@link #createTimer(int, ActionListener)} to create
 * timers so they are automatically stopped when {@link #dispose()} is called.
 */
public abstract class BasePanel extends JPanel {
    private final List<Timer> timers = new ArrayList<>();

    /**
     * Create a Swing Timer that is tracked for lifecycle management.
     *
     * @param delayMs the delay between timer events in milliseconds
     * @param action  the action to perform on each timer tick
     * @return the created (but not yet started) Timer
     */
    protected Timer createTimer(int delayMs, ActionListener action) {
        Timer timer = new Timer(delayMs, action);
        timers.add(timer);
        return timer;
    }

    /**
     * Stop all timers created by this panel and release resources.
     */
    public void dispose() {
        timers.forEach(Timer::stop);
    }
}
