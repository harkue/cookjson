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

import java.math.BigDecimal;
import java.util.Stack;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.yuanheng.cookjson.value.*;

/**
 * @author Heng Yuan
 */
class Utils
{
	public static int THRESHOLD = 10000;

	public static void setInt (byte[] bytes, int value)
	{
		bytes[0] = (byte) (value & 0xff);
		bytes[1] = (byte) ((value >> 8) & 0xff);
		bytes[2] = (byte) ((value >> 16) & 0xff);
		bytes[3] = (byte) ((value >> 24) & 0xff);
	}

	public static void setLong (byte[] bytes, long value)
	{
		bytes[0] = (byte) (value & 0xff);
		bytes[1] = (byte) ((value >> 8) & 0xff);
		bytes[2] = (byte) ((value >> 16) & 0xff);
		bytes[3] = (byte) ((value >> 24) & 0xff);
		bytes[4] = (byte) ((value >> 32) & 0xff);
		bytes[5] = (byte) ((value >> 40) & 0xff);
		bytes[6] = (byte) ((value >> 48) & 0xff);
		bytes[7] = (byte) ((value >> 56) & 0xff);
	}

	private static void addStructure (CookJsonParser p, JsonStructure struct)
	{
		Stack<JsonStructure> structStack = new Stack<JsonStructure> ();
		Stack<String> nameStack = new Stack<String> ();

		structStack.push (struct);
		while (p.hasNext ())
		{
			Event e = p.next ();
			JsonValue value = null;
			switch (e)
			{
				case START_ARRAY:
					structStack.push (new CookJsonArray ());
					continue;
				case START_OBJECT:
					continue;
				case KEY_NAME:
					nameStack.push (p.getString ());
					break;
				case END_ARRAY:
				{
					value = structStack.pop ();
					if (!(value instanceof JsonArray))
						throw new IllegalStateException ();
					if (structStack.isEmpty ())
						return;	// done
					break;
				}
				case END_OBJECT:
				{
					value = structStack.pop ();
					if (!(value instanceof JsonObject))
						throw new IllegalStateException ();
					if (structStack.isEmpty ())
						return;	// done
					break;
				}
				case VALUE_TRUE:
				case VALUE_FALSE:
				case VALUE_NULL:
				case VALUE_NUMBER:
				case VALUE_STRING:
				{
					value = p.getValue ();
					break;
				}
			}
			struct = structStack.peek ();
			if (struct instanceof JsonArray)
			{
				((JsonArray)struct).add (value);
			}
			else
			{
				String name = nameStack.pop ();
				((JsonObject)struct).put (name, value);
			}
		}
	}

	public static JsonValue getValue (CookJsonParser p)
	{
		Event e = p.getEvent ();
		switch (e)
		{
			case START_ARRAY:
			{
				CookJsonArray v = new CookJsonArray ();
				addStructure (p, v);
				return v;
			}
			case START_OBJECT:
			{
				CookJsonObject v = new CookJsonObject ();
				addStructure (p, v);
				return v;
			}
			case KEY_NAME:
			case END_ARRAY:
			case END_OBJECT:
				throw new IllegalStateException ();
			case VALUE_TRUE:
				return CookJsonBoolean.TRUE;
			case VALUE_FALSE:
				return CookJsonBoolean.FALSE;
			case VALUE_NULL:
				return CookJsonNull.NULL;
			case VALUE_NUMBER:
			case VALUE_STRING:
				return p.getValue ();
			default:
				throw new IllegalStateException ();
		}
	}

	public static void convert (JsonParser p, JsonGenerator g)
	{
		String name = null;
		while (p.hasNext ())
		{
			Event e = p.next ();
			switch (e)
			{
				case START_ARRAY:
					assert Debug.debug ("READ: " + e);
					if (name == null)
						g.writeStartArray ();
					else
					{
						g.writeStartArray (name);
						name = null;
					}
					break;
				case START_OBJECT:
					assert Debug.debug ("READ: " + e);
					if (name == null)
						g.writeStartObject ();
					else
					{
						g.writeStartObject (name);
						name = null;
					}
					break;
				case KEY_NAME:
					assert Debug.debug ("READ: " + e + " = " + p.getString ());
					name = p.getString ();
					break;
				case END_ARRAY:
				case END_OBJECT:
					assert Debug.debug ("READ: " + e);
					g.writeEnd ();
					name = null;
					break;
				case VALUE_TRUE:
				{
					assert Debug.debug ("READ: " + e);
					if (name == null)
					{
						g.write (true);
					}
					else
					{
						g.write (name, true);
						name = null;
					}
					break;
				}
				case VALUE_FALSE:
				{
					assert Debug.debug ("READ: " + e);
					if (name == null)
					{
						g.write (false);
					}
					else
					{
						g.write (name, false);
						name = null;
					}
					break;
				}
				case VALUE_NULL:
				{
					assert Debug.debug ("READ: " + e);
					if (name == null)
					{
						g.writeNull ();
					}
					else
					{
						g.writeNull (name);
						name = null;
					}
					break;
				}
				case VALUE_NUMBER:
				{
					BigDecimal value = p.getBigDecimal ();
					assert Debug.debug ("READ: " + e + " = " + value);
					if (p.isIntegralNumber ())
					{
						try
						{
							if (name == null)
							{
								g.write (value.intValueExact ());
							}
							else
							{
								g.write (name, value.intValueExact ());
								name = null;
							}
						}
						catch (ArithmeticException ex)
						{
							try
							{
								if (name == null)
								{
									g.write (value.longValueExact ());
								}
								else
								{
									g.write (name, value.longValueExact ());
									name = null;
								}
							}
							catch (ArithmeticException ex2)
							{
								if (name == null)
								{
									g.write (value.toBigInteger ());
								}
								else
								{
									g.write (name, value.toBigInteger ());
									name = null;
								}
							}
						}
					}
					else
					{
						if (name == null)
						{
							g.write (value);
						}
						else
						{
							g.write (name, value);
							name = null;
						}
					}
					break;
				}
				case VALUE_STRING:
				{
					assert Debug.debug ("READ: " + e + " = " + p.getString ());
					if (p instanceof BasicBsonParser &&
						g instanceof FastBsonGenerator)
					{
						JsonValue v = ((BasicBsonParser) p).getValue ();
						if (v instanceof CookJsonBinary)
						{
							byte[] bytes = ((CookJsonBinary) v).getBytes ();
							if (name == null)
								((FastBsonGenerator)g).write (bytes);
							else
								((FastBsonGenerator)g).write (name, bytes);
							break;
						}
					}
					if (name == null)
					{
						g.write (p.getString ());
					}
					else
					{
						g.write (name, p.getString ());
						name = null;
					}
				}
				default:
					break;
			}
		}
	}

	private static void assertState (boolean b)
	{
		if (!b)
			throw new IllegalStateException ();
	}

	public static void validateGeneratorAction (int currentState, int action)
	{
		switch (action)
		{
			case GeneratorAction.END_ARRAY_OBJECT:
				assertState (currentState == GeneratorState.IN_ARRAY ||
							 currentState == GeneratorState.IN_OBJECT);
				break;
			case GeneratorAction.WRITE_ARRAY_VALUE:
				assertState (currentState == GeneratorState.INITIAL ||
							 currentState == GeneratorState.IN_ARRAY);
				break;
			case GeneratorAction.WRITE_OBJECT_VALUE:
				assertState (currentState == GeneratorState.IN_OBJECT);
				break;
			case GeneratorAction.CLOSE:
				assertState (currentState == GeneratorState.INITIAL ||
							 currentState == GeneratorState.END);
				break;
		}
	}
}
