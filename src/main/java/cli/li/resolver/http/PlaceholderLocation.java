package cli.li.resolver.http;

import cli.li.resolver.captcha.CaptchaRequest;

/**
 * Class representing a placeholder location in a request
 */
public record PlaceholderLocation(String placeholder, CaptchaRequest captchaRequest,
                                  PlaceholderLocationType locationType, String headerName, int startIndex,
                                  int endIndex) {
}