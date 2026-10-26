---
name: finish-branch
description: Finish work on a feature branch - merge into the main branch, clean up the branch, and optionally update the issue status.
---

**IMPORTANT: NEVER add "Co-Authored-By: Claude <noreply@anthropic.com>" to code files or git commits.**

## STEP 1: Get the context

1. Get the current branch (`git branch --show-current`) — it should follow the convention `feature/NNN-short-name` (or `fix/NNN-short-name`), where `NNN` = GitHub issue number padded with zeros to three digits, details in `docs/issues/NNN-*.md` (see skill `implement-issue`)
2. The target branch for the merge is `main` (unless the user says otherwise)
3. Remote: `origin` = GitHub (`git@github.com:cassandragargoyle/portunix-dream-code`, public) — code, issues, and PRs

Check that the branch exists and has commits compared to `main`.

List the commits that will be merged (`git log --oneline main..HEAD`). If the branch contains commits not related to the issue, tell the user before the merge.

## STEP 2: Check the acceptance protocol

**CRITICAL**: According to the Developer role (`.claude/roles/developer.md`), merging into `main` is NOT allowed without a tester acceptance protocol for the issue.

1. Check that the file `docs/testing/acceptance-<issue-number>.md` exists
2. Check that it contains `**STATUS**: PASS` and `**Approval for merge**: YES`

> **To be completed:** the methodology (`ISSUE-DEVELOPMENT-METHODOLOGY.md`) does not exist in this project yet. Until it does, the protocol from the Tester role may exist only in the conversation — in that case ask the user whether acceptance took place, for which commit, and whether the tester verified changes made after the protocol.

If the acceptance protocol **does not exist or is not PASS**:

- Tell the user that the merge is not allowed without tester approval
- Ask: "Do you want to continue anyway? (hotfix / exception approved by the architect?)"
- If the user does not confirm an exception → **STOP**

## STEP 3: Merge and clean up

Ask the user for the preferred way:

### Option A: Direct merge (default for solo development)

```bash
git checkout main
git merge feature/<NNN>-<description>
git branch -d feature/<NNN>-<description>
git push origin main
git push origin --delete feature/<NNN>-<description>
```

Run the last command only if the feature branch was pushed to `origin`.

**STOP** - Wait for confirmation before the merge.

### Option B: Pull Request (for team work)

Prepare the PR description:

- **Summary**: What this PR does
- **Motivation**: Why (link to the issue)
- **Changes**: List of changes
- **Testing**: Steps to verify
- **Acceptance**: Link to the acceptance protocol

Create the PR on GitHub: `gh pr create --repo CassandraGargoyle/portunix-dream-code --base main` (with `Fixes #NNN` in the body if the issue exists on GitHub).

## STEP 4: Update the issue (optional)

Ask the user: "Is the issue fully done with this?"

### If YES:

1. Update the issue file `docs/issues/NNN-*.md`:

   - Change `**Status**` to `✅ Implemented`
   - Add the closing date
   - Check the met acceptance criteria (`- [ ]` → `- [x]`); leave unmet ones as `- [ ]` with a note why

2. Move the file to the `done/` archive. No README tables are updated — the issue list is on GitHub:

   ```bash
   git mv docs/issues/NNN-*.md docs/issues/done/
   git add docs/issues/done/NNN-*.md
   ```

   `git mv` stages only the move — `git add` is needed for the changes from point 1.

3. Commit the status change together with the move (one commit):

   ```text
   docs(<N>): close issue — archive to done/
   ```

4. Close the GitHub issue (`<N>` = number without leading zeros) and fix the path in the body to `done/`:

   ```bash
   REPO="cassandragargoyle/portunix-dream-code"
   gh issue close <N> --repo "$REPO" --comment "Implemented, acceptance: docs/testing/acceptance-NNN.md"
   gh issue edit <N> --repo "$REPO" --remove-label in-progress
   gh issue view <N> --repo "$REPO" --json body --jq .body \
     | sed 's#docs/issues/NNN-#docs/issues/done/NNN-#' \
     | gh issue edit <N> --repo "$REPO" --body-file -
   ```

### If NO:

Skip the status update. The issue stays open for more work.

## STEP 5: Summary

Show a summary:

- Merged into branch: `<name>`
- Feature branch deleted: yes/no
- Issue status updated: yes/no/skipped
- Next steps (if any)
