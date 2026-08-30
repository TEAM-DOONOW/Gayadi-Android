# Branch Naming Convention

## Structure

`<type>/#<issue-number>`

Use one of the commit types from [the commit convention](convention.md). Do not append a description after the issue number.

## Examples

- `feat/#38`
- `fix/#105`
- `refactor/#88`
- `design/#112`

## Issue Extraction

Extract `<type>/#<issue-number>` from a matching branch and format the message as `<type>/#<issue-number>: <subject>`.

If the branch does not match, do not infer or invent an issue number. Ask the user for it.
