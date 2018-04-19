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
package org.apache.sling.scripting.nodejs.impl.engine;

import java.io.File;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.nodejs.impl.threadpool.ScriptExecutionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V8ScriptEngine extends AbstractSlingScriptEngine {

	private static final Logger log = LoggerFactory.getLogger(V8ScriptEngine.class);
	
	private V8ScriptEngineFactory engineFactory;
	
	private ScriptExecutionPool threadPool;
	
	private ClassLoader dynamicClassLoader; 
	
	protected V8ScriptEngine(ClassLoader dynamicClassLoader, V8ScriptEngineFactory scriptEngineFactory, ScriptExecutionPool threadPool) {
		super(scriptEngineFactory);
		this.engineFactory = scriptEngineFactory;
		this.threadPool = threadPool;
		this.dynamicClassLoader = dynamicClassLoader;
		engineFactory.getScriptLoader().setChangeListener(threadPool);
	}
	
	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        SlingScript script = new SlingScript(dynamicClassLoader, context, engineFactory.getScriptLoader() /*, engineFactory.getScriptCollector() */);
        threadPool.exec(script);
		File scriptFile = script.getScriptFile();
		if(scriptFile != null) {
			Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
	        SlingScriptHelper scriptHelper = (SlingScriptHelper) bindings.get("sling");
			engineFactory.getScriptCollector().add(scriptHelper.getRequest(), scriptFile.getAbsolutePath());
		}
		return null;
	}
	
	void release() {
		log.debug("Stopping V8ScriptEngine");
		if(threadPool != null) {
			threadPool.shutdown();
		}
	}
	
}
