---
name: start-feature
description: Starts work on one roadmap item. Syncs main, deletes merged local branches, creates the feature branch, sets the roadmap item to in progress, then commits and pushes. Use only when the user types /start-feature or asks to start a roadmap item, for example "start 0.4".
argument-hint: "[roadmap item, e.g. 0.4] [--dry-run]"
disable-model-invocation: true
---

# Start a feature

Argument: $ARGUMENTS (the roadmap item number, example: 0.4).
With --dry-run, only print what you would do and change nothing.

## Checks (stop and tell me if one fails)
1. Find the item in roadmap.md. It must be `[ ]`. If it is `[x]`, stop.
   If it is `[~]`, ask me first.
2. Find its spec in specs/ (example: specs/phase-0/0.4-walking-skeleton.md).
   No spec, no branch: stop and tell me.
3. `git status --porcelain` must be empty. Never stash, reset or discard my changes.

## Steps
1. `git checkout main`, then `git pull --ff-only`, then `git fetch --prune`.
   If the pull is not a fast-forward, stop.
2. Clean old local branches (only `feature/*`; never main, never the current branch):
   - List the ones whose upstream is gone: `git branch -vv`.
   - Delete each with `git branch -d`.
   - If `-d` refuses (it happens after a squash merge), show
     `git log main..<branch> --oneline` and ask me before using `git branch -D`.
3. Create the branch `feature/<item>-<slug>`. The slug comes from the spec file name
   (example: feature/0.4-walking-skeleton). If the branch already exists
   (local or remote), stop and ask.
4. In roadmap.md change only this item from `[ ]` to `[~]`. Nothing else.
   Show the diff.
5. Commit with the message `docs: start task <item> - <short title> [skip ci]`,
   then `git push -u origin <branch>`. Never force-push.
6. Report: the branch name, the deleted branches, the commit hash.
   Then tell me the next step: `/implement-spec <item>`. Do not start the work.
