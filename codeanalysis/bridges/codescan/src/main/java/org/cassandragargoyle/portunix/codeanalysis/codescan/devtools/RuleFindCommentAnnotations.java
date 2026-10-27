/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan.devtools;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds TODO, FIXME, HACK and XXX annotations in comments of a Java compilation unit.
 *
 * Only real comments are scanned, so string literals and pattern definitions do not
 * produce false positives.
 */
public class RuleFindCommentAnnotations implements Rule
{

	public enum AnnotationType
	{
		TODO, FIXME, HACK, XXX
	}

	private static final Map<AnnotationType, Pattern> ANNOTATION_PATTERNS = new EnumMap<>(AnnotationType.class);

	static
	{
		ANNOTATION_PATTERNS.put(AnnotationType.TODO, Pattern.compile("\\bTODO\\b[:\\s]*(.*)"));
		ANNOTATION_PATTERNS.put(AnnotationType.FIXME, Pattern.compile("\\bFIXME\\b[:\\s]*(.*)"));
		ANNOTATION_PATTERNS.put(AnnotationType.HACK, Pattern.compile("\\bHACK\\b[:\\s]*(.*)"));
		ANNOTATION_PATTERNS.put(AnnotationType.XXX, Pattern.compile("\\bXXX\\b[:\\s]*(.*)"));
	}

	private final List<CodeIssue<?>> issues = new ArrayList<>();

	private CompilationUnit compilationUnit;

	private ClientFile clientFile;

	@Override
	public void init(CompilationUnit compilationUnit, ClientFile clientFile)
	{
		issues.clear();
		this.compilationUnit = compilationUnit;
		this.clientFile = clientFile;
	}

	@Override
	public void execute()
	{
		if (compilationUnit == null)
		{
			return;
		}

		for (Comment comment : compilationUnit.getAllComments())
		{
			int startLine = comment.getBegin().map(p -> p.line).orElse(0);
			scanCommentContent(comment.getContent(), startLine);
		}
	}

	private void scanCommentContent(String content, int startLine)
	{
		String[] lines = content.split("\n");
		for (int i = 0; i < lines.length; i++)
		{
			int lineNum = startLine + i;
			for (Map.Entry<AnnotationType, Pattern> entry : ANNOTATION_PATTERNS.entrySet())
			{
				Matcher matcher = entry.getValue().matcher(lines[i]);
				if (matcher.find())
				{
					String message = matcher.group(1).trim();
					issues.add(new CommentAnnotationIssue(clientFile, entry.getKey().name(), lineNum, message));
				}
			}
		}
	}

	@Override
	public List<CodeIssue<?>> getIssues()
	{
		return issues;
	}
}
