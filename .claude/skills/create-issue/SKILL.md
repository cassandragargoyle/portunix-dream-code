---
name: create-issue
description: 'Založení nového issue metodou GitHub-first — nejdřív vznikne GitHub issue (GitHub přidělí číslo), pak se dopíše detailní zápis do docs/issues/NNN-*.md. Použij když chce uživatel vytvořit/založit issue, feature request nebo bug. Příklady: "založ issue …", "/create-issue codeinfo JSON output", "vytvoř issue na …".'
argument-hint: [krátký popis issue]
allowed-tools: Read, Glob, Grep, Bash, AskUserQuestion, Write, Edit
user-invocable: true
---

# Založení issue (GitHub-first)

**DŮLEŽITÉ: NIKDY nepřidávej "Co-Authored-By: Claude <noreply@anthropic.com>" do kódových souborů ani git commitů.**

**Argument:** $ARGUMENTS

## Princip

**GitHub je jediný zdroj pravdy pro seznam issues.** Číslo přiděluje GitHub při
založení. Na GitHubu je jen **základ** (titulek + krátký popis + cesta k detailnímu
souboru). Detaily žijí v `docs/issues/NNN-*.md`, kde `NNN` = číslo GitHub issue
doplněné nulami na tři místa.

**Issues i kód jsou na GitHubu** (`cassandragargoyle/portunix-dream-code`, **public**).
V těle issue uváděj **cestu** k detailnímu souboru, ne webový odkaz.

**Nepřidávej žádné přehledové tabulky** do README a nepoužívej adresář `internal/`
(zrušen).

Repozitář na GitHubu je veřejný — do těla issue ani do detailního souboru nedávej
přihlašovací údaje, osobní data, interní hostname, absolutní lokální cesty ani odkazy
do privátních repozitářů.

## KROK 1: Zjisti podklady

Z argumentu / kontextu (nebo přes `AskUserQuestion`, pokud chybí) zjisti:

- **Titulek** — krátký, výstižný, anglicky (bez čísla; GitHub ho doplní sám)
- **Slug** — kebab-case pro název souboru (odvoď z titulku, např. `codeinfo-json-output`)
- **Typ** → label: `bug` | `enhancement` | `documentation` | `testing`
- **Priorita** → label: `critical` | `high` | `medium` | `low`
- **Krátký popis** (1–3 věty) — půjde do těla GitHub issue
- **Detailní obsah** — půjde do lokálního `.md` (problém, kroky, akceptační kritéria, testy)

Všechny texty **anglicky** (projektová konvence).

## KROK 2: Preflight — ověř `gh`

```bash
gh --version && gh auth status
```

Pokud není přihlášeno, přeruš a vyzvi uživatele: `gh auth login` (klidně přes `!` prefix).

## KROK 3: Založ GitHub issue (GitHub přidělí číslo)

**STOP** — nejdřív ukaž titulek, tělo a labely a požádej o potvrzení. Issue je veřejné.

```bash
REPO="cassandragargoyle/portunix-dream-code"
gh issue create --repo "$REPO" \
  --title "<TITULEK>" \
  --body "<KRÁTKÝ POPIS>" \
  --label <type>,<priority>
```

`gh issue create` vypíše URL — z ní vytáhni **číslo `N`** (`.../issues/12` → `NNN=012`
pro název souboru, `#12` na GitHubu).

## KROK 4: Napiš detailní soubor

Vytvoř `docs/issues/<NNN>-<slug>.md` s plným obsahem (šablona v
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

## KROK 5: Doplň do GitHub issue cestu k souboru

```bash
gh issue edit <N> --repo "$REPO" \
  --body "<KRÁTKÝ POPIS>

📄 Full details: \`docs/issues/<NNN>-<slug>.md\` (repository path)"
```

## KROK 6: Shrnutí

```text
Issue #<N> založeno.

GitHub:  https://github.com/cassandragargoyle/portunix-dream-code/issues/<N>
Detail:  docs/issues/<NNN>-<slug>.md
Typ/Priorita: <type> / <priority>

Další kroky:
  /implement-issue <N>
```

Soubor necommituj automaticky — commit nech na uživateli, pokud neřekne jinak.

## Poznámky

- Číslo **nikdy nevymýšlej** — vždy ho dá GitHub. Issues a PR sdílejí jeden čítač.
- Archivace hotového issue: `gh issue close <N>` +
  `git mv docs/issues/<NNN>-*.md docs/issues/done/` (viz skill `finish-branch`).
- Kompletní metodika: `docs/contributing/ISSUE-MANAGEMENT.md`.
