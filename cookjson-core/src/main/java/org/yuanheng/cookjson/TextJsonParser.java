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

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.json.JsonValue;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParsingException;

import org.yuanheng.cookjson.value.CookJsonBigDecimal;
import org.yuanheng.cookjson.value.CookJsonString;

/**
 * This JsonParser adds the ability to allow parse JavaScript line and
 * block comments.
 *
 * @author	Heng Yuan
 */
public class TextJsonParser implements CookJsonParser
{
	private final static int START = 1;
	private final static int VALUE = 2;
	private final static int FIELD = 3;

	private boolean m_allowComments;

	private final Reader m_reader;
	/** append buffer for storing output string */
	private char[] m_appendBuf = new char[8192];
	/** position tracking for append buffer */
	private int m_appendPos;

	private long m_line;
	private long m_offset;
	private long m_column;
	private long savedLine;
	private long savedColumn;
	private long savedOffset;

	private final char[] m_readBuf = new char[8192];
	private int m_readPos = 0;
	private int m_readMax = 0;

	private final ArrayList<Boolean> m_states = new ArrayList<Boolean> ();
	private int m_state = ParserState.INITIAL;
	private int m_lastToken;
	private boolean m_int;

	private Event m_event;

	public TextJsonParser (Reader r)
	{
		m_reader = r;
		m_line = 1;
		m_column = 1;
	}

	private void saveLocation ()
	{
		savedLine = m_line;
		savedOffset = m_offset;
		savedColumn = m_column;
	}

	private void append (char ch)
	{
		int len = m_appendBuf.length;
		if (m_appendPos >= len)
		{
			// we need to expand the buffer by 50%
			char[] newBuffer = new char[len + len / 2];
			System.arraycopy (m_appendBuf, 0, newBuffer, 0, len);
			m_appendBuf = newBuffer;
		}
		m_appendBuf[m_appendPos++] = ch;
	}

	private IllegalStateException stateError (String function)
	{
		return new IllegalStateException (function + " cannot be called at the current state: " + m_event + ".");
	}

	private JsonParsingException ioError (String msg)
	{
		JsonLocationImpl location = new JsonLocationImpl ();
		location.m_lineNumber = m_line;
		// -1 to back track the last read character.
		location.m_columnNumber = m_column -1;
		location.m_streamOffset = m_offset - 1;
		return new JsonParsingException ("Parsing error at " + location.toString () + ": " + msg, location);
	}

	private JsonParsingException eofError ()
	{
		JsonLocation location = getCurrentLocation ();
		return new JsonParsingException ("Parsing error at " + location.toString () + ": unexpected eof.", location);
	}

	private JsonParsingException unexpected (char ch)
	{
		return ioError ("unexpected character '" + ch + "'");
	}

	private char read () throws IOException
	{
		if (m_readPos >= m_readMax)
			fill ();
		++m_offset;
		++m_column;
		return m_readBuf[m_readPos++];
	}

	private void unread ()
	{
		--m_readPos;
		--m_offset;
		--m_column;
	}

	private void fill () throws IOException
	{
		m_readPos = 0;
		m_readMax = m_reader.read (m_readBuf);
		if (m_readMax <= 0)
			throw eofError ();
	}

	private void readLineComment () throws IOException
	{
		char[] buf = m_readBuf;
		int len = 0;
		for (;;)
		{
			int readPos = m_readPos;
			int readMax = m_readMax;
			while (readPos < readMax)
			{
				char ch = buf[readPos++];
				++len;

				if (ch == '\n')
				{
					m_offset += len;
					m_column = 1;
					++m_line;
					m_readPos = readPos;
					return;
				}
			}
			fill ();
		}
	}

	private void readBlockComment () throws IOException
	{
		char[] buf = m_readBuf;
		int len = 0;
		for (;;)
		{
			int readPos = m_readPos;
			int readMax = m_readMax;
			while (readPos < readMax)
			{
				char ch = buf[readPos++];
				++len;

				// switch is useful for scanning characters that are
				// mostly handled as default.
				switch (ch)
				{
					case '\n':
					{
						m_offset += len;
						m_column = 1;
						++m_line;
						len = 0;
						break;
					}
					case '*':
					{
						char nextChar;
						if (readPos < readMax)
						{
							nextChar = buf[readPos];
						}
						else
						{
							fill ();
							readPos = m_readPos;
							readMax = m_readMax;
							nextChar = buf[readPos];
						}
						if (nextChar == '/')
						{
							m_offset += len + 1;
							m_column += len + 1;
							m_readPos = readPos + 1;
							return;
						}
						break;
					}
				}
			}
			fill ();
		}
	}

