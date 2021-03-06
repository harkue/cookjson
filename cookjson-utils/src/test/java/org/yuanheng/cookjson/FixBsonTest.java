/*
 * Copyright 2016 Heng Yuan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yuanheng.cookjson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.json.stream.JsonGenerator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junitx.framework.FileAssert;

/**
 * @author	Heng Yuan
 */
public class FixBsonTest
{
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder ();

	@Test
	public void testHelp () throws Exception
	{
		FixBson.main (new String[]{ "-h" });
	}

	private void runConvert (String src, File dstFile) throws Exception
	{
		File srcFile = new File (src.replace ('/', File.separatorChar));

		// first generate the BSON file
		CookJsonParser p = TextJsonConfigHandler.getJsonParser (new FileInputStream (srcFile));
		JsonGenerator g = new BsonGenerator (new FileOutputStream (dstFile));
		Utils.convert (p, g);
		p.close ();
		g.close ();
	}

	@Test
	public void testFixBson () throws Exception
	{
		File dstFile = testFolder.newFile ("output.bson");
		runConvert ("../tests/data/complex1.json", dstFile);

		FixBson.main (new String[]{ dstFile.getPath () });

		File actualFile = new File ("../tests/data/complex1.bson".replace ('/', File.separatorChar));
		FileAssert.assertBinaryEquals (dstFile, actualFile);
	}
}
