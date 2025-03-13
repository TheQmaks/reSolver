package cli.li.resolver.logger;

import burp.api.montoya.MontoyaApi;
import cli.li.resolver.ResolverExtension;
import cli.li.resolver.logger.LoggerService.LogEntry;
import cli.li.resolver.logger.LoggerService.LogLevel;

import java.util.function.Consumer;

/**
 * Adapter to integrate LoggerService with Burp Suite API logging
 */
public class BurpLoggerAdapter {
    private static BurpLoggerAdapter instance;
    private final LoggerService loggerService;
    private final MontoyaApi api;
    private boolean initialized = false;

    private BurpLoggerAdapter() {
        this.loggerService = LoggerService.getInstance();
        this.api = ResolverExtension.api;
    }

    public static synchronized BurpLoggerAdapter getInstance() {
        if (instance == null) {
            instance = new BurpLoggerAdapter();
        }
        return instance;
    }

    /**
     * Initialize the adapter and set up listeners
     */
    public void initialize() {
        if (initialized || api == null) {
            return;
        }

        // Add listener to forward logs to Burp's logging API
        loggerService.addListener(new Consumer<LogEntry>() {
            @Override
            public void accept(LogEntry entry) {
                if (entry == null) {
                    return;
                }

                String formattedMessage = String.format("[%s] [%s] %s",
                        entry.getLevel(), entry.getSource(), entry.getMessage());

                switch (entry.getLevel()) {
                    case ERROR:
                        api.logging().logToError(formattedMessage);
                        if (entry.getException() != null) {
                            api.logging().logToError("Exception: " + entry.getException().getMessage());
                        }
                        break;

                    case WARNING:
                    case INFO:
                        api.logging().logToOutput(formattedMessage);
                        break;

                    case DEBUG:
                        // Debug messages are typically not sent to Burp's output
                        // to avoid cluttering the output panel
                        break;
                }
            }
        });

        initialized = true;
    }

    /**
     * Log to both LoggerService and directly to Burp's output
     * @param level Log level
     * @param source Source of the log
     * @param message Log message
     * @param exception Exception (can be null)
     */
    public void log(LogLevel level, String source, String message, Throwable exception) {
        switch (level) {
            case DEBUG:
                loggerService.debug(source, message);
                break;
            case INFO:
                loggerService.info(source, message);
                break;
            case WARNING:
                loggerService.warning(source, message);
                break;
            case ERROR:
                loggerService.error(source, message, exception);
                break;
        }
    }

    /**
     * Log message with INFO level
     * @param source Source of the log
     * @param message Log message
     */
    public void info(String source, String message) {
        log(LogLevel.INFO, source, message, null);
    }

    /**
     * Log message with DEBUG level
     * @param source Source of the log
     * @param message Log message
     */
    public void debug(String source, String message) {
        log(LogLevel.DEBUG, source, message, null);
    }

    /**
     * Log message with WARNING level
     * @param source Source of the log
     * @param message Log message
     */
    public void warning(String source, String message) {
        log(LogLevel.WARNING, source, message, null);
    }

    /**
     * Log message with ERROR level
     * @param source Source of the log
     * @param message Log message
     * @param exception Exception that caused the error
     */
    public void error(String source, String message, Throwable exception) {
        log(LogLevel.ERROR, source, message, exception);
    }

    /**
     * Log message with ERROR level
     * @param source Source of the log
     * @param message Log message
     */
    public void error(String source, String message) {
        log(LogLevel.ERROR, source, message, null);
    }
}