	private void readComment () throws IOException
	{
		char ch;
		ch = read ();
		if (ch == '/')
			readLineComment ();
		else if (ch == '*')
			readBlockComment ();
		else
		{
			--m_offset;
			--m_column;
			throw unexpected ('/');
		}
	}

	/**
	 * We do not want to add cost for handling comments.  So we embed the
	 * comment handling in unexpected character handling.
	 *
	 * @param	ch
	 *			the unexpected character just read
	 * @throws	IOException
	 * 			in case of error.
	 */
	private void scanUnexpected (char ch) throws IOException
	{
		if (!m_allowComments || ch != '/')
			throw unexpected (ch);
		readComment ();
	}

	private void expect (String str) throws IOException
	{
		final int len = str.length ();
		if ((m_readPos + len) >= m_readMax)
		{
			// we do the slow matching
			for (int i = 0; i < len; ++i)
			{
				char ch = read ();
				if (ch != str.charAt (i))
					throw ioError ("expecting '" + ch + "'");
			}
		}
		else
		{
			final char[] readBuf = m_readBuf;
			int readPos = m_readPos;
			for (int i = 0; i < len; ++i)
			{
				char ch = readBuf[readPos++];
				if (ch != str.charAt (i))
					throw ioError ("expecting '" + ch + "'");
			}
			m_readPos = readPos;
			m_offset += len;
			m_column += len;
		}
	}

	private void readExp () throws IOException
	{
		char ch = read ();
		if (ch >= '0' && ch <= '9')
		{
			append (ch);
		}
		else if (ch == '+' || ch == '-')
		{
			append (ch);
			ch = read ();
			if (ch >= '0' && ch <= '9')
			{
				append (ch);
			}
			else
			{
				throw unexpected (ch);
			}
		}
		else
		{
			throw unexpected (ch);
		}
		

		for (;;)
		{
			ch = read ();
			if (ch >= '0' && ch <= '9')
			{
				append (ch);
				continue;
			}
			unread ();		// unread the last char
			return;
		}
	}

	private void readFraction () throws IOException
	{
		m_int = false;
		char ch;
		char[] buf = m_appendBuf;
		while ((ch = read ()) >= '0' && ch <= '9')
		{
			buf[m_appendPos++] = ch;
		}
		if (ch == 'E' || ch == 'e')
		{
			buf[m_appendPos++] = ch;
			readExp ();
		}
		else
			unread ();		// unread the last char
	}

	private void readNumber (char firstChar) throws IOException
	{
		m_int = true;
		char[] buf = m_appendBuf;
		int appendPos = m_appendPos;
		buf[appendPos++] = firstChar;

		if (firstChar == '0')
		{
			// JSON requires 0 to be followed with . unless it is 0.
			char ch = read ();
			if (ch == '.')
			{
				buf[appendPos++] = ch;
				m_appendPos = appendPos;
				readFraction ();
			}
			else
			{
				m_appendPos = appendPos;
				unread ();		// unread the last char
			}
			return;
		}

		// now read the integer part.
		char ch;
		while ((ch = read ()) >= '0' && ch <= '9')
		{
			buf[appendPos++] = ch;
		}

		if (ch == '.')
		{
			buf[appendPos++] = ch;
			m_appendPos = appendPos;
			readFraction ();
		}
		else if (ch == 'E' || ch == 'e')
		{
			m_int = false;
			buf[appendPos++] = ch;
			m_appendPos = appendPos;
			readExp ();
		}
		else
		{
			m_appendPos = appendPos;
			unread ();		// unread the last char
		}
	}

