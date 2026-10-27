/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan.devtools;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RuleFindCommentAnnotationsTest
{

	private static List<CommentAnnotationIssue> scan(String source)
	{
		RuleFindCommentAnnotations rule = new RuleFindCommentAnnotations();
		rule.init(StaticJavaParser.parse(source), new ClientFile(new File("Sample.java")));
		rule.execute();
		return rule.getIssues().stream()
			.map(CommentAnnotationIssue.class::cast)
			.collect(Collectors.toList());
	}

	@Test
	void testFindsAllAnnotationTypesInComments()
	{
		String source = String.join("\n",
			"class Sample {",
			"    // TODO: first",
			"    // FIXME second",
			"    /* HACK: third */",
			"    // XXX fourth",
			"}");

		List<CommentAnnotationIssue> issues = scan(source);

		assertEquals(4, issues.size());
		assertEquals("TODO", issues.get(0).getAnnotationType());
		assertEquals(2, issues.get(0).getLine());
		assertEquals("first", issues.get(0).getMessage());
		assertEquals("FIXME", issues.get(1).getAnnotationType());
		assertEquals("second", issues.get(1).getMessage());
		assertEquals("HACK", issues.get(2).getAnnotationType());
		assertEquals(4, issues.get(2).getLine());
		assertEquals("XXX", issues.get(3).getAnnotationType());
	}

	@Test
	void testIgnoresAnnotationsInStringLiterals()
	{
		String source = String.join("\n",
			"class Sample {",
			"    String s = \"TODO: not a comment\";",
			"}");

		assertTrue(scan(source).isEmpty());
	}

	@Test
	void testCountsLinesInsideBlockComment()
	{
		String source = String.join("\n",
			"class Sample {",
			"    /*",
			"     * text",
			"     * FIXME: inside block",
			"     */",
			"}");

		List<CommentAnnotationIssue> issues = scan(source);

		assertEquals(1, issues.size());
		assertEquals(4, issues.get(0).getLine());
		assertEquals("inside block", issues.get(0).getMessage());
	}

	@Test
	void testIgnoresAnnotationAsPartOfWord()
	{
		String source = String.join("\n",
			"class Sample {",
			"    // TODOS and PREFIXME are not annotations",
			"}");

		assertTrue(scan(source).isEmpty());
	}

	@Test
	void testInitResetsPreviousIssues()
	{
		RuleFindCommentAnnotations rule = new RuleFindCommentAnnotations();
		ClientFile file = new ClientFile(new File("Sample.java"));

		rule.init(StaticJavaParser.parse("class A { // TODO: a\n}"), file);
		rule.execute();
		rule.init(StaticJavaParser.parse("class B {}"), file);
		rule.execute();

		assertTrue(rule.getIssues().isEmpty());
	}

	@Test
	void testIssueKeepsInputFile()
	{
		List<CommentAnnotationIssue> issues = scan("class A { // TODO: a\n}");

		assertTrue(issues.get(0).getInputFile().uri().endsWith("Sample.java"));
		assertEquals("a", issues.get(0).getValue().getMessage());
	}
}
