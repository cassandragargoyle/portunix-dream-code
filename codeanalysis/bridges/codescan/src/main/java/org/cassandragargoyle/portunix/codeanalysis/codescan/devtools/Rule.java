/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan.devtools;

import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * Analysis rule executed on a parsed Java compilation unit.
 */
public interface Rule
{

	void init(CompilationUnit compilationUnit, ClientFile clientFile);

	void execute();

	List<CodeIssue<?>> getIssues();
}
