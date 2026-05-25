# Contributing to Hexive

Thank you for your interest in contributing to Hexive. This document outlines the process for submitting changes and the standards we expect.

## Getting Started

1. **Fork** the repository and clone your fork locally.
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Set up the development environment:
   - **Backend:** Java 17+, Maven 3.8+ → `cd src/backend && ./mvnw spring-boot:run`
   - **Hardware:** Arduino IDE 2.x with the ESP32 board package installed.

## Development Standards

### Java Backend (`src/backend/`)
- **JavaDoc** is mandatory on all public classes, methods, and non-trivial fields. Use `@param`, `@return`, and `@throws` tags.
- **Logging:** Use SLF4J (`LoggerFactory.getLogger(...)`) — never `System.out.println`.
- **Exception handling:** Do not expose stack traces in API responses.
- **Tests:** Add or update unit tests in `src/test/` for any new service or controller logic.

### C++ Firmware (`src/hardware/`)
- **Doxygen** comment blocks (`/** ... */`) are required above every function.
- Define pin numbers as `static const int` — avoid magic numbers.
- Do not commit WiFi credentials, API keys, or certificates. Use `Secrets.h` templates.

### General
- Keep commits atomic and descriptive: `feat: add humidity threshold to decision engine`
- Use [Conventional Commits](https://www.conventionalcommits.org/) format:
  - `feat:` — New feature
  - `fix:` — Bug fix
  - `docs:` — Documentation changes
  - `refactor:` — Code restructuring without functional change
  - `test:` — Adding or updating tests

## Pull Request Process

1. Ensure your branch is up to date with `main`.
2. Run the backend tests before submitting:
   ```bash
   cd src/backend && ./mvnw test
   ```
3. Open a Pull Request against `main` with:
   - A clear **title** following Conventional Commits
   - A **description** explaining what changed and why
   - References to any related issues
4. At least one maintainer review is required before merge.
5. All CI checks must pass (build, tests, lint).

## Reporting Issues

- Use the GitHub **Issues** tab with the appropriate label:
  - `bug` — Something is broken
  - `enhancement` — Feature request
  - `documentation` — Missing or incorrect docs
- Include steps to reproduce, expected behaviour, and actual behaviour.

## Code of Conduct

Be respectful, constructive, and professional. We follow the [Contributor Covenant](https://www.contributor-covenant.org/) Code of Conduct.

---

Thank you for helping make Hexive better.
