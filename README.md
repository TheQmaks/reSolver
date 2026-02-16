# reSolver - CAPTCHA solver for Burp Suite

![reSolver Logo](https://github.com/TheQmaks/reSolver/blob/main/resources/logo.jpg?raw=true)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://www.oracle.com/java/)
[![Burp Suite Extension](https://img.shields.io/badge/Burp%20Suite-Extension-purple)](https://portswigger.net/bappstore)

Automatically solve CAPTCHAs during web application security testing. Intercepts HTTP requests containing CAPTCHA placeholders, solves them via external providers, and injects the tokens — seamlessly integrating with Repeater, Intruder, and other Burp tools.

## Features

- **8 CAPTCHA types**: reCAPTCHA v2/v3 (incl. invisible & enterprise), hCaptcha, Cloudflare Turnstile, FunCaptcha, GeeTest v3/v4, AWS WAF
- **6 providers**: [2Captcha](https://2captcha.com/), [RuCaptcha](https://rucaptcha.com/), [Anti-Captcha](https://anti-captcha.com/), [CapMonster Cloud](https://capmonster.cloud/), [CapSolver](https://capsolver.com/), [SolveCaptcha](https://solvecaptcha.com/)
- **Auto-Detection**: passively scans HTTP responses for embedded CAPTCHAs and generates ready-to-use placeholders
- **Smart failover**: priority-based provider selection with circuit breaker
- **Statistics**: per-provider success rates, solve times, and balance tracking

## Getting Started

**Requirements:** Burp Suite 2024.x+, Java 21+, account with a supported CAPTCHA solving provider.

1. Download the JAR from [GitHub Releases](https://github.com/TheQmaks/reSolver/releases)
2. In Burp Suite: Extensions > Installed > Add > select the JAR
3. Go to the **Services** tab, enter your API key(s), and enable providers

## Usage

### Placeholder Format

Insert placeholders into HTTP requests — reSolver will replace them with solved tokens:

```
{{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}
```

The `[:]` separator avoids conflicts with colons in URLs and site keys.

### CAPTCHA Types

| Type | Code | Optional Parameters |
|------|------|-------------------|
| reCAPTCHA v2 | `recaptchav2` | `invisible`, `enterprise`, `timeout_seconds` |
| reCAPTCHA v3 | `recaptchav3` | `action`, `min_score`, `enterprise`, `timeout_seconds` |
| hCaptcha | `hcaptcha` | `timeout_seconds` |
| Cloudflare Turnstile | `turnstile` | `timeout_seconds` |
| FunCaptcha | `funcaptcha` | `timeout_seconds` |
| GeeTest v3 | `geetest` | `timeout_seconds` |
| GeeTest v4 | `geetestv4` | `timeout_seconds` |
| AWS WAF | `awswaf` | `timeout_seconds` |

### Examples

```
{{CAPTCHA[:]recaptchav2[:]6LfD3PIbAAAAAJs_eEHvoOl75_83eXSqpPSRFJ_u[:]https://example.com}}
{{CAPTCHA[:]recaptchav2[:]6LfD3PIb...[:]https://example.com[:]invisible,enterprise}}
{{CAPTCHA[:]recaptchav3[:]6LcW00EU...[:]https://example.com[:]action=login,min_score=0.7}}
{{CAPTCHA[:]hcaptcha[:]a1b2c3d4-e5f6...[:]https://example.com}}
{{CAPTCHA[:]turnstile[:]0x4AAAAAAA...[:]https://example.com}}
```

### Auto-Detection

As you browse, reSolver automatically detects CAPTCHAs in HTTP responses. Check the **Detections** tab — click **Copy Placeholder** and paste it into your request.

## Building from Source

```bash
git clone https://github.com/TheQmaks/reSolver.git
cd reSolver
./gradlew build
```

The JAR will be in `build/libs/`.

## Contributing

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit and push your changes
4. Open a Pull Request

## License

[MIT](LICENSE)
