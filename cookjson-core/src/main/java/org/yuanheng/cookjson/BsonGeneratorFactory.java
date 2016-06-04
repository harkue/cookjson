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

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import javax.json.JsonException;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

/**
 * Json based generator.
 *
 * @author	Heng Yuan
 */
class BsonGeneratorFactory implements JsonGeneratorFactory
{
	private final Map<String, ?> m_config;

	public BsonGeneratorFactory (Map<String, ?> config)
	{
		m_config = config;
	}

	@Override
	public JsonGenerator createGenerator (Writer writer)
	{
		throw new JsonException ("BSON output is binary.");
	}

	@Override
	public JsonGenerator createGenerator (OutputStream os)
	{
		return new FastBsonGenerator (os);
	}

	@Override
	public JsonGenerator createGenerator (OutputStream os, Charset charset)
	{
		return new FastBsonGenerator (os);
	}

	@Override
	public Map<String, ?> getConfigInUse ()
	{
		return Collections.unmodifiableMap (m_config);
	}
}