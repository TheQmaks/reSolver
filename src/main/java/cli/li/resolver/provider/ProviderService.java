package cli.li.resolver.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cli.li.resolver.captcha.exception.CaptchaSolverException;

/**
 * Runtime wrapper around CaptchaProvider that holds state such as
 * apiKey, enabled flag, priority, cached balance, and statistics.
 */
public class ProviderService {

    private static final long BALANCE_CACHE_SECONDS = 20;

    private final CaptchaProvider provider;
    private volatile String apiKey = "";
    private volatile boolean enabled = false;
    private volatile int priority;
    private volatile BigDecimal cachedBalance = null;
    private volatile Instant balanceCacheExpiry = Instant.EPOCH;
    private final ProviderStatistics statistics = new ProviderStatistics();
    private final AtomicBoolean isCheckingBalance = new AtomicBoolean(false);

    /**
     * Create a new ProviderService wrapping the given provider.
     *
     * @param provider the underlying CAPTCHA provider
     * @param priority the priority for this provider (lower = higher priority)
     */
    public ProviderService(CaptchaProvider provider, int priority) {
        this.provider = provider;
        this.priority = priority;
    }

    // ---- Delegate methods ----

    /**
     * Get the unique identifier of the provider.
     *
     * @return provider id
     */
    public String getId() {
        return provider.id();
    }

    /**
     * Get the display name of the provider.
     *
     * @return human-readable provider name
     */
    public String getDisplayName() {
        return provider.displayName();
    }

    /**
     * Get the set of CAPTCHA types supported by this provider.
     *
     * @return set of supported type codes
     */
    public Set<String> getSupportedTypes() {
        return provider.supportedTypes();
    }

    // ---- Solve ----

    /**
     * Solve a CAPTCHA request using the underlying provider, recording statistics.
     *
     * @param request the solve request containing all CAPTCHA parameters
     * @return the solution token/string
     * @throws CaptchaSolverException if solving fails
     */
    public String solve(SolveRequest request) throws CaptchaSolverException {
        long startTime = System.currentTimeMillis();
        try {
            String result = provider.solve(request);
            long elapsed = System.currentTimeMillis() - startTime;
            statistics.recordSuccess(elapsed);
            return result;
        } catch (CaptchaSolverException e) {
            statistics.recordFailure();
            throw e;
        }
    }

    // ---- Balance ----

    /**
     * Refresh and return the cached balance.
     * If the cache has not expired, the cached value is returned immediately.
     * If the cache has expired and no check is already in progress, a virtual
     * thread is started to fetch the balance asynchronously. The stale cached
     * value is returned in the meantime.
     *
     * @return the most recently cached balance
     */
    public BigDecimal refreshBalance() {
        if (apiKey == null || apiKey.isEmpty()) {
            return cachedBalance;
        }

        if (Instant.now().isBefore(balanceCacheExpiry)) {
            return cachedBalance;
        }

        startBalanceFetch();
        return cachedBalance;
    }

    /**
     * Force-refresh the balance, bypassing the cache.
     * Used when the user explicitly requests a balance update.
     */
    public void forceRefreshBalance() {
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }
        balanceCacheExpiry = Instant.EPOCH;
        startBalanceFetch();
    }

    private void startBalanceFetch() {
        if (isCheckingBalance.compareAndSet(false, true)) {
            Thread.ofVirtual().start(() -> {
                try {
                    BigDecimal balance = provider.fetchBalance(apiKey);
                    cachedBalance = balance;
                    balanceCacheExpiry = Instant.now().plusSeconds(BALANCE_CACHE_SECONDS);
                } catch (Exception e) {
                    // Keep stale cached value on failure
                } finally {
                    isCheckingBalance.set(false);
                }
            });
        }
    }

    // ---- Accessors ----

    /**
     * Get the API key configured for this provider.
     *
     * @return API key string
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Set the API key for this provider.
     *
     * @param apiKey the API key to use
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Check whether this provider is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable this provider.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the priority of this provider.
     *
     * @return priority value (lower = higher priority)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the priority of this provider.
     *
     * @param priority the new priority value
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get the last cached balance value without triggering a refresh.
     *
     * @return cached balance
     */
    public BigDecimal getCachedBalance() {
        return cachedBalance;
    }

    /**
     * Get the statistics tracker for this provider.
     *
     * @return provider statistics
     */
    public ProviderStatistics getStatistics() {
        return statistics;
    }

    /**
     * Get the underlying CaptchaProvider implementation.
     *
     * @return the wrapped provider
     */
    public CaptchaProvider getProvider() {
        return provider;
    }

    /**
     * Check whether this provider is fully configured and ready to use.
     * A provider is considered configured when it is enabled, has a non-empty
     * API key, and the key passes the provider's format validation.
     *
     * @return true if configured and ready
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isEmpty() && provider.isValidKeyFormat(apiKey);
    }
}
