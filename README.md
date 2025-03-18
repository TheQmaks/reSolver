# reSolver
This extension integrates popular CAPTCHA solving services to handle various CAPTCHA types without manual intervention.

## Features

- Automatic solving of CAPTCHA types:
  - reCAPTCHA v2
  - reCAPTCHA v3

- Supports multiple CAPTCHA solving services:
  - 2Captcha
  - Anti-Captcha
  - CapMonster

- Robust architecture:
  - Configurable thread management
  - Retry logic with error handling
  - Statistics tracking
  - High load detection
  - Custom timeout configuration

## Usage

### Configuring CAPTCHA Services

1. Go to the "Services" tab
2. Enter your API keys for one or more services
3. Enable the services you want to use
4. The balance will be automatically checked and displayed when a valid API key is entered

### Using CAPTCHA Placeholders

Add CAPTCHA placeholders to your requests using the following format:

```
{{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}
```

Where:
- `TYPE` is the CAPTCHA type code (e.g., `recaptchav2`, `recaptchav3`)
- `SITEKEY` is the site key for the CAPTCHA
- `URL` is the URL where the CAPTCHA is located
- `OPTIONAL_PARAMS` are additional parameters as key-value pairs (comma-separated)

#### Optional Parameters

The following optional parameters are supported:

- `timeout_seconds` - Custom timeout duration for CAPTCHA solving (default: 30 seconds)
  - Minimum value: 10 seconds
  - Maximum value: 120 seconds
  - Example: `timeout_seconds=60` to set a 60-second timeout

- `invisible` - Indicates that the reCAPTCHA v2 is an invisible type
  - Example: `invisible` to mark the CAPTCHA as invisible

- `enterprise` - Indicates that the reCAPTCHA is an enterprise version
  - Example: `enterprise` to use enterprise solving methods
  
- For reCAPTCHA v3:
  - `action` - The action name for reCAPTCHA v3 (default: "verify")
  - `min_score` - Minimum score threshold for reCAPTCHA v3 (default: varies by service)

#### Examples

reCAPTCHA v2 with default timeout:
```
{{CAPTCHA[:]recaptchav2[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com}}
```

reCAPTCHA v2 with custom timeout (60 seconds):
```
{{CAPTCHA[:]recaptchav2[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com[:]timeout_seconds=60}}
```

reCAPTCHA v2 Enterprise invisible with custom timeout:
```
{{CAPTCHA[:]recaptchav2[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com[:]invisible,enterprise,timeout_seconds=60}}
```

reCAPTCHA v3 with default parameters:
```
{{CAPTCHA[:]recaptchav3[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com}}
```

reCAPTCHA v3 Enterprise with action and min_score:
```
{{CAPTCHA[:]recaptchav3[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com[:]enterprise,action=login,min_score=0.7}}
```

### Viewing Statistics

Navigate to the "Statistics" tab to view metrics about:
- Number of attempts (success/failure)
- Average solving time
- Success rate per CAPTCHA type and service

## Building from Source

1. Clone the repository
2. Build using Gradle: `./gradlew build`
3. Find the JAR file in the `build/libs` directory
4. Load the extension in Burp Suite from the Extensions tab

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
