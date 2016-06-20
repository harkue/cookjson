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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * @author	Heng Yuan
 */
public class BsonFixLength
{
	private static class Pair implements Comparable<Pair>
	{
		long offset;
		int size;

		@Override
		public int compareTo (Pair o)
		{
			long diff = offset - o.offset;
			return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
		}

		@Override
		public String toString ()
		{
			return offset + ": " + size;
		}
	}

	private static void getOffsets (JsonParser p, ArrayList<Pair> pairs) throws IOException
	{
		long offset;
		long start;
		Stack<Long> matches = new Stack<Long> ();
		Pair pair;

		boolean firstObject = true;
		boolean justStarted = false;

		while (p.hasNext ())
		{
			Event e = p.next ();
			switch (e)
			{
				case START_ARRAY:
				case START_OBJECT:
					if (firstObject)
					{
						offset = p.getLocation ().getStreamOffset ();
						matches.push (offset);
						firstObject = false;
					}
					justStarted = true;
					break;
				case KEY_NAME:
					if (justStarted)
					{
						justStarted = false;
						offset = p.getLocation ().getStreamOffset ();
						matches.push (offset - 4);
					}
					break;
				case END_ARRAY:
				case END_OBJECT:
					offset = p.getLocation ().getStreamOffset () + 1;
					if (justStarted)
					{
						start = offset - 5;
						justStarted = false;
					}
					else
					{
						start = matches.pop ();
					}
					pair = new Pair ();
					pair.offset = start;
					pair.size = (int) (offset - start);
					pairs.add (pair);
					break;
				default:
					// value cases
					if (justStarted)
					{
						// we can only get here if we area dealing with
						// array.
						justStarted = false;
						offset = p.getLocation ().getStreamOffset ();
						matches.push (offset - 4);
					}
					break;
			}
		}
	}

	public static void fix (File file) throws IOException
	{
		FileInputStream is = new FileInputStream (file);
		JsonParser p = new BsonParser (is);

		ArrayList<Pair> pairs = new ArrayList<Pair> ();

		// compute the offsets and sizes need to be updated.
		getOffsets (p, pairs);
		p.close ();

		// sort the pairs
		Pair[] pa = pairs.toArray (new Pair[pairs.size ()]);

		Arrays.sort (pa);

		// debugging dump
//		for (Pair pair : pa)
//		{
//			System.out.println (pair);
//		}

		byte[] bytes = new byte[4];
		ByteBuffer buffer = ByteBuffer.wrap (bytes);
		RandomAccessFile f = new RandomAccessFile (file, "rw");
		FileChannel channel = f.getChannel ();
		for (int i = 0; i < pairs.size (); ++i)
		{
			Utils.setInt (bytes, pa[i].size);
			channel.write (buffer, pa[i].offset);
			buffer.position (0);
		}
		f.close ();
	}
}
