# reSolver - CAPTCHA solver for BurpSuite

![reSolver Logo](https://github.com/TheQmaks/reSolver/blob/main/resources/logo.jpg?raw=true)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-11%2B-orange)](https://www.oracle.com/java/)
[![Burp Suite Extension](https://img.shields.io/badge/Burp%20Suite-Extension-purple)](https://portswigger.net/bappstore)


An essential tool for penetration testers and security professionals, allowing you to bypass CAPTCHA protection during web application testing.

<details>
  <summary><b>Extension UI preview</b></summary>
  
  ![reSolver Preview](https://github.com/TheQmaks/reSolver/blob/main/resources/preview.gif?raw=true)
</details>

## 📋 Table of Contents

- [Features](#-features)
- [Getting Started](#-getting-started)
  - [Requirements](#requirements)
  - [Installation](#installation)
- [Usage](#-usage)
  - [Configuring Services](#configuring-services)
  - [Using CAPTCHA Placeholders](#using-captcha-placeholders)
  - [Optional Parameters](#optional-parameters)
  - [Basic Examples](#basic-examples)
  - [Viewing Statistics](#viewing-statistics)
- [Real-world Examples](#-real-world-examples-with-2captcha-demo)
  - [reCAPTCHA v2 Standard](#recaptcha-v2-standard)
  - [reCAPTCHA v2 Invisible](#recaptcha-v2-invisible)
  - [reCAPTCHA v2 Enterprise](#recaptcha-v2-enterprise)
  - [reCAPTCHA v3](#recaptcha-v3)
  - [reCAPTCHA v3 Enterprise](#recaptcha-v3-enterprise)
- [Building from Source](#-building-from-source)
- [Compatibility](#-compatibility)
- [FAQ](#-faq)
- [Contributing](#-contributing)
- [License](#-license)

## 🚀 Features

- **Automatic CAPTCHA Solving**:
  - reCAPTCHA v2
  - reCAPTCHA v3

- **Support for Popular Services**:
  - [2Captcha](https://2captcha.com/)
  - [Anti-Captcha](https://anti-captcha.com/)
  - [CapMonster](https://capmonster.cloud/)

- **Robust Architecture**:
  - Configurable thread management
  - Retry logic with error handling
  - Statistics tracking
  - High load detection
  - Custom timeout configuration

## 🚦 Getting Started

### Requirements
- Burp Suite (latest version recommended)
- Java 11+
- Account with one of the supported CAPTCHA solving services

### Installation

1. Download the latest version of the extension from [GitHub Releases](https://github.com/TheQmaks/reSolver/releases)
2. In Burp Suite, go to Extensions → Installed
3. Click "Add" and select the downloaded JAR file
4. After loading, the extension will be ready to use

## 🛠️ Usage

### Configuring Services

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

### Optional Parameters

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

### Basic Examples

**reCAPTCHA v2 with default timeout:**
```
{{CAPTCHA[:]recaptchav2[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com}}
```

**reCAPTCHA v2 with custom timeout (60 seconds):**
```
{{CAPTCHA[:]recaptchav2[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com[:]timeout_seconds=60}}
```

**reCAPTCHA v2 Enterprise invisible with custom timeout:**
```
{{CAPTCHA[:]recaptchav2[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com[:]invisible,enterprise,timeout_seconds=60}}
```

**reCAPTCHA v3 with default parameters:**
```
{{CAPTCHA[:]recaptchav3[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com}}
```

**reCAPTCHA v3 Enterprise with action and min_score:**
```
{{CAPTCHA[:]recaptchav3[:]6LcwIQwfAAAAANmAYa9nt-J_x0Sfh6QcY-x1Vioe[:]https://example.com[:]enterprise,action=login,min_score=0.7}}
```

### Viewing Statistics

Navigate to the "Statistics" tab to view metrics about:
- Number of attempts (success/failure)
- Average solving time
- Success rate per CAPTCHA type and service

## 📝 Real-world Examples with 2Captcha Demo

These examples demonstrate how to use reSolver with the 2Captcha demo site. You can test these examples to see the extension in action.

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

### reCAPTCHA v2 Callback

<details>
  <summary><b>View</b></summary>
  
  ```http
  POST /api/v1/captcha-demo/recaptcha/verify HTTP/2
  Host: 2captcha.com
  Content-Type: application/json
  
  {
    "siteKey": "6LfD3PIbAAAAAJs_eEHvoOl75_83eXSqpPSRFJ_u",
    "answer": "{{CAPTCHA[:]recaptchav2[:]6LfD3PIbAAAAAJs_eEHvoOl75_83eXSqpPSRFJ_u[:]https://2captcha.com/demo/recaptcha-v2-callback}}"
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

## 📦 Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/TheQmaks/reSolver.git
   cd reSolver
   ```

2. Build using Gradle:
   ```bash
   ./gradlew build
   ```
   or for Windows:
   ```bash
   gradlew.bat build
   ```

3. Find the JAR file in the `build/libs` directory

4. Load the extension in Burp Suite from the Extensions tab

## 🔄 Compatibility

- **Burp Suite**: 2024.x and newer
- **Java**: 11 and newer
- **Operating Systems**: Windows, macOS, Linux

## ❓ FAQ

<details>
<summary><b>Which CAPTCHA solving service is the best?</b></summary>
<p> Each service has its advantages. 2Captcha is typically cheaper, while Anti-Captcha and CapMonster are often faster. We recommend configuring multiple services with different priorities for optimal results. </p>
</details>

<details>
<summary><b>How do I find the SiteKey for a CAPTCHA?</b></summary>
<p>Usually, the SiteKey can be found in the page source code. Look at the HTML code and find the "data-sitekey" attribute in a div element with class "g-recaptcha" or similar.</p>
</details>

<details>
<summary><b>Why does CAPTCHA solving take a long time?</b></summary>
<p>Solving time depends on the workload of the chosen service. During high demand periods, waiting times can increase. You can adjust the timeout using the timeout_seconds parameter.</p>
</details>

## 👥 Contributing

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

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

This means you are free to:
- Use, copy, modify, and distribute the software
- Use the software for commercial purposes
- Sublicense and distribute copies of the software as part of your own projects

Under the following terms:
- The original copyright notice and permission notice shall be included in all copies or substantial portions of the software
- The software is provided "as is", without any warranties
