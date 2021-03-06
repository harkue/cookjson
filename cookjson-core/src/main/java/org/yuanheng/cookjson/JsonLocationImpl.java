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

import javax.json.stream.JsonLocation;

/**
 * @author	Heng Yuan
 */
class JsonLocationImpl implements JsonLocation
{
	public final static JsonLocationImpl Unknown = new JsonLocationImpl ();

	static
	{
		Unknown.m_columnNumber = -1;
		Unknown.m_lineNumber = -1;
		Unknown.m_streamOffset = -1;
	}

	long m_lineNumber;
	long m_columnNumber;
	long m_streamOffset;

	@Override
	public long getLineNumber ()
	{
		return m_lineNumber;
	}

	@Override
	public long getColumnNumber ()
	{
		return m_columnNumber;
	}

	@Override
	public long getStreamOffset ()
	{
		return m_streamOffset;
	}

	@Override
	public String toString ()
	{
		return "line " + m_lineNumber + ", column " + m_columnNumber + ", offset " + m_streamOffset;
	}
}
