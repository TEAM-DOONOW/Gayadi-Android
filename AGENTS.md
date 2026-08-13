# [AGENTS.md](http://AGENTS.md)

## Secrets and environment files

  - Never read, print, parse, copy, modify, or source `.env` or any `.env.*` file.
  - `.env.example` is the only allowed environment-file reference.
  - Never access secret files such as `*.key`, `*.pem`, `*.p12`, `*.jks`, or files under `secrets/`.

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
