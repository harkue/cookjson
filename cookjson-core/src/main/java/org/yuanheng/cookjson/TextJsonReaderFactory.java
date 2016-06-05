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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

/**
 * @author	Heng Yuan
 */
public class TextJsonReaderFactory implements JsonReaderFactory
{
	private final Map<String, ?> m_config;

	public TextJsonReaderFactory (Map<String, ?> config)
	{
		m_config = config;
	}

	@Override
	public JsonReader createReader (Reader reader)
	{
		return new JsonReaderImpl (TextJsonProvider.createParser (m_config, reader));
	}

	@Override
	public JsonReader createReader (InputStream is)
	{
		return new JsonReaderImpl (TextJsonProvider.createParser (m_config, is));
	}

	@Override
	public JsonReader createReader (InputStream is, Charset charset)
	{
		return new JsonReaderImpl (TextJsonProvider.createParser (m_config, new InputStreamReader (is, charset)));
	}

	@Override
	public Map<String, ?> getConfigInUse ()
	{
		return Collections.unmodifiableMap (m_config);
	}
}
