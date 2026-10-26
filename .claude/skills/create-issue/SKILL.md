---
name: create-issue
description: 'Create a new issue the GitHub-first way — first the GitHub issue is created (GitHub assigns the number), then the detailed write-up goes to docs/issues/NNN-*.md. Use when the user wants to create an issue, feature request, or bug. Examples: "create an issue …", "/create-issue codeinfo JSON output", "open an issue for …".'
argument-hint: [short issue description]
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion, Write, Edit
user-invocable: true
---

# Create an issue (GitHub-first)

**IMPORTANT: NEVER add "Co-Authored-By: Claude <noreply@anthropic.com>" to code files or git commits.**

**Argument:** $ARGUMENTS

## Principle

**GitHub is the single source of truth for the issue list.** GitHub assigns the number
on creation. GitHub holds only the **basics** (title + short description + path to the
detail file). Details live in `docs/issues/NNN-*.md`, where `NNN` = GitHub issue number
padded with zeros to three digits.

**Issues and code are on GitHub** (`cassandragargoyle/portunix-dream-code`, **public**).
In the issue body give the **path** to the detail file, not a web link.

**Do not add any overview tables** to README and do not use the `internal/` directory
(removed).

The GitHub repository is public — do not put credentials, personal data, internal
hostnames, absolute local paths, or links to private repositories into the issue body or
the detail file.

## STEP 1: Collect the inputs

From the argument / context (or with `AskUserQuestion` if missing) get:

- **Title** — short and clear, in English (no number; GitHub adds it)
- **Slug** — kebab-case for the file name (derive from the title, e.g. `codeinfo-json-output`)
- **Type** → label: `bug` | `enhancement` | `documentation` | `testing`
- **Priority** → label: `critical` | `high` | `medium` | `low`
- **Short description** (1–3 sentences) — goes to the GitHub issue body
- **Detailed content** — goes to the local `.md` (problem, steps, acceptance criteria, tests)

All texts in **English** (project convention).

## STEP 2: Preflight — check `gh`

```bash
gh --version && gh auth status
```

If not logged in, stop and ask the user to run `gh auth login` (the `!` prefix works).

## STEP 3: Create the GitHub issue (GitHub assigns the number)

**STOP** — first show the title, body, and labels and ask for confirmation. The issue is public.

```bash
REPO="cassandragargoyle/portunix-dream-code"
gh issue create --repo "$REPO" \
  --title "<TITLE>" \
  --body "<SHORT DESCRIPTION>" \
  --label <type>,<priority>
```

`gh issue create` prints the URL — take the **number `N`** from it (`.../issues/12` →
`NNN=012` for the file name, `#12` on GitHub).

## STEP 4: Write the detail file

Create `docs/issues/<NNN>-<slug>.md` with the full content (template in
[ISSUE-MANAGEMENT.md](../../../docs/contributing/ISSUE-MANAGEMENT.md#detail-file-template)):

```markdown
# Issue #<N>: <Title>

## Summary

<1–3 sentences>

**Status**: 📋 Open
**Type**: <type>
**Priority**: <priority>
**Component**: <component>
**Created**: <YYYY-MM-DD>
**GitHub**: #<N>
**Related**: #<...>

## Problem Description

## Acceptance Criteria

1. ...

## Testing
```

## STEP 5: Add the file path to the GitHub issue

```bash
gh issue edit <N> --repo "$REPO" \
  --body "<SHORT DESCRIPTION>

📄 Full details: \`docs/issues/<NNN>-<slug>.md\` (repository path)"
```

## STEP 6: Summary

```text
Issue #<N> created.

GitHub:  https://github.com/cassandragargoyle/portunix-dream-code/issues/<N>
Detail:  docs/issues/<NNN>-<slug>.md
Type/Priority: <type> / <priority>

Next steps:
  /implement-issue <N>
```

Do not commit the file automatically — leave the commit to the user unless they say otherwise.

## Notes

- **Never make up** the number — GitHub always assigns it. Issues and PRs share one counter.
- Archiving a finished issue: `gh issue close <N>` +
  `git mv docs/issues/<NNN>-*.md docs/issues/done/` (see skill `finish-branch`).
- Full methodology: `docs/contributing/ISSUE-MANAGEMENT.md`.
