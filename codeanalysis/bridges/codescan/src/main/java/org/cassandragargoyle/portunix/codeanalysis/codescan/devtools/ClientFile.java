/*
 *  SPDX-License-Identifier: MIT
 *  This file is part of CassandraGargoyle Community Project
 *  Licensed under the MIT License - see LICENSE file for details
 */
package org.cassandragargoyle.portunix.codeanalysis.codescan.devtools;

import java.io.File;

/**
 * Source file processed by a rule.
 */
public class ClientFile
{

	private final File file;

	public ClientFile(File file)
	{
		this.file = file;
	}

	public String uri()
	{
		return file == null ? "" : file.getAbsolutePath();
	}
}
