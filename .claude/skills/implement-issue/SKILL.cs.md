---
name: implement-issue
description: Průvodce implementací issue - od přečtení zadání přes vytvoření branch, implementaci kódu, commit až po přípravu PR popisu.
argument-hint: "[issue-id]"
disable-model-invocation: true
---

<!-- Jazyk komunikace určuje AGENTS.md (sekce Communication Language), roli CLAUDE.local.md. Tento soubor přidává workflow. -->

## KROK 0: Kontrola role

Zkontroluj aktuální roli v `CLAUDE.local.md`. Pokud role není **Developer**:

1. Informuj uživatele, že implementace vyžaduje roli Developer
2. Nabídni přepnutí role příkazem `/role developer`
3. **STOP** - Nepokračuj dokud není role Developer aktivní

## KROK 1: Získej issue

Pokud bylo ID issue předáno jako argument (`$ARGUMENTS`), použij ho a pokračuj krokem 2.

Jinak se zeptej na číslo GitHub issue (např. `7` nebo `#7`). Počkej na mou odpověď.

## KROK 2: Potvrzení porozumění

Poté, co ti poskytnu issue:

1. Přečti soubor issue `docs/issues/<NNN>-*.md` (`<NNN>` = číslo doplněné nulami na tři
   místa). Pokud tam není, zkus `docs/issues/done/<NNN>-*.md` — může jít o už uzavřené
   issue. Stav a assignee ověř na GitHubu:
   `gh issue view <N> --repo cassandragargoyle/portunix-dream-code --json state,assignees,title`
2. Přeformuluj issue vlastními slovy
3. Vysvětli potřebné změny v kódu
4. Požádej mě o potvrzení

**STOP** - Počkej na mé výslovné potvrzení než budeš pokračovat.

## KROK 3: Vytvoř branch

Vytvoř git branch: `feature/<NNN>-<kratky-popis>` nebo `fix/<NNN>-<kratky-popis>`

- `<NNN>` je číslo GitHub issue doplněné nulami na tři místa (`#12` → `012`)
- Použij malá písmena a pomlčky pro popis
- Příklad: `feature/012-codeinfo-json-output`

Pak si issue přiřaď a označ jako rozpracované:

```bash
gh issue edit <N> --repo cassandragargoyle/portunix-dream-code --add-assignee @me --add-label in-progress
```

Zobraz git příkaz před jeho spuštěním.

## KROK 4: Implementace

- Proveď všechny změny kódu v jedné souvislé fázi
- NEPTEJ se na otázky, pokud to není skutečně nejednoznačné
- Modifikuj pouze relevantní soubory
- Dodržuj coding style projektu

Na konci poskytni shrnutí:

- Modifikované soubory
- Přidané metody/třídy
- Odstraněný kód
- Klíčová rozhodnutí

Zeptej se mě, co chci:

- a) Přijmout tak jak je
- b) Upravit konkrétní části
- c) Předělat kompletně

**STOP** - Počkej na mé schválení než budeš pokračovat.

## KROK 5: Commit

Připrav commit message:

```
<type>(<N>): <krátké shrnutí>

- klíčová změna 1
- klíčová změna 2

refs #<N>
```

Povolené typy: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

**STOP** - Zeptej se mě na potvrzení commit message před commitnutím.

## KROK 6: PR popis

Připrav PR/MR popis:

- **Summary**: Co tento PR dělá
- **Motivation**: Proč (odkaz na issue)
- **Changes**: Seznam změn
- **Testing**: Kroky pro ověření
