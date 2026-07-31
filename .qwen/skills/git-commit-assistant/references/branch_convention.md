# Branch Naming Convention

## Structure

`<type>/#<issue-number>-<description>`

Use one of the commit types from [the commit convention](convention.md). Keep the description short and use lowercase kebab-case.

## Examples

- `feat/#38-upload-api`
- `fix/#105-token-leak`
- `refactor/#88-mypage-vm`
- `design/#112-login-screen`

## Issue Extraction

Extract `<type>/#<issue-number>` from a matching branch and format the message as `<type>/#<issue-number>: <subject>`.

If the branch does not match, do not infer or invent an issue number. Ask the user for it.
