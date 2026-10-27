/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan.devtools;

/**
 * Issue found by a rule in a source file.
 *
 * @param <T> type of the issue data
 */
public interface CodeIssue<T>
{

	ClientFile getInputFile();

	T getValue();
}
