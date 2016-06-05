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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;

/**
 * @author	Heng Yuan
 */
public class BOM
{
	public final static Charset utf8 = Charset.forName ("utf-8");
	public final static Charset utf16le = Charset.forName ("utf-16le");
	public final static Charset utf16be = Charset.forName ("utf-16be");
	public final static Charset utf32le = Charset.forName ("utf-32le");
	public final static Charset utf32be = Charset.forName ("utf-32be");

	/**
	 * Write the BOM for a given character set.
	 *
	 * @param	os
	 *			an output stream
	 * @param	charset
	 * 			the character set for the output stream
	 * @return	the number of bytes for BOM written.
	 * @throws	IOException
	 * 			in case of error.
	 */
	public int write (OutputStream os, Charset charset) throws IOException
	{
		byte[] bytes = null;

		if (utf8.equals (charset))
		{
			bytes = new byte[]{ (byte) 0xef, (byte) 0xbb, (byte) 0xbf };
		}
		else if (utf16le.equals (charset))
		{
			bytes = new byte[]{ (byte) 0xff, (byte) 0xfe };
		}
		else if (utf16be.equals (charset))
		{
			bytes = new byte[]{ (byte) 0xfe, (byte) 0xff };
		}
		else if (utf32le.equals (charset))
		{
			bytes = new byte[]{ (byte) 0xff, (byte) 0xfe, 0, 0 };
		}
		else if (utf32be.equals (charset))
		{
			bytes = new byte[]{ 0, 0, (byte) 0xfe, (byte) 0xff };
		}

		if (bytes != null)
		{
			os.write (bytes);
			return bytes.length;
		}
		return 0;
	}

	/**
	 * It should be noted that (rfc4627) does not requires BOM.  Instead,
	 * it uses a pattern to determine if a JSON file is encoded in
	 * utf-8, utf-16le, utf-16be, utf-32le, utf-32be.
	 *
	 * @param	is
	 *			a PushbackInputStream to allow bytes to be unread.
	 * @return	the character set of the input stream.
	 * @throws	IOException
	 *			in case of I/O error.
	 */
	public static Charset guessCharset (PushbackInputStream is) throws IOException
	{
		int b1;
		int b2;

		b1 = is.read ();
		if (b1 < 0)
			throw new IOException ("Json minimum size is 2");
		b2 = is.read ();
		if (b2 < 0)
			throw new IOException ("Json minimum size is 2");

		if (b1 == 0)
		{
			is.unread (b2);
			is.unread (b1);
			if (b2 == 0)
			{
				return utf32be;
			}
			return utf16be;
		}
		else
		{
			if (b2 == 0)
			{
				int b3 = is.read ();
				if (b3 < 0)
					throw new IOException ("Not a json file.");
				is.unread (b3);
				is.unread (b2);
				is.unread (b1);
				if (b3 == 0)
					return utf32le;
				return utf16le;
			}
			else
			{
				is.unread (b2);
				is.unread (b1);
				return utf8;
			}
		}
	}
}
