/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.yuanheng.cookjson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author	Heng Yuan
 */
public class BsonParserTest
{
	void testFile (String f1, String f2) throws IOException
	{
		File file1 = new File (f1.replace ('/', File.separatorChar));
		File file2 = new File (f2.replace ('/', File.separatorChar));

		StringWriter out1 = new StringWriter ();
		BsonParser p1 = new BsonParser (new FileInputStream (file1));
		p1.setRootAsArray (true);
		TextJsonGenerator g1 = new TextJsonGenerator (out1);
		Utils.convert (p1, g1);
		p1.close ();
		g1.close ();

		StringWriter out2 = new StringWriter ();
		JsonProvider provider = new org.glassfish.json.JsonProviderImpl ();
		JsonParser p2 = provider.createParser (new FileInputStream (file2));
		JsonGenerator g2 = new TextJsonGenerator (out2);
		Utils.convert (p2, g2);
		p2.close ();
		g2.close ();

		Assert.assertEquals (out1.toString (), out2.toString ());
	}

	@Test
	public void test () throws IOException
	{
		// data1.bson has 0 in Document / Array length
		testFile ("../tests/data/data1.bson", "../tests/data/data3.json");
		// data2.bson has the correct length
		testFile ("../tests/data/data2.bson", "../tests/data/data3.json");
	}
}
