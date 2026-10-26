---
name: implement-issue
description: Guide for implementing an issue - from reading the assignment through creating a branch, implementing the code, and committing to preparing the PR description.
argument-hint: "[issue-id]"
disable-model-invocation: true
---

<!-- The chat language comes from AGENTS.md (section Communication Language), the role from CLAUDE.local.md. This file adds the workflow. -->

## STEP 0: Check the role

Check the current role in `CLAUDE.local.md`. If the role is not **Developer**:

1. Tell the user that implementation requires the Developer role
2. Offer to switch the role with `/role developer`
3. **STOP** - Do not continue until the Developer role is active

## STEP 1: Get the issue

If the issue ID was passed as an argument (`$ARGUMENTS`), use it and continue with step 2.

Otherwise ask for the GitHub issue number (e.g. `7` or `#7`). Wait for the answer.

## STEP 2: Confirm understanding

After the user gives the issue:

1. Read the issue file `docs/issues/<NNN>-*.md` (`<NNN>` = number padded with zeros to
   three digits). If it is not there, try `docs/issues/done/<NNN>-*.md` — the issue may
   already be closed. Check the status and assignee on GitHub:
   `gh issue view <N> --repo cassandragargoyle/portunix-dream-code --json state,assignees,title`
2. Restate the issue in your own words
3. Explain the needed code changes
4. Ask for confirmation

**STOP** - Wait for explicit confirmation before continuing.

## STEP 3: Create the branch

Create the git branch: `feature/<NNN>-<short-description>` or `fix/<NNN>-<short-description>`

- `<NNN>` is the GitHub issue number padded with zeros to three digits (`#12` → `012`)
- Use lowercase letters and hyphens in the description
- Example: `feature/012-codeinfo-json-output`

Then assign the issue to yourself and mark it as in progress:

```bash
gh issue edit <N> --repo cassandragargoyle/portunix-dream-code --add-assignee @me --add-label in-progress
```

Show the git command before running it.

## STEP 4: Implementation

- Make all code changes in one continuous phase
- Do NOT ask questions unless something is truly ambiguous
- Modify only relevant files
- Follow the project coding style

At the end give a summary:

- Modified files
- Added methods/classes
- Removed code
- Key decisions

Ask the user what they want:

- a) Accept as is
- b) Change specific parts
- c) Redo completely

**STOP** - Wait for approval before continuing.

## STEP 5: Commit

Prepare the commit message:

```
<type>(<N>): <short summary>

- key change 1
- key change 2

refs #<N>
```

Allowed types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

**STOP** - Ask for confirmation of the commit message before committing.

## STEP 6: PR description

Prepare the PR/MR description:

- **Summary**: What this PR does
- **Motivation**: Why (link to the issue)
- **Changes**: List of changes
- **Testing**: Steps to verify
