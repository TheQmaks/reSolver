package cli.li.resolver.util;

/**
 * Utility class for working with API keys
 */
public class ApiKeyUtils {

    /**
     * Masks an API key for display or logging purposes
     * @param apiKey The API key to mask
     * @return A masked version of the API key
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "(empty)";
        } else if (apiKey.length() > 8) {
            // Use exact number of hidden characters
            int hiddenLength = apiKey.length() - 8; // Length of hidden part (all except first and last 4 chars)
            StringBuilder masked = new StringBuilder(apiKey.substring(0, 4));
            for (int i = 0; i < hiddenLength; i++) {
                masked.append("•");
            }
            masked.append(apiKey.substring(apiKey.length() - 4));
            return masked.toString();
        } else {
            return "••••••••";
        }
    }
}
