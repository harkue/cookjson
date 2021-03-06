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

import java.io.*;

import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author	Heng Yuan
 */
public class PrettyTextJsonGenerator2Test
{
	private TextJsonParser getJsonParser (File file) throws IOException
	{
		return new TextJsonParser (new InputStreamReader (new FileInputStream (file), BOM.utf8));
	}

	private void testFile (String f) throws IOException
	{
		File file = new File (f.replace ('/', File.separatorChar));

		StringWriter out1 = new StringWriter ();
		TextJsonParser p1 = getJsonParser (file);
		p1.next ();
		JsonValue v = p1.getValue ();
		TextJsonGenerator g1 = new PrettyTextJsonGenerator (out1);
		g1.write (v);
		p1.close ();
		g1.close ();

		StringWriter out2 = new StringWriter ();
		TextJsonParser p2 = getJsonParser (file);
		JsonGenerator g2 = new PrettyTextJsonGenerator (out2);
		Utils.convert (p2, g2);
		p2.close ();
		g2.close ();

		// because JsonObject ordering is based on hash, we cannot directly
		// compare the output.  Instead, we compare the length.
		Assert.assertEquals (out1.toString ().length (), out2.toString ().length ());
	}

	void testBsonFile (String f) throws IOException
	{
		File file = new File (f.replace ('/', File.separatorChar));

		StringWriter out1 = new StringWriter ();
		BsonParser p1 = new BsonParser (new FileInputStream (file));
		p1.next ();
		JsonValue v = p1.getValue ();
		TextJsonGenerator g1 = new PrettyTextJsonGenerator (out1);
		g1.write (v);
		p1.close ();
		g1.close ();

		ByteArrayOutputStream bos = new ByteArrayOutputStream ();
		BsonParser p2 = new BsonParser (new FileInputStream (file));
		JsonGenerator g2 = new PrettyTextJsonGenerator (bos);
		Utils.convert (p2, g2);
		p2.close ();
		g2.close ();

		// because JsonObject ordering is based on hash, we cannot directly
		// compare the output.  Instead, we compare the length.
		Assert.assertEquals (new String (bos.toByteArray (), BOM.utf8).length (), out1.toString ().length ());
	}

	@Test
	public void test () throws IOException
	{
		testFile ("../tests/data/complex1.json");
		testFile ("../tests/data/double.json");
		testFile ("../tests/data/empty.json");
		testFile ("../tests/data/long.json");
		testFile ("../tests/data/nested1.json");
		testFile ("../tests/data/nested2.json");

		testBsonFile ("../tests/data/binary.bson");
	}
}
