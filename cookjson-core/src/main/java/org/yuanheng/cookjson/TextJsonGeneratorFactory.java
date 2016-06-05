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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

/**
 * Json based generator.
 *
 * @author	Heng Yuan
 */
public class TextJsonGeneratorFactory implements JsonGeneratorFactory
{
	private final Map<String, ?> m_config;

	public TextJsonGeneratorFactory (Map<String, ?> config)
	{
		m_config = config;
	}

	@Override
	public JsonGenerator createGenerator (Writer writer)
	{
		return TextJsonProvider.createGenerator (m_config, writer);
	}

	@Override
	public JsonGenerator createGenerator (OutputStream os)
	{
		return TextJsonProvider.createGenerator (m_config, os);
	}

	@Override
	public JsonGenerator createGenerator (OutputStream out, Charset charset)
	{
		return TextJsonProvider.createGenerator (m_config, new OutputStreamWriter (out, charset));
	}

	@Override
	public Map<String, ?> getConfigInUse ()
	{
		return Collections.unmodifiableMap (m_config);
	}
}