	private void readEscape () throws IOException
	{
		char ch = read ();
		switch (ch)
		{
			case 'b':
				append ('\b');
				break;
			case 'f':
				append ('\f');
				break;
			case 'n':
				append ('\n');
				break;
			case 'r':
				append ('\r');
				break;
			case 't':
				append ('\t');
				break;
			case '\\':
			case '/':
			case '"':
				append (ch);
				break;
			case 'u':
			{
				int val = 0;
				for (int i = 0; i < 4; ++i)
				{
					ch = read ();
					if (ch >= '0')
					{
						if (ch <= '9')
						{
							int hex = ch - '0';
							val = (val << 4) | hex;
							continue;
						}
						else if (ch >= 'A')
						{
							if (ch <= 'F')
							{
								int hex = ch - 'A' + 10;
								val = (val << 4) | hex;
								continue;
							}
							else if (ch >= 'a')
							{
								if (ch <= 'f')
								{
									int hex = ch - 'a' + 10;
									val = (val << 4) | hex;
									continue;
								}
							}
						}
					}
					throw unexpected (ch);
				}
				append ((char) val);
				break;
			}
			default:
				throw ioError ("unknown escape sequence '\\" + ch + "'");
		}
	}

	private void readString () throws IOException
	{
		m_appendPos = 0;

		char[] buf = m_readBuf;
		for (;;)
		{
			int readPos = m_readPos;
			int readMax = m_readMax;
			while (readPos < readMax)
			{
				char ch = buf[readPos++];
				++m_offset;
				++m_column;
				// JSON does not allow 0x00 - 0x1f in string.
				// And '"' and '\\' must be escaped.
				if (ch > '\\')
				{
					append (ch);
				}
				else if (ch > '"')
				{
					if (ch < '\\')
					{
						append (ch);
					}
					else
					{
						// handle '\\' case
						m_readPos = readPos;
						readEscape ();
						readPos = m_readPos;
						readMax = m_readMax;
					}
				}
				else if (ch >= ' ')
				{
					if (ch < '"')
					{
						append (ch);
					}
					else
					{
						// handle '"' case
						m_readPos = readPos;
						return;
					}
				}
				else
				{
					throw unexpected (ch);
				}
			}
			fill ();
		}
	}

	private String getBufferString ()
	{
		return new String (m_appendBuf, 0, m_appendPos);
	}

	@Override
	public Event getEvent ()
	{
		return m_event;
	}

	@Override
	public JsonValue getValue ()
	{
		switch (m_event)
		{
			case START_ARRAY:
			case START_OBJECT:
				return Utils.getStructure (this);
			case END_ARRAY:
			case END_OBJECT:
			case KEY_NAME:
				throw stateError ("getValue()");
			case VALUE_TRUE:
				return JsonValue.TRUE;
			case VALUE_FALSE:
				return JsonValue.FALSE;
			case VALUE_NULL:
				return JsonValue.NULL;
			case VALUE_NUMBER:
				return new CookJsonBigDecimal (getBigDecimal ());
			case VALUE_STRING:
				return new CookJsonString (getString ());
		}
		throw stateError ("getValue()");
	}

	private void expectArrayObject () throws IOException
	{
		for (;;)
		{
			char ch = read ();
			// since we are only call at initiation.  We are mostly expecting
			// '[' and '{', not anything else.
			if (ch == '[')
			{
				pushState (true);
				m_event = Event.START_ARRAY;
				m_lastToken = START;
				return;
			}
			if (ch == '{')
			{
				pushState (false);
				m_event = Event.START_OBJECT;
				m_lastToken = START;
				return;
			}
			if (ch == ' ' || ch == '\t' || ch == '\r')
				continue;
			if (ch == '\n')
			{
				m_column = 1;
				++m_line;
				continue;
			}
			scanUnexpected (ch);
		}
	}

	private boolean expectCommaObject () throws IOException
	{
		for (;;)
		{
			char ch = read ();
			if (ch == ',')
				return false;
			if (ch == '}')
			{
				popState (false);
				m_event = Event.END_OBJECT;
				m_lastToken = VALUE;
				return true;
			}
			if (ch == ' ' || ch == '\t' || ch == '\r')
				continue;
			if (ch == '\n')
			{
				m_column = 1;
				++m_line;
				continue;
			}
			scanUnexpected (ch);
		}
	}

	private boolean expectCommaArray () throws IOException
	{
		for (;;)
		{
			char ch = read ();
			if (ch == ',')
				return false;
			if (ch == ']')
			{
				popState (true);
				m_event = Event.END_ARRAY;
				m_lastToken = VALUE;
				return true;
			}
			if (ch == ' ' || ch == '\t' || ch == '\r')
				continue;
			if (ch == '\n')
			{
				m_column = 1;
				++m_line;
				continue;
			}
			scanUnexpected (ch);
		}
	}

