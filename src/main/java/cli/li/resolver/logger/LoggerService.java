package cli.li.resolver.logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service for logging extension events
 */
public class LoggerService {
    public enum LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Class representing a log entry
     */
    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final LogLevel level;
        private final String source;
        private final String message;
        private final Throwable exception;

        public LogEntry(LogLevel level, String source, String message, Throwable exception) {
            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.source = source;
            this.message = message;
            this.exception = exception;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public LogLevel getLevel() {
            return level;
        }

        public String getSource() {
            return source;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getException() {
            return exception;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = timestamp.format(formatter);

            StringBuilder builder = new StringBuilder();
            builder.append(formattedTime)
                    .append(" [").append(level).append("] ")
                    .append("[").append(source).append("] ")
                    .append(message);

            if (exception != null) {
                builder.append("\nException: ").append(exception.getMessage());

                // Add stack trace
                for (StackTraceElement element : exception.getStackTrace()) {
                    builder.append("\n  at ").append(element.toString());
                }
            }

            return builder.toString();
        }
    }

    private static LoggerService instance;
    private final List<LogEntry> logs;
    private final List<Consumer<LogEntry>> listeners;
    private LogLevel minLevel = LogLevel.INFO;
    private final int maxLogSize;

    private LoggerService() {
        logs = new CopyOnWriteArrayList<>();
        listeners = new ArrayList<>();
        maxLogSize = 1000; // Default max log size
    }

    public static synchronized LoggerService getInstance() {
        if (instance == null) {
            instance = new LoggerService();
        }
        return instance;
    }

    /**
     * Add a log listener
     * @param listener Log entry consumer
     */
    public void addListener(Consumer<LogEntry> listener) {
        listeners.add(listener);
    }

    /**
     * Remove a log listener
     * @param listener Log entry consumer
     */
    public void removeListener(Consumer<LogEntry> listener) {
        listeners.remove(listener);
    }

    /**
     * Set minimum log level to display
     * @param level Minimum log level
     */
    public void setMinLevel(LogLevel level) {
        this.minLevel = level;
    }

    /**
     * Get all logs
     * @return List of log entries
     */
    public List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    /**
     * Clear all logs
     */
    public void clearLogs() {
        logs.clear();
        for (Consumer<LogEntry> listener : listeners) {
            listener.accept(null); // Signal to clear logs
        }
    }

    /**
     * Log a message with INFO level
     * @param source Source of the log
     * @param message Log message
     */
    public void info(String source, String message) {
        log(LogLevel.INFO, source, message, null);
    }

    /**
     * Log a message with DEBUG level
     * @param source Source of the log
     * @param message Log message
     */
    public void debug(String source, String message) {
        log(LogLevel.DEBUG, source, message, null);
    }

    /**
     * Log a message with WARNING level
     * @param source Source of the log
     * @param message Log message
     */
    public void warning(String source, String message) {
        log(LogLevel.WARNING, source, message, null);
    }

    /**
     * Log a message with ERROR level
     * @param source Source of the log
     * @param message Log message
     * @param exception Exception that caused the error
     */
    public void error(String source, String message, Throwable exception) {
        log(LogLevel.ERROR, source, message, exception);
    }

    /**
     * Log a message with ERROR level
     * @param source Source of the log
     * @param message Log message
     */
    public void error(String source, String message) {
        log(LogLevel.ERROR, source, message, null);
    }

    /**
     * Log a message
     * @param level Log level
     * @param source Source of the log
     * @param message Log message
     * @param exception Exception that caused the error (can be null)
     */
    private void log(LogLevel level, String source, String message, Throwable exception) {
        if (level.ordinal() < minLevel.ordinal()) {
            return;
        }

        LogEntry entry = new LogEntry(level, source, message, exception);

        // Add to logs
        logs.add(entry);

        // If the logs list is too large, remove the oldest entries
        while (logs.size() > maxLogSize) {
            logs.removeFirst();
        }

        // Notify listeners
        for (Consumer<LogEntry> listener : listeners) {
            listener.accept(entry);
        }
    }
}