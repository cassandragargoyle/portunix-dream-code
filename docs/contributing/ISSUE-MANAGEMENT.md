# Issue Management for portunix-dream-code

## Purpose

This document defines the process for creating, tracking, and archiving issues in the
portunix-dream-code project.

> **GitHub is the single source of truth for the issue list.** There are **no overview
> tables** in `docs/issues/README.md` or `docs/issues/done/README.md` — a shared Markdown
> table edited by everyone causes merge conflicts. Status, assignee, and labels live on
> GitHub; the repository keeps only the long-form write-up per issue.

- **Issue tracker**: <https://github.com/cassandragargoyle/portunix-dream-code/issues>
- **Code**: same GitHub repository. The GitHub issue body refers to the detail file by its
  **repository path**, so the reference stays valid when the file moves to `done/`.

## GitHub-First Workflow

The issue is **created on GitHub first**, so **GitHub assigns the number `N`**. The GitHub
issue carries only the *basics* (title + a short description + the path to the detail
file). The full write-up lives in the repository at `docs/issues/NNN-name.md`, where the
filename number **equals the GitHub issue number** (zero-padded to three digits).

1. **Create the GitHub issue** (GitHub assigns `N`):

   ```bash
   gh issue create --repo cassandragargoyle/portunix-dream-code \
     --title "Short descriptive title" \
     --body "Short description." \
     --label enhancement,medium
   ```

2. **Write the detail file** `docs/issues/NNN-name.md` (problem, acceptance criteria,
   testing, related issues). Use `N` in the filename and in the `# Issue #N:` heading.

3. **Update the GitHub body** with the path to the detail file once `N` is known:

   ```bash
   gh issue edit N --repo cassandragargoyle/portunix-dream-code --body "Short description.

   📄 Full details: \`docs/issues/NNN-name.md\` (repository path)"
   ```

4. **Reference `N`** in branches and commits: `feature/NNN-name` (zero-padded,
   `#9` → `feature/009-name`), `feat(N): …`, `refs #N`.

5. **Archive on completion**: `gh issue close N` and
   `git mv docs/issues/NNN-name*.md docs/issues/done/`. `git mv` stages only the move:
   run `git add docs/issues/done/NNN-name*.md` again if you edited the file (status,
   closing date) before the move. Update the path in the GitHub body to `done/`.

The `create-issue` skill (`/create-issue`) automates steps 1–3.

> **Numbering note.** GitHub issues and pull requests share one counter, so numbers may
> skip where a PR consumed one. Never invent a number — always take the one GitHub returns.

### Legacy Numbering

Issues 001 … 008 were created before the move to GitHub and lived in
`docs/issues/internal/`. They were mirrored to GitHub 1:1, so **issue `NNN` = `#N`**.
The `internal/` directory is retired. Existing mentions in commits (e.g. `fix(008): …`)
stay as they are.

### Public Content

The GitHub repository and its issues are **public**. GitHub bodies and issue write-ups
must not contain credentials, personal data, internal hostnames, absolute local paths,
or links into private repositories. Use placeholders in examples.

## File Structure

```text
docs/
├── issues/
│   ├── README.md                  # Process notes only — NO overview table
│   ├── NNN-name.md                # Active issue write-up (NNN = GitHub issue number)
│   └── done/                      # Archived (✅ Implemented / ❌ Closed / ➡️ Moved)
│       ├── README.md
│       └── NNN-*.md
└── testing/
    └── acceptance-NNN.md          # Tester acceptance protocol
```

## File Naming

Issue filenames use **lowercase kebab-case** only, matching
[NAMING-CONVENTIONS.md](NAMING-CONVENTIONS.md).

- **Pattern**: `NNN-<slug>.md`
- **`NNN`**: the GitHub issue number, zero-padded to three digits (`#7` → `007`)
- **`<slug>`**: lowercase ASCII letters and digits, words separated by a single hyphen,
  derived from the issue title

Examples:

- OK: `009-codeinfo-json-output.md`
- NOT OK: `009-CodeInfo_JSON.md`

## Detail File Template

```markdown
# Issue #N: Title

## Summary

1–3 sentences.

**Status**: 📋 Open
**Type**: Bug | Enhancement | Feature | Task
**Priority**: Critical | High | Medium | Low
**Component**: <module or path>
**Created**: YYYY-MM-DD
**GitHub**: #N
**Related**: #...

## Problem Description

## Acceptance Criteria

1. ...

## Testing
```

## Status Values

- 📋 Open — needs work
- 🚧 In Progress — actively worked on (GitHub label `in-progress`)
- ✅ Implemented — completed (file in `done/`, GitHub issue closed as completed)
- ❌ Closed — closed without implementation (file in `done/`, closed as not planned)
- ➡️ Moved — moved to another project (file in `done/`)
- ⏸️ On Hold — temporarily paused

## Labels

- **Type**: `bug`, `enhancement`, `documentation`, `testing`
- **Priority**: `critical`, `high`, `medium`, `low`
- **Status**: `in-progress`

Always assign one type label and one priority label. Create a missing label only when it
is needed by more than one issue.

## Claiming an Issue

GitHub also holds the assignee. Before starting work, check the issue is open and not
assigned to someone else, then assign it to yourself:

```bash
gh issue view N --repo cassandragargoyle/portunix-dream-code --json state,assignees
gh issue edit N --repo cassandragargoyle/portunix-dream-code --add-assignee @me --add-label in-progress
```

## Integration with Development Workflow

### Branches

- `feature/NNN-short-name` for features and enhancements
- `fix/NNN-short-name` for bug fixes

`NNN` is the GitHub issue number zero-padded to three digits (`#9` → `fix/009-short-name`),
matching the issue file name.

### Commit Messages

```text
feat(N): short summary
fix(N): short summary
docs(N): close issue — archive to done/
```

Use `refs #N` or `closes #N` in the commit body or PR description to link the GitHub issue.

---

*Last updated: 2026-10-26 — moved from `internal/` + README table to GitHub-first numbering*
