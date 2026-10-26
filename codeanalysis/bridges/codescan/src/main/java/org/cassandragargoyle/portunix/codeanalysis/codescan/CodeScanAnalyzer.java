/*
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.cassandragargoyle.devtools.code.ClientFile;
import org.cassandragargoyle.devtools.code.CodeIssue;
import org.cassandragargoyle.devtools.code.CommentAnnotationIssue;
import org.cassandragargoyle.devtools.code.RuleFindCommentAnnotations;
import org.cassandragargoyle.portunix.codeanalysis.codescan.AnalyzeCommand.AnalysisIssue;
import org.cassandragargoyle.portunix.codeanalysis.codescan.AnalyzeCommand.AnalysisRequest;
import org.cassandragargoyle.portunix.codeanalysis.codescan.AnalyzeCommand.PatternConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CodeScan analyzer for annotation extraction and custom pattern matching.
 *
 * Supports:
 * - Built-in annotation detection (TODO, FIXME, HACK, XXX, NOTE, BUG)
 * - Custom regex patterns with configurable severity
 * - Severity mapping for annotations
 */
public class CodeScanAnalyzer
{

	private static final Logger log = LoggerFactory.getLogger(CodeScanAnalyzer.class);

	// Default annotations with their patterns
	private static final Map<String, Pattern> DEFAULT_ANNOTATION_PATTERNS = new LinkedHashMap<>();

	static
	{
		DEFAULT_ANNOTATION_PATTERNS.put("TODO", Pattern.compile("\\bTODO\\b[:\\s]*(.*)"));
		DEFAULT_ANNOTATION_PATTERNS.put("FIXME", Pattern.compile("\\bFIXME\\b[:\\s]*(.*)"));
		DEFAULT_ANNOTATION_PATTERNS.put("HACK", Pattern.compile("\\bHACK\\b[:\\s]*(.*)"));
		DEFAULT_ANNOTATION_PATTERNS.put("XXX", Pattern.compile("\\bXXX\\b[:\\s]*(.*)"));
		DEFAULT_ANNOTATION_PATTERNS.put("NOTE", Pattern.compile("\\bNOTE\\b[:\\s]*(.*)"));
		DEFAULT_ANNOTATION_PATTERNS.put("BUG", Pattern.compile("\\bBUG\\b[:\\s]*(.*)"));
	}

	// Default severity for annotations
	private static final Map<String, String> DEFAULT_SEVERITY = new LinkedHashMap<>();

	static
	{
		DEFAULT_SEVERITY.put("TODO", "INFO");
		DEFAULT_SEVERITY.put("FIXME", "MAJOR");
		DEFAULT_SEVERITY.put("HACK", "MAJOR");
		DEFAULT_SEVERITY.put("XXX", "MINOR");
		DEFAULT_SEVERITY.put("NOTE", "INFO");
		DEFAULT_SEVERITY.put("BUG", "CRITICAL");
	}

	private final AnalysisRequest request;

	private final Set<String> enabledAnnotations;

	private final Map<String, String> severityMapping;

	private final List<CompiledPattern> customPatterns;

	public CodeScanAnalyzer(AnalysisRequest request)
	{
		this.request = request;

		// Determine enabled annotations
		if (request.annotations != null && !request.annotations.isEmpty())
		{
			this.enabledAnnotations = new HashSet<>(request.annotations);
		}
		else
		{
			// Default: TODO, FIXME, HACK, XXX
			this.enabledAnnotations = new HashSet<>(Arrays.asList("TODO", "FIXME", "HACK", "XXX"));
		}

		// Merge severity mapping
		this.severityMapping = new HashMap<>(DEFAULT_SEVERITY);
		if (request.severityMapping != null)
		{
			this.severityMapping.putAll(request.severityMapping);
		}

		// Compile custom patterns
		this.customPatterns = compilePatterns(request.patterns);
	}

	public List<AnalysisIssue> analyze()
	{
		List<AnalysisIssue> issues = new ArrayList<>();

		for (String filePath : request.files)
		{
			try
			{
				List<AnalysisIssue> fileIssues = analyzeFile(filePath);
				issues.addAll(fileIssues);
			}
			catch (Exception e)
			{
				log.warn("Failed to analyze file: {} - {}", filePath, e.getMessage());
			}
		}

		return issues;
	}

