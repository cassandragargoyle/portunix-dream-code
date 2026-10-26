---
name: finish-branch
description: Dokončení práce na feature branch - merge do hlavní větve, úklid branch a volitelná aktualizace stavu issue.
---

@.claude/prompts/assistant-programming.md

**DŮLEŽITÉ: NIKDY nepřidávej "Co-Authored-By: Claude <noreply@anthropic.com>" do kódových souborů ani git commitů.**

## KROK 1: Zjisti kontext

1. Zjisti aktuální branch (`git branch --show-current`) — měla by odpovídat konvenci `feature/NNN-short-name` (nebo `fix/NNN-short-name`), kde `NNN` = číslo GitHub issue doplněné nulami na tři místa, detail v `docs/issues/NNN-*.md` (viz skill `implement-issue`)
2. Cílová větev pro merge je `main` (pokud uživatel neřekne jinak)
3. Remote: `origin` = GitHub (`git@github.com:cassandragargoyle/portunix-dream-code`, public) — kód, issues i PR

Ověř, že branch existuje a obsahuje commity oproti `main`.

Vypiš commity, které se budou mergovat (`git log --oneline main..HEAD`). Pokud branch obsahuje commity, které nesouvisí s issue, upozorni na to uživatele ještě před merge.

## KROK 2: Ověření acceptance protokolu

**KRITICKÉ**: Podle role Developer (`.claude/roles/developer.md`) se NESMÍ mergovat do `main` bez tester acceptance protokolu pro dané issue.

1. Zkontroluj, zda existuje soubor `docs/testing/acceptance-<issue-number>.md`
2. Ověř, že obsahuje `**STATUS**: PASS` a `**Approval for merge**: YES`

> **K doplnění:** adresář `docs/testing/` ani metodika (`ISSUE-DEVELOPMENT-METHODOLOGY.md`) v tomto projektu zatím neexistují. Dokud nejsou, protokol od role Tester bývá jen v konverzaci — v takovém případě se uživatele zeptej, zda acceptance proběhla, pro jaký commit, a zda změny po protokolu tester znovu ověřil.

Pokud acceptance protokol **neexistuje nebo není PASS**:

- Informuj uživatele, že merge není povolený bez tester approval
- Zeptej se: "Chceš pokračovat přesto? (hotfix / výjimka schválená architektem?)"
- Pokud uživatel nepotvrdí výjimku → **STOP**

## KROK 3: Merge a úklid

Zeptej se uživatele na preferovaný postup:

### Varianta A: Přímý merge (výchozí pro solo vývoj)

```bash
git checkout main
git merge feature/<NNN>-<popis>
git branch -d feature/<NNN>-<popis>
git push origin main
git push origin --delete feature/<NNN>-<popis>
```

Poslední příkaz spusť jen pokud byla feature branch pushnutá na `origin`.

**STOP** - Počkej na potvrzení před provedením merge.

### Varianta B: Pull Request (pro týmovou spolupráci)

Připrav PR popis:

- **Summary**: Co tento PR dělá
- **Motivation**: Proč (odkaz na issue)
- **Changes**: Seznam změn
- **Testing**: Kroky pro ověření
- **Acceptance**: Odkaz na acceptance protokol

Vytvoř PR na GitHubu: `gh pr create --repo CassandraGargoyle/portunix-dream-code --base main` (v těle `Fixes #NNN`, pokud issue existuje i na GitHubu).

## KROK 4: Aktualizace issue (volitelné)

Zeptej se uživatele: "Je tímto issue kompletně dokončeno?"

### Pokud ANO:

1. Uprav soubor issue `docs/issues/NNN-*.md`:

   - Změň `**Status**` na `✅ Implemented`
   - Přidej datum uzavření
   - Odškrtni splněná acceptance criteria (`- [ ]` → `- [x]`); nesplněná nech `- [ ]` s poznámkou proč

2. Přesuň soubor do archivu `done/`. Žádné tabulky v README se neaktualizují — seznam issues je na GitHubu:

   ```bash
   git mv docs/issues/NNN-*.md docs/issues/done/
   git add docs/issues/done/NNN-*.md
   ```

   `git mv` stageuje jen přesun — `git add` je nutný kvůli úpravám z bodu 1.

3. Commitni změnu stavu spolu s přesunem (jeden commit):

   ```text
   docs(<N>): close issue — archive to done/
   ```

4. Uzavři GitHub issue (`<N>` = číslo bez úvodních nul) a oprav cestu v těle na `done/`:

   ```bash
   REPO="cassandragargoyle/portunix-dream-code"
   gh issue close <N> --repo "$REPO" --comment "Implemented, acceptance: docs/testing/acceptance-NNN.md"
   gh issue edit <N> --repo "$REPO" --remove-label in-progress
   gh issue view <N> --repo "$REPO" --json body --jq .body \
     | sed 's#docs/issues/NNN-#docs/issues/done/NNN-#' \
     | gh issue edit <N> --repo "$REPO" --body-file -
   ```

### Pokud NE:

Přeskoč aktualizaci stavu. Issue zůstává otevřené pro další práci.

## KROK 5: Shrnutí

Zobraz shrnutí:

- Merge provedený do větve: `<název>`
- Feature branch smazána: ano/ne
- Issue status aktualizován: ano/ne/přeskočeno
- Další kroky (pokud jsou)
