# Issue #007: Rename JavaCodeScanAnalyzer to CodeScanAnalyzer

**Status:** Open
**Priority:** Low
**GitHub**: #7
**Component:** codeanalysis/bridges/javacodescan
**Created:** 2026-01-20

## Summary

Rename the `JavaCodeScanAnalyzer` class to `CodeScanAnalyzer` to remove redundant "Java" prefix and improve naming consistency.

## Current State

The class is located at:
```
codeanalysis/bridges/javacodescan/src/main/java/org/cassandragargoyle/portunix/codeanalysis/javacodescan/JavaCodeScanAnalyzer.java
```

The "Java" prefix is redundant because:
- The class is already in the `javacodescan` package
- The bridge is specifically for Java code scanning
- Follows the naming simplification done when TvkCode was renamed to CodeScan

## Required Changes

### 1. Rename Class File

**From:**
```
JavaCodeScanAnalyzer.java
```

**To:**
```
CodeScanAnalyzer.java
```

### 2. Update Class Declaration

```java
// Before
public class JavaCodeScanAnalyzer

// After
public class CodeScanAnalyzer
```

### 3. Update Logger

```java
// Before
private static final Logger log = LoggerFactory.getLogger(JavaCodeScanAnalyzer.class);

// After
private static final Logger log = LoggerFactory.getLogger(CodeScanAnalyzer.class);
```

### 4. Update Constructor

```java
// Before
public JavaCodeScanAnalyzer(AnalysisRequest request)

// After
public CodeScanAnalyzer(AnalysisRequest request)
```

### 5. Update Javadoc

```java
// Before
/**
 * JavaCodeScan analyzer for annotation extraction...

// After
/**
 * CodeScan analyzer for annotation extraction...
```

### 6. Update References in AnalyzeCommand.java

Update any instantiation or references to use the new class name.

## Files to Modify

1. `codeanalysis/bridges/javacodescan/src/main/java/org/cassandragargoyle/portunix/codeanalysis/javacodescan/JavaCodeScanAnalyzer.java`
   - Rename file to `CodeScanAnalyzer.java`
   - Update class name, constructor, logger, and javadoc

2. `codeanalysis/bridges/javacodescan/src/main/java/org/cassandragargoyle/portunix/codeanalysis/javacodescan/AnalyzeCommand.java`
   - Update reference to use `CodeScanAnalyzer`

## Acceptance Criteria

- [ ] Class renamed from `JavaCodeScanAnalyzer` to `CodeScanAnalyzer`
- [ ] File renamed from `JavaCodeScanAnalyzer.java` to `CodeScanAnalyzer.java`
- [ ] All internal references updated
- [ ] Build passes: `mvn clean compile -pl :javacodescan-bridge`
- [ ] Tests pass (if any)

## Related

- Issue #004: Code Analysis Plugin (parent feature)