	private List<AnalysisIssue> analyzeFile(String filePath) throws IOException
	{
		List<AnalysisIssue> issues = new ArrayList<>();
		Path path = Path.of(filePath);

		if (!Files.exists(path))
		{
			log.warn("File not found: {}", filePath);
			return issues;
		}

		String language = detectLanguage(filePath);

		// Use JavaParser-based analysis for Java files to avoid false positives
		if ("java".equals(language))
		{
			issues.addAll(analyzeJavaFileWithAst(filePath));
		}
		else
		{
			// Fallback to line-by-line for non-Java files
			issues.addAll(analyzeFileLineByLine(filePath, language));
		}

		// Always check custom patterns (line by line)
		String content = Files.readString(path, StandardCharsets.UTF_8);
		String[] lines = content.split("\n", -1);
		for (int i = 0; i < lines.length; i++)
		{
			issues.addAll(checkCustomPatterns(filePath, language, lines[i], i + 1));
		}

		return issues;
	}

	/**
	 * Analyze Java file using JavaParser AST - only scans actual comments.
	 * This avoids false positives from string literals and pattern definitions.
	 */
	private List<AnalysisIssue> analyzeJavaFileWithAst(String filePath)
	{
		List<AnalysisIssue> issues = new ArrayList<>();

		try
		{
			CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
			ClientFile clientFile = new ClientFile(new File(filePath));

			RuleFindCommentAnnotations rule = new RuleFindCommentAnnotations();
			rule.init(cu, clientFile);
			rule.execute();

			for (CodeIssue<?> codeIssue : rule.getIssues())
			{
				if (codeIssue instanceof CommentAnnotationIssue)
				{
					CommentAnnotationIssue cai = (CommentAnnotationIssue) codeIssue;
					String annotationType = cai.getAnnotationType();

					// Only include if annotation is enabled
					if (!enabledAnnotations.contains(annotationType))
					{
						continue;
					}

					AnalysisIssue issue = new AnalysisIssue();
					issue.ruleKey = "codescan:" + annotationType;
					issue.message = buildAnnotationMessage(annotationType, cai.getMessage());
					issue.filePath = filePath;
					issue.startLine = cai.getLine();
					issue.endLine = cai.getLine();
					issue.startColumn = 1;
					issue.endColumn = 1;
					issue.severity = severityMapping.getOrDefault(annotationType, "INFO");
					issue.language = "java";
					issue.matchedText = annotationType + ": " + cai.getMessage();

					issues.add(issue);
				}
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to parse Java file with AST: {} - {}", filePath, e.getMessage());
			// Fallback to line-by-line
			try
			{
				issues.addAll(analyzeFileLineByLine(filePath, "java"));
			}
			catch (IOException ioe)
			{
				log.error("Failed to analyze file: {}", filePath, ioe);
			}
		}

		return issues;
	}

	/**
	 * Fallback line-by-line analysis for non-Java files.
	 */
	private List<AnalysisIssue> analyzeFileLineByLine(String filePath, String language) throws IOException
	{
		List<AnalysisIssue> issues = new ArrayList<>();
		String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
		String[] lines = content.split("\n", -1);

		for (int i = 0; i < lines.length; i++)
		{
			issues.addAll(checkAnnotations(filePath, language, lines[i], i + 1));
		}

		return issues;
	}

	private List<AnalysisIssue> checkAnnotations(String filePath, String language,
												 String line, int lineNum)
	{
		List<AnalysisIssue> issues = new ArrayList<>();

		for (String annotation : enabledAnnotations)
		{
			Pattern pattern = DEFAULT_ANNOTATION_PATTERNS.get(annotation.toUpperCase());
			if (pattern == null)
			{
				// Custom annotation without built-in pattern
				pattern = Pattern.compile("\\b" + Pattern.quote(annotation) + "\\b[:\\s]*(.*)");
			}

			Matcher matcher = pattern.matcher(line);
			if (matcher.find())
			{
				String matchedText = matcher.group(0);
				String description = matcher.groupCount() > 0 ? matcher.group(1).trim() : "";

				AnalysisIssue issue = new AnalysisIssue();
				issue.ruleKey = "codescan:" + annotation.toUpperCase();
				issue.message = buildAnnotationMessage(annotation, description);
				issue.filePath = filePath;
				issue.startLine = lineNum;
				issue.endLine = lineNum;
				issue.startColumn = matcher.start() + 1;
				issue.endColumn = matcher.end() + 1;
				issue.severity = severityMapping.getOrDefault(annotation.toUpperCase(), "INFO");
				issue.language = language;
				issue.matchedText = matchedText;

				issues.add(issue);
			}
		}

		return issues;
	}

	private List<AnalysisIssue> checkCustomPatterns(String filePath, String language,
													String line, int lineNum)
	{
		List<AnalysisIssue> issues = new ArrayList<>();

		for (CompiledPattern cp : customPatterns)
		{
			Matcher matcher = cp.pattern.matcher(line);
			while (matcher.find())
			{
				AnalysisIssue issue = new AnalysisIssue();
				issue.ruleKey = "codescan:custom:" + cp.name;
				issue.message = cp.message != null ? cp.message : "Pattern matched: " + cp.name;
				issue.filePath = filePath;
				issue.startLine = lineNum;
				issue.endLine = lineNum;
				issue.startColumn = matcher.start() + 1;
				issue.endColumn = matcher.end() + 1;
				issue.severity = cp.severity;
				issue.language = language;
				issue.matchedText = matcher.group(0);

				issues.add(issue);
			}
		}

		return issues;
	}

	private String buildAnnotationMessage(String annotation, String description)
	{
		String upper = annotation.toUpperCase();
		String base;
		switch (upper)
		{
			case "TODO":
				base = "TODO comment found";
				break;
			case "FIXME":
				base = "FIXME comment requires attention";
				break;
			case "HACK":
				base = "HACK comment indicates workaround";
				break;
			case "XXX":
				base = "XXX comment marks problematic code";
				break;
			case "NOTE":
				base = "NOTE comment for documentation";
				break;
			case "BUG":
				base = "BUG comment indicates known issue";
				break;
			default:
				base = upper + " annotation found";
		}

		if (description != null && !description.isEmpty())
		{
			return base + ": " + description;
		}
		return base;
	}

	private String detectLanguage(String filePath)
	{
		String lower = filePath.toLowerCase();
		if (lower.endsWith(".java"))
			return "java";
		if (lower.endsWith(".ts") || lower.endsWith(".tsx"))
			return "typescript";
		if (lower.endsWith(".js") || lower.endsWith(".jsx"))
			return "javascript";
		if (lower.endsWith(".py"))
			return "python";
		if (lower.endsWith(".go"))
			return "go";
		if (lower.endsWith(".kt"))
			return "kotlin";
		if (lower.endsWith(".rb"))
			return "ruby";
		if (lower.endsWith(".php"))
			return "php";
		if (lower.endsWith(".cs"))
			return "csharp";
		if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx"))
			return "cpp";
		if (lower.endsWith(".c") || lower.endsWith(".h"))
			return "c";
		if (lower.endsWith(".rs"))
			return "rust";
		if (lower.endsWith(".swift"))
			return "swift";
		if (lower.endsWith(".scala"))
			return "scala";
		return "unknown";
	}

	private List<CompiledPattern> compilePatterns(List<PatternConfig> patterns)
	{
		List<CompiledPattern> compiled = new ArrayList<>();

		if (patterns == null)
		{
			return compiled;
		}

		for (PatternConfig config : patterns)
		{
			try
			{
				Pattern pattern = Pattern.compile(config.pattern);
				compiled.add(new CompiledPattern(
					config.name != null ? config.name : "pattern-" + compiled.size(),
					pattern,
					config.severity != null ? config.severity : "INFO",
					config.message
				));
			}
			catch (Exception e)
			{
				log.warn("Invalid regex pattern '{}': {}", config.pattern, e.getMessage());
			}
		}

		return compiled;
	}

	private static class CompiledPattern
	{
		final String name;

		final Pattern pattern;

		final String severity;

		final String message;

		CompiledPattern(String name, Pattern pattern, String severity, String message)
		{
			this.name = name;
			this.pattern = pattern;
			this.severity = severity;
			this.message = message;
		}
	}
}
