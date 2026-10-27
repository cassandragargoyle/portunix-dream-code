# AGENTS.md — portunix-dream-code

Project instructions for all AI coding agents (Claude Code, Codex, Copilot, …).
This file is the single source of truth — `CLAUDE.md` only imports it.

## Project Information

- **Project Name**: portunix-dream-code
- **Primary Purpose**: Toolkit that simplifies programming itself (code analysis, code
  information, AI-assisted code work). Portunix simplifies the environment, Dream Code
  simplifies writing the code — see [README.md](README.md)
- **Languages**: Java 21 (Maven), Go (codeanalysis plugin), Python 3.10+ (scripts and tests, managed by uv)
- **Platforms**: Linux, Windows (macOS development status TBD)
- **Code Repository**: GitHub — <https://github.com/cassandragargoyle/portunix-dream-code> (public)
- **Issue Tracker**: GitHub — <https://github.com/cassandragargoyle/portunix-dream-code/issues>

## Security Guidelines

- Never run destructive commands without confirmation
- Always warn about potential data loss before executing risky operations
- Require explicit confirmation for operations that could lose implemented code
- The GitHub repository is public: never put credentials, personal data, internal
  hostnames, or absolute local paths into issues or files that go to GitHub

## Project Structure

- `/codeanalysis/` - Portunix plugin `ptx-codeanalysis` (Go + Java bridges: SonarLint, CodeScan)
- `/codeinfo/` - Standalone Java CLI `ptx-codeinfo` (Picocli, GraalVM Native Image)
- `/docs/` - Documentation: ADRs (`adr/`), issues (`issues/`), contributing guidelines (`contributing/`)
- `/scripts/` - Release and helper scripts
- `/.claude/` - AI assistant configuration: roles and skills

See [docs/MODULES.md](docs/MODULES.md) for module details.

## Coding Guidelines & Development Instructions

### Strict Rules

- Code, code comments, and program messages must be in English
- Chat with the AI assistant follows [Communication Language](#communication-language); everything written to the repository or GitHub stays in English
- When implementing new features, always check existing project structure first
- Prefer editing existing files over creating new ones
- Do not add "Generated with …" signatures or AI co-author lines to code, files, or commits

See `docs/contributing/CODE-STYLE-*.md` for language-specific guidelines.

### General Principles

- Follow existing conventions and styles in the project
- Always explore existing code before creating new implementations
- Maintain consistency with established patterns
- All program text (messages, labels, constants) in English
- Comments in English, written as phrases (no ending periods)

### TODO Management

- Follow the established TODO format: `TODO:NNN [INITIALS]: description`
- Use per-file sequential numbering (001, 002, 003, etc.)
- Use `TODO:XXX` as temporary placeholder, then request proper numbering

### Issue Tracking

- Issues are **GitHub-first**: the GitHub issue list is the single source of truth (number, status, assignee, labels)
- GitHub assigns the number; the long-form write-up lives in `docs/issues/NNN-name.md` (archived in `docs/issues/done/`), no overview tables
- Branches: `feature/NNN-name` or `fix/NNN-name`; commits: `feat(N): …`, `fix(N): …`
- Claude Code: use the `/create-issue` skill to create issues
- Full process: [docs/contributing/ISSUE-MANAGEMENT.md](docs/contributing/ISSUE-MANAGEMENT.md)

### Team Communication

- English is the primary language for all project communication
- Use simple, clear English to accommodate non-native speakers
- Avoid idioms, colloquialisms, and culture-specific references
- Team initials assignment follows collision resolution (JS → JSm if JS exists)

### Communication Language

- Default language for chat with the AI assistant is English
- If the user's personal instructions set a different communication language, that
  language wins for the chat — for Claude Code the global `~/.claude/CLAUDE.md` or the
  untracked `CLAUDE.local.md`, for other tools their user-level instructions
- The setting changes only the chat (answers, questions, explanations). These always
  stay in English: code, code comments, program messages, commit messages, branch
  names, issues (GitHub and `docs/issues/`), PR descriptions, and documentation
- Skills, roles, and prompts in `.claude/` are written in English too; the chat language
  comes only from the user's personal instructions. A translation may sit next to the
  English file as `<NAME>.<lang>.md` (for example `SKILL.cs.md`); only the English file
  is loaded by the assistant

### AI Assistant Guidelines

- Claude Code is the preferred AI assistant with pre-configured context (roles in `.claude/roles/`, skills in `.claude/skills/`)
- Alternative AI tools use this file; other context materials must be prepared by the user
- Follow [docs/contributing/AI-ASSISTANTS.md](docs/contributing/AI-ASSISTANTS.md)

### Translation Workflow

- Use `.translated/` directory structure for team member translations
- Follow ISO 639-1 language codes (cs, de, fr, etc.)
- Refer to [docs/contributing/TRANSLATION-WORKFLOW.md](docs/contributing/TRANSLATION-WORKFLOW.md)

### Repository Management

- Main branch is `main` (not `master`)
- License: MIT, see [LICENSE](LICENSE)

### System Information Detection

- **Always use** `portunix system info` for OS detection when Portunix is available
- **Only if Portunix is not available**, use manual detection methods
- Do not write custom OS detection scripts when Portunix already provides this functionality

## Development Setup

### Prerequisites

- Java 21+ and Maven 3.6+
- Go (for `codeanalysis`)
- Python 3.10+ and [uv](https://docs.astral.sh/uv/)
- GraalVM (`GRAALVM_HOME`) only for native image builds

### Initial Setup

```bash
git clone git@github.com:cassandragargoyle/portunix-dream-code.git
cd portunix-dream-code
make deps
```

### Build

Always build with `make`, not with `mvn` or `go build` directly:

```bash
make build                  # Java modules + codeanalysis (default)
make build-codeinfo         # codeinfo JAR only
make build-codeinfo-native  # codeinfo native image (requires GRAALVM_HOME)
make build-codeanalysis     # codeanalysis (Go + Java bridges)
make help                   # all targets
```

## Testing

- Frameworks: JUnit 5 (Java), Go tests and pytest (codeanalysis)
- Coverage target: 80%
- Details: [TESTING.md](TESTING.md)

```bash
make test        # all tests
make test-quick  # quick tests (codeanalysis)
```
