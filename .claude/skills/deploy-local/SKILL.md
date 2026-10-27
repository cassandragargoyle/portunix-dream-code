---
name: deploy-local
description: >-
  Deploy the locally built Portunix plugins of this project (code-analysis and
  code-info) to the local Portunix with `portunix plugin install` — check the
  installed and source versions, build the distributions, validate, install and
  verify. Use when the user runs /deploy-local or asks to deploy the local build
  (e.g. "nasaď lokálně", "deploy local", "nainstaluj pluginy lokálně").
user-invocable: true
disable-model-invocation: true
---

# Deploy Local

Deploy the locally built plugins `code-analysis` (`codeanalysis/`) and `code-info`
(`codeinfo/`) to the local Portunix with `portunix plugin install`.

## STEP 1: Check the environment

```bash
command -v portunix && portunix --version
java -version
go version
uv --version
```

`portunix`, Java 21+, Go and uv are required. If one is missing, tell the user and
**STOP**.

## STEP 2: Check the state before deployment

Get the versions in the source code:

```bash
uv run python -c "import json; [print(p, json.load(open(p))['version']) for p in ('codeanalysis/plugin.json', 'codeinfo/plugin.json')]"
```

Check the installed plugins:

```bash
portunix plugin info code-analysis
portunix plugin info code-info
```

If a plugin is not installed, the command returns an error — this is OK, it is installed
in STEP 4.

Check what is in `dist/` now:

```bash
ls dist/ptx-codeanalysis dist/ptx-codeinfo
```

Show the user, for each plugin:

- The installed version (or "not installed")
- The version in `plugin.json`

**IMPORTANT**: STEP 3 deletes and rebuilds `dist/ptx-codeanalysis/` and
`dist/ptx-codeinfo/`. If `dist/ptx-codeinfo/` contains a native binary
(`ptx-codeinfo` without `.jar`), tell the user that it is replaced by the JAR build, and
ask whether to deploy the native build instead (see [Native codeinfo](#native-codeinfo)).

## STEP 3: Build the distributions

```bash
make dist
```

This command:

1. Builds the Go binary and both Java bridges (SonarLint, CodeScan) and packs
   `dist/ptx-codeanalysis/` for the current OS and architecture
2. Builds the codeinfo JAR and packs `dist/ptx-codeinfo/`
3. Copies `plugin.json`, `README.md` and `LICENSE` into each package

If the build fails, show the error and **STOP**.

## STEP 4: Validate and deploy

```bash
portunix plugin validate dist/ptx-codeanalysis
portunix plugin validate dist/ptx-codeinfo
```

If a validation fails, show the error and **STOP** — do not install an invalid plugin.

Then deploy:

```bash
make deploy-local
```

This command installs both plugins with `portunix plugin install dist/ptx-codeanalysis`
and `portunix plugin install dist/ptx-codeinfo`.

**IMPORTANT**: `make deploy-local` runs `make dist` again and only prints a warning when an
install fails — the exit code stays 0. Always check the output for `Warning:` and verify
in STEP 5.

## STEP 5: Verify the deployment

```bash
portunix plugin list | grep -E "code-analysis|code-info"
portunix plugin info code-analysis
portunix plugin info code-info
portunix plugin health code-analysis
portunix plugin health code-info
git status --short
```

Show the user the result of the deployment. The installed versions MUST match the
versions in `plugin.json` from STEP 2. `git status` must show no file changed by the
build.

## Native codeinfo

To deploy the GraalVM native binary of codeinfo instead of the JAR (requires
`GRAALVM_HOME`, the build takes several minutes):

```bash
make dist-codeanalysis dist-codeinfo-native
portunix plugin validate dist/ptx-codeinfo
portunix plugin install dist/ptx-codeinfo
```

Do not run `make deploy-local` after this — it rebuilds the JAR distribution.

## Uninstall

```bash
make undeploy-local
```

## Notes

- Do not copy files by hand — always use the `portunix plugin` commands
- **Plugin names**: `code-analysis` and `code-info` (field `name` in `plugin.json`); the
  directories in `dist/` are `ptx-codeanalysis` and `ptx-codeinfo`
- **Version**: defined in `codeanalysis/plugin.json` and `codeinfo/plugin.json`
- **Bridges**: `code-analysis` needs Java 21+ at runtime for the SonarLint and CodeScan
  bridges
