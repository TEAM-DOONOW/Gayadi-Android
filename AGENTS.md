# [AGENTS.md](http://AGENTS.md)

## Secrets and environment files

  - Never read, print, parse, copy, modify, or source `.env` or any `.env.*` file.
  - `.env.example` is the only allowed environment-file reference.
  - Never access secret files such as `*.key`, `*.pem`, `*.p12`, `*.jks`, `*.keystore`, or files under `secrets/`.

  - Never use commands such as `cat .env`, `cat .env.local`, `sed .env`, `grep .env`, `rg .env`, or

  `source .env` or `source .env.*`.

  - Do not expose environment variable values in logs, responses, patches, or

  screenshots.

  - When environment variable names or configuration structure are needed, read

  `.env.example` only.

  - Use placeholder values from `.env.example`; never use real secrets.

  - If a task requires information that exists only in `.env`, stop and ask the

  user to provide a redacted value or a safe alternative.

  - Tests and commands must not dump the process environment.

## UI and design work

  - Before creating or changing any Compose page, read `design.md` and follow its design tokens, layout rules, component patterns, and verification checklist.
  - Reuse components from `core/designsystem` and `core/ui` before adding feature-local UI implementations.
  - If a UI task introduces a new reusable design decision, update `design.md` in the same change.
