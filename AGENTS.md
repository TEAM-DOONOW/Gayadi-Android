# AGENTS.md

## Secrets and environment files

- Never read, print, parse, copy, modify, or source `.env` or any `.env.*` file.
- `.env.example` is the only allowed environment-file reference.
- Never access secret files such as `*.key`, `*.pem`, `*.p12`, `*.jks`,
  `*.keystore`, or files under `secrets/`.
- Never use commands such as `cat .env`, `cat .env.local`, `sed .env`,
  `grep .env`, `rg .env`, `source .env`, or `source .env.*`.
- Do not expose environment variable values in logs, responses, patches, or
  screenshots.
- When environment variable names or configuration structure are needed, read
  `.env.example` only.
- Use placeholder values from `.env.example`; never use real secrets.
- If a task requires information that exists only in `.env`, stop and ask the
  user to provide a redacted value or a safe alternative.
- Tests and commands must not dump the process environment.

## Git and GitHub workflow

### Issues

- Create or identify a GitHub issue before starting a repository change.
- Use `.github/ISSUE_TEMPLATE/custom.md` and describe both the purpose and the
  actionable TODO checklist.
- Do not reuse an unrelated issue merely to obtain an issue number.

### Branches

- Never commit directly to `main`.
- Create a dedicated branch from an up-to-date `main` for each issue.
- Name branches as `<type>/#<issue-number>-<short-kebab-description>`.
- Use the type that best matches the change, such as `feat`, `fix`, `refactor`,
  `style`, `test`, `docs`, or `chore`.
- Keep a branch scoped to its linked issue and do not mix unrelated changes.

### Commits

- Format commit subjects as `<type>/#<issue-number>: <concise Korean summary>`.
- Keep commits focused and reviewable.
- Inspect the staged diff before committing. Never stage secrets, environment
  files, generated credentials, local IDE state, or unrelated user changes.
- Do not rewrite, squash, amend, or force-push shared history unless the user
  explicitly requests it.

### Pull requests

- Open pull requests from the issue branch into `main`.
- Follow `.github/pull_request_template.md` and complete `Summary`,
  `Description`, `PR Point`, `Reference`, and `Test`.
- Put `close: #<issue-number>` in the description so the linked issue closes
  when the pull request is merged.
- Report the exact verification commands and results in the `Test` section.
- Do not merge while required checks are failing or pending.
- Do not merge a pull request unless the user explicitly requests the merge.

### Verification

- Before opening or updating a pull request, run the checks relevant to the
  changed modules.
- The repository-wide Android quality gate is:

  ```shell
  ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
  ```

- Run additional module or instrumentation tests when the changed behavior is
  not covered by the repository-wide gate.
- Confirm `git status` and review the final diff before pushing.