	private void expectColon () throws IOException
	{
		for (;;)
		{
			char ch = read ();
			if (ch == ':')
				return;
			if (ch == ' ' || ch == '\t' || ch == '\r')
				continue;
			if (ch == '\n')
			{
				m_column = 1;
				++m_line;
				continue;
			}
			scanUnexpected (ch);
		}
	}

	private void expectKeyName () throws IOException
	{
		for (;;)
		{
			char ch = read ();
			if (ch == '"')
			{
				saveLocation ();
				readString ();
				m_event = Event.KEY_NAME;
				m_lastToken = FIELD;
				m_event = Event.KEY_NAME;
				return;
			}
			if (ch == '}')
			{
				popState (false);
				m_event = Event.END_OBJECT;
				m_lastToken = VALUE;
				return;
			}
			if (ch == ' ' || ch == '\t' || ch == '\r')
				continue;
			if (ch == '\n')
			{
				m_column = 1;
				++m_line;
				continue;
			}
			scanUnexpected (ch);
		}
	}

	private void expectValue () throws IOException
	{
		for (;;)
		{
			char ch = read ();
			switch (ch)
			{
				case '\t':
				case '\r':
					break;
				case '\n':
					m_column = 1;
					++m_line;
					break;
				case ' ':
					break;
				case '"':
				{
					saveLocation ();
					readString ();
					m_lastToken = VALUE;
					m_event = Event.VALUE_STRING;
					return;
				}
				case '-':
				{
					m_appendPos = 0;
					append (ch);
					ch = read ();
					if (!(ch >= '0' && ch <= '9'))
					{
						throw unexpected (ch);
					}

					readNumber (ch);
					m_event = Event.VALUE_NUMBER;
					m_lastToken = VALUE;
					return;
				}
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				{
					m_appendPos = 0;
					readNumber (ch);
					m_event = Event.VALUE_NUMBER;
					m_lastToken = VALUE;
					return;
				}
				case '[':
				{
					pushState (true);
					m_event = Event.START_ARRAY;
					m_lastToken = START;
					return;
				}
				case ']':
				{
					popState (true);
					m_event = Event.END_ARRAY;
					m_lastToken = VALUE;
					return;
				}
				case 'f':
				{
					expect ("alse");
					m_event = Event.VALUE_FALSE;
					m_lastToken = VALUE;
					return;
				}
				case 'n':
				{
					expect ("ull");
					m_event = Event.VALUE_NULL;
					m_lastToken = VALUE;
					return;
				}
				case 't':
				{
					expect ("rue");
					m_event = Event.VALUE_TRUE;
					m_lastToken = VALUE;
					return;
				}
				case '{':
				{
					pushState (false);
					m_event = Event.START_OBJECT;
					m_lastToken = START;
					return;
				}
				default:
				{
					scanUnexpected (ch);
				}
			}
		}
	}

	@Override
	public Event next ()
	{
		try
		{
			int state = m_state;
			if (state == ParserState.IN_OBJECT)
			{
				int lastToken = m_lastToken;
				if (lastToken == FIELD)
				{
					expectColon ();
					expectValue ();
					return m_event;
				}

				if (lastToken == VALUE)
				{
					// we now expect either ',' or '}'
					if (expectCommaObject ())
					{
						return m_event;
					}
				}

				expectKeyName ();
				return m_event;
			}
			else if (state == ParserState.IN_ARRAY)
			{
				if (m_lastToken == VALUE)
				{
					// we now expect either ',' or ']'
					if (expectCommaArray ())
					{
						return m_event;
					}
				}
				expectValue ();
				return m_event;
			}
			else if (state == ParserState.INITIAL)
			{
				expectArrayObject ();
				return m_event;
			}
			else if (state == ParserState.END)
				throw new NoSuchElementException ();
			throw new IllegalStateException ();
		}
		catch (IOException ex)
		{
			throw new JsonParsingException (ex.getMessage (), ex, getCurrentLocation ());
		}
	}

	@Override
	public boolean hasNext ()
	{
		return m_state != ParserState.END;
	}

	@Override
	public boolean isIntegralNumber ()
	{
		if (m_event != Event.VALUE_NUMBER)
			throw stateError ("isIntegralNumber()");
		return m_int || getBigDecimal ().scale () == 0;
	}

