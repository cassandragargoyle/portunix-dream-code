# Changelog

All notable changes to `ptx-codeinfo` are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `loc <file>` command: counts code, comment and empty lines of a Java source file
- `--verbose` option with a detailed breakdown of line counts
- `--help-ai` option with machine-readable help in JSON format
- Examples and exit codes in `loc --help`
- Colored `Error:` and `Warning:` prefixes on terminals (respects `NO_COLOR`)
- GraalVM Native Image build (`make build-codeinfo-native`)
- Native binary check against the JAR (`make test-codeinfo-native`)
- Test coverage report with 95% line coverage gate (`make coverage-codeinfo`)

### Changed

- Lower memory use on large files: the parser no longer stores the token list, and the
  native image limits the young generation to 8 MB (5000 LOC: 110 MB -> 35 MB,
  132 ms -> 58 ms)

### Fixed

- Comment detection is column based, so several comments on one line and comment
  markers inside string literals are classified correctly
- Files with syntax errors are counted with a warning instead of failing
- Text blocks and other modern syntax no longer fail to parse
- Native binary no longer crashes on JavaParser validators
- Comments written with unicode escapes (`\u002F\u002F`) are comments, as in `javac`
- An unclosed block comment makes all following lines comment lines instead of code
- `.java` extension is matched case insensitively (`Example.JAVA` is accepted)
- Unicode escapes that decode to whitespace (`\u0020`, `\u000A`) count as whitespace
- An invalid unicode escape (`\u00G1`) prints the syntax error warning, as `javac` rejects it
- Only Java whitespace (space, tab, form feed) makes a line empty; characters such as
  U+2028 or U+3000 count as code, as `javac` rejects them
