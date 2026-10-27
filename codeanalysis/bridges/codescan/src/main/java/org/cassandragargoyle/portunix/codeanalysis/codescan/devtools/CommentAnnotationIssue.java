/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan.devtools;

/**
 * Annotation (TODO, FIXME, ...) found in a comment.
 *
 * Copied from dream-code-devtools (org.cassandragargoyle.devtools.code), see issue #12.
 */
public class CommentAnnotationIssue implements CodeIssue<CommentAnnotationIssue.AnnotationData>
{

	private final ClientFile clientFile;

	private final AnnotationData data;

	public CommentAnnotationIssue(ClientFile clientFile, String annotationType, int line, String message)
	{
		this.clientFile = clientFile;
		this.data = new AnnotationData(annotationType, line, message);
	}

	@Override
	public AnnotationData getValue()
	{
		return data;
	}

	@Override
	public ClientFile getInputFile()
	{
		return clientFile;
	}

	public String getAnnotationType()
	{
		return data.annotationType;
	}

	public int getLine()
	{
		return data.line;
	}

	public String getMessage()
	{
		return data.message;
	}

	public static class AnnotationData
	{

		private final String annotationType;

		private final int line;

		private final String message;

		public AnnotationData(String annotationType, int line, String message)
		{
			this.annotationType = annotationType;
			this.line = line;
			this.message = message;
		}

		public String getAnnotationType()
		{
			return annotationType;
		}

		public int getLine()
		{
			return line;
		}

		public String getMessage()
		{
			return message;
		}
	}
}