	@Override
	public int getInt ()
	{
		if (m_event != Event.VALUE_NUMBER)
			throw stateError ("getInt()");
		String str = getBufferString ();
		if (m_int &&
			(m_appendPos < 10 ||
			 (m_appendPos < 11 && m_appendBuf[0] == '-')))
		{
			return Integer.valueOf (str);
		}
		return new BigDecimal (str).intValue ();
	}

	@Override
	public long getLong ()
	{
		if (m_event != Event.VALUE_NUMBER)
			throw stateError ("getLong()");
		String str = getBufferString ();
		if (m_int &&
			(m_appendPos < 19 ||
			 (m_appendPos < 20 && m_appendBuf[0] == '-')))
		{
			return Long.valueOf (str);
		}
		return new BigDecimal (str).longValue ();
	}

	@Override
	public BigDecimal getBigDecimal ()
	{
		if (m_event != Event.VALUE_NUMBER)
			throw stateError ("getBigDecimal()");
		return new BigDecimal (getBufferString ());
	}

	private JsonLocation getCurrentLocation ()
	{
		JsonLocationImpl location = new JsonLocationImpl ();
		location.m_columnNumber = m_column;
		location.m_streamOffset = m_offset;
		location.m_lineNumber = m_line;
		return location;
	}

	@Override
	public JsonLocation getLocation ()
	{
		JsonLocationImpl location = new JsonLocationImpl ();
		long diff = 0;
		switch (m_event)
		{
			case START_OBJECT:
			case START_ARRAY:
			case END_OBJECT:
			case END_ARRAY:
				diff = 1;
				break;
			case VALUE_NUMBER:
				diff = m_appendPos;
				break;
			case KEY_NAME:
			case VALUE_STRING:
			{
				location.m_columnNumber = savedColumn - 1;	// minus 2 to account for '"'
				location.m_streamOffset = savedOffset - 1;	// minus 2 to account for '"'
				location.m_lineNumber = savedLine;
				return location;
			}
			case VALUE_FALSE:
				diff = 5;
				break;
			case VALUE_NULL:
			case VALUE_TRUE:
				diff = 4;
				break;
		}
		location.m_columnNumber = m_column - diff;
		location.m_streamOffset = m_offset - diff;
		location.m_lineNumber = m_line;
		return location;
	}

	@Override
	public String getString ()
	{
		if (m_event != Event.VALUE_STRING &&
			m_event != Event.VALUE_NUMBER &&
			m_event != Event.KEY_NAME)
			throw stateError ("getString()");
		return getBufferString ();
	}

	@Override
	public boolean isBinary ()
	{
		if (m_event != Event.VALUE_STRING)
			throw stateError ("isBinary()");
		// For Json, it is up to the caller to interpret the string
		// value as the binary encoded string.
		return false;
	}

	@Override
	public byte[] getBytes ()
	{
		if (m_event != Event.VALUE_STRING)
			throw stateError ("getBytes()");
		throw new IllegalStateException ("The current string value is not binary.");
	}

	@Override
	public void close ()
	{
		try
		{
			m_reader.close ();
		}
		catch (IOException ex)
		{
			throw new JsonParsingException (ex.getMessage (), ex, getCurrentLocation ());
		}
	}

	private void pushState (boolean isArray)
	{
		if (m_state != ParserState.INITIAL)
			m_states.add (Boolean.valueOf (m_state == ParserState.IN_ARRAY));
		m_state = isArray ? ParserState.IN_ARRAY : ParserState.IN_OBJECT;
	}

	private void popState (boolean isArrayEnd)
	{
		boolean isArray = m_state == ParserState.IN_ARRAY;
		if (isArrayEnd != isArray)
			throw new IllegalStateException ();
		if (m_states.isEmpty ())
			m_state = ParserState.END;
		else
		{
			boolean b = m_states.remove (m_states.size () - 1);
			m_state = b ? ParserState.IN_ARRAY : ParserState.IN_OBJECT;
		}
	}

	/**
	 * @return	the allowComments
	 */
	public boolean isAllowComments ()
	{
		return m_allowComments;
	}

	/**
	 * @param	allowComments
	 *			the allowComments to set
	 */
	public void setAllowComments (boolean allowComments)
	{
		m_allowComments = allowComments;
	}
}
