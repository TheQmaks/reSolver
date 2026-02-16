# reSolver - CAPTCHA solver for BurpSuite

![reSolver Logo](https://github.com/TheQmaks/reSolver/blob/main/resources/logo.jpg?raw=true)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://www.oracle.com/java/)
[![Burp Suite Extension](https://img.shields.io/badge/Burp%20Suite-Extension-purple)](https://portswigger.net/bappstore)


An essential tool for penetration testers and security professionals, allowing you to bypass CAPTCHA protection during web application testing.

<details>
  <summary><b>Extension UI preview</b></summary>

  ![reSolver Preview](https://github.com/TheQmaks/reSolver/blob/main/resources/preview.gif?raw=true)
</details>

## Table of Contents

- [Features](#features)
- [Demonstration Video](#demonstration-video)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Real-world Examples](#real-world-examples-with-2captcha-demo)
- [Building from Source](#building-from-source)
- [Compatibility](#compatibility)
- [FAQ](#faq)
- [Contributing](#contributing)
- [License](#license)

## Features

- **8 CAPTCHA types supported**:
  - reCAPTCHA v2 (standard, invisible, enterprise)
  - reCAPTCHA v3 (with action and min_score)
  - hCaptcha
  - Cloudflare Turnstile
  - FunCaptcha (Arkose Labs)
  - GeeTest v3 / v4
  - AWS WAF CAPTCHA

- **6 solving providers**:
  - [2Captcha](https://2captcha.com/)
  - [RuCaptcha](https://rucaptcha.com/)
  - [Anti-Captcha](https://anti-captcha.com/)
  - [CapMonster Cloud](https://capmonster.cloud/)
  - [CapSolver](https://capsolver.com/)
  - [SolveCaptcha](https://solvecaptcha.com/)

- **Auto-Detection**: passively scans HTTP responses for embedded CAPTCHAs and generates ready-to-use placeholders (supports 16 CAPTCHA types including hCaptcha, Turnstile, FunCaptcha, GeeTest, Yandex SmartCaptcha, and more)

- **Smart provider selection**: priority-based ordering with circuit breaker for automatic failover between providers

- **Statistics and monitoring**: per-provider success rates, average solve times, and balance tracking

## Demonstration Video

[![reSolver Demo Video](https://img.youtube.com/vi/9hI14Thj1aY/0.jpg)](https://www.youtube.com/watch?v=9hI14Thj1aY)

This video demonstrates the real-world application of reSolver with BurpSuite's Intruder tool. In 1 minute, 100 requests are sent across 10 threads, successfully bypassing reCAPTCHA v2.

## Getting Started

### Requirements
- Burp Suite (latest version recommended)
- Java 21+
- Account with one of the supported CAPTCHA solving services

### Installation

1. Download the latest version from [GitHub Releases](https://github.com/TheQmaks/reSolver/releases)
2. In Burp Suite, go to Extensions > Installed
3. Click "Add" and select the downloaded JAR file

## Usage

### Configuring Services

1. Go to the **Services** tab
2. Enter your API keys for one or more providers
3. Enable the providers you want to use
4. Balance is checked automatically when a valid API key is entered
5. Adjust priority order with the Priority Up/Down buttons (lower = tried first)

### Auto-Detection

As you browse through Burp Suite, reSolver automatically detects CAPTCHAs in HTTP responses:

1. Go to the **Detections** tab to see discovered CAPTCHAs
2. Each detection shows the page URL, CAPTCHA type, site key, and a ready-to-use placeholder
3. Click **Copy Placeholder** to copy it to your clipboard
4. Paste the placeholder into your request in Repeater, Intruder, or any other Burp tool

### Placeholder Format

```
{{CAPTCHA[:]TYPE[:]SITEKEY[:]URL[:][OPTIONAL_PARAMS]}}
```

Where:
- `TYPE` - CAPTCHA type code (see table below)
- `SITEKEY` - the site key for the CAPTCHA
- `URL` - URL of the page containing the CAPTCHA
- `OPTIONAL_PARAMS` - additional parameters, comma-separated

**Note:** The `[:]` separator is used instead of plain `:` to avoid conflicts with colons in URLs and site keys.

### Supported CAPTCHA Types

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

**reCAPTCHA v2:**
```
{{CAPTCHA[:]recaptchav2[:]6LfD3PIbAAAAAJs_eEHvoOl75_83eXSqpPSRFJ_u[:]https://example.com}}
```

**reCAPTCHA v2 invisible + enterprise:**
```
{{CAPTCHA[:]recaptchav2[:]6LfD3PIbAAAAAJs_...[:]https://example.com[:]invisible,enterprise}}
```

**reCAPTCHA v3 with action and min_score:**
```
{{CAPTCHA[:]recaptchav3[:]6LcW00EUAAAA...[:]https://example.com[:]action=login,min_score=0.7}}
```

**hCaptcha:**
```
{{CAPTCHA[:]hcaptcha[:]a1b2c3d4-e5f6...[:]https://example.com}}
```

**Cloudflare Turnstile:**
```
{{CAPTCHA[:]turnstile[:]0x4AAAAAAA...[:]https://example.com}}
```

**FunCaptcha:**
```
{{CAPTCHA[:]funcaptcha[:]12345678-1234...[:]https://example.com}}
```

**AWS WAF:**
```
{{CAPTCHA[:]awswaf[:]AQIDAHjcYu/GjX...[:]https://example.com}}
```

## Real-world Examples with 2Captcha Demo

These examples work with the [2Captcha demo site](https://2captcha.com/demo). You can test them to see the extension in action.

### reCAPTCHA v2 Standard

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/recaptcha/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "6LfD3PIbAAAAAJs_eEHvoOl75_83eXSqpPSRFJ_u",
    "answer": "{{CAPTCHA[:]recaptchav2[:]6LfD3PIbAAAAAJs_eEHvoOl75_83eXSqpPSRFJ_u[:]https://2captcha.com/demo/recaptcha-v2}}"
  }
  ```
</details>

### reCAPTCHA v2 Invisible

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/recaptcha/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "6LdO5_IbAAAAAAeVBL9TClS19NUTt5wswEb3Q7C5",
    "answer": "{{CAPTCHA[:]recaptchav2[:]6LdO5_IbAAAAAAeVBL9TClS19NUTt5wswEb3Q7C5[:]https://2captcha.com/demo/recaptcha-v2-invisible[:]invisible}}"
  }
  ```
</details>

### reCAPTCHA v2 Enterprise

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/recaptcha-enterprise/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "6Lf26sUnAAAAAIKLuWNYgRsFUfmI-3Lex3xT5N-s",
    "token": "{{CAPTCHA[:]recaptchav2[:]6Lf26sUnAAAAAIKLuWNYgRsFUfmI-3Lex3xT5N-s[:]https://2captcha.com/demo/recaptcha-v2-enterprise[:]enterprise}}"
  }
  ```
</details>

### reCAPTCHA v3

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/recaptcha/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "6Lcyqq8oAAAAAJE7eVJ3aZp_hnJcI6LgGdYD8lge",
    "answer": "{{CAPTCHA[:]recaptchav3[:]6Lcyqq8oAAAAAJE7eVJ3aZp_hnJcI6LgGdYD8lge[:]https://2captcha.com/demo/recaptcha-v3[:]min_score=0.7}}"
  }
  ```
</details>

### reCAPTCHA v3 Enterprise

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/recaptcha-enterprise/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "6Lel38UnAAAAAMRwKj9qLH2Ws4Tf2uTDQCyfgR6b",
    "token": "{{CAPTCHA[:]recaptchav3[:]6Lel38UnAAAAAMRwKj9qLH2Ws4Tf2uTDQCyfgR6b[:]https://2captcha.com/demo/recaptcha-v3-enterprise[:]enterprise,min_score=0.9}}"
  }
  ```
</details>

### hCaptcha

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/hcaptcha/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "f7de0da3-3303-44e8-ab48-fa32ff8cbe7c",
    "answer": "{{CAPTCHA[:]hcaptcha[:]f7de0da3-3303-44e8-ab48-fa32ff8cbe7c[:]https://2captcha.com/demo/hcaptcha}}"
  }
  ```
</details>

### Cloudflare Turnstile

<details>
  <summary><b>View</b></summary>

  ```http
  POST /api/v1/captcha-demo/cloudflare-turnstile/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json

  {
    "siteKey": "0x4AAAAAAAVrOwQWPlm3Bnr5",
    "answer": "{{CAPTCHA[:]turnstile[:]0x4AAAAAAAVrOwQWPlm3Bnr5[:]https://2captcha.com/demo/cloudflare-turnstile}}"
  }
  ```
</details>

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/TheQmaks/reSolver.git
   cd reSolver
   ```

2. Build using Gradle:
   ```bash
   ./gradlew build
   ```

3. Find the JAR file in the `build/libs` directory

4. Load the extension in Burp Suite from the Extensions tab

## Compatibility

- **Burp Suite**: 2024.x and newer
- **Java**: 21 and newer
- **Operating Systems**: Windows, macOS, Linux

## FAQ

<details>
<summary><b>Which CAPTCHA solving service is the best?</b></summary>
<p>Each service has its advantages. 2Captcha is typically cheaper, while Anti-Captcha and CapMonster are often faster. Configure multiple providers with different priorities — reSolver will automatically fail over to the next provider if one fails.</p>
</details>

<details>
<summary><b>How do I find the SiteKey for a CAPTCHA?</b></summary>
<p>The easiest way is to use reSolver's <b>Auto-Detection</b> feature — just browse the target site and check the <b>Detections</b> tab for discovered CAPTCHAs with ready-to-use placeholders. Alternatively, look for the <code>data-sitekey</code> attribute in the page source.</p>
</details>

<details>
<summary><b>Why does CAPTCHA solving take a long time?</b></summary>
<p>Solving time depends on the provider's workload. You can adjust the timeout using the <code>timeout_seconds</code> parameter (10-120 seconds, default 30). Consider configuring multiple providers for faster results.</p>
</details>

<details>
<summary><b>What if my provider doesn't support a CAPTCHA type?</b></summary>
<p>Check the <b>Supported Types</b> column in the Services tab. Not all providers support all types. Configure a provider that supports the CAPTCHA type you need. Auto-Detection will detect CAPTCHAs even if no provider can solve them yet.</p>
</details>

## Contributing

Contributions are welcome! If you want to contribute:

1. Fork the repository
2. Create a branch for your changes:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. Make your changes and commit them:
   ```bash
   git commit -m 'Add some amazing feature'
   ```
4. Push to your fork:
   ```bash
   git push origin feature/amazing-feature
   ```
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
