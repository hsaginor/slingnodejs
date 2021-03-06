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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.nodejs.impl.clientside.ScriptCollector;
import org.apache.sling.scripting.nodejs.impl.objects.ResourceIncluder;
import org.apache.sling.scripting.nodejs.impl.objects.SlingLogger;
import org.apache.sling.scripting.nodejs.impl.objects.V8ObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

public class SlingScript implements Releasable {
	private static final Logger log = LoggerFactory.getLogger( SlingScript.class );
	
	/**
	 * Name of a default method that will be called on JavaScript script object to render content on the server.<br/>
	 * This may become a configuration option in the future.
	 */
	public static final String DEFAULT_SERVER_METHOD = "renderServerResponse";
	
	private ScriptContext context;
	private ScriptLoader loader;
	// private ScriptCollector scriptCollector;
	private List<Releasable> scriptableObjects = new ArrayList<Releasable>(); 
	
	private Bindings bindings;
    private SlingScriptHelper scriptHelper;
    private SlingHttpServletResponse response;
    private Resource scriptResource;
    
	private ScriptException exception;
	private boolean executed;
	
	private File scriptFile;
	private String output;
	
	private ClassLoader dynamicClassLoader;
	
	/**
	 * Constructor called by Sling ScriptEngine to process a request.
	 * 
	 * @param context
	 * @param loader
	 * @throws ScriptException 
	 */
	public SlingScript(ClassLoader dynamicClassLoader, ScriptContext context, ScriptLoader loader /*, ScriptCollector scriptCollector */) throws ScriptException {
		this.context = context;
		this.loader = loader;
		this.dynamicClassLoader = dynamicClassLoader;
		// this.scriptCollector = scriptCollector;
		init();
	}
	
	private void init() throws ScriptException {
		bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        scriptHelper = (SlingScriptHelper) bindings.get("sling");
        response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
        scriptResource = scriptHelper.getScript().getScriptResource();
	}
	
	public File getScriptFile() {
		return scriptFile;
	}

	/**
	 * This constructor can be used to test script execution outside of Sling.
	 * 
	 * @param scriptFile
	 */
	public SlingScript(File scriptFile) {
		if(scriptFile==null || !scriptFile.exists() || !scriptFile.isFile()) {
			throw new IllegalArgumentException();
		}
		
		this.scriptFile = scriptFile;
	}
	
	public void eval(NodeJS nodeJS) {
		if(context != null) {
			evalScriptContext(nodeJS);
		} else {
			evalScriptFile(nodeJS);
		}
	}
	
	/**
	 * Returns script output after execution completes. In context of Sling request output will also be written to response when eval method is called.
	 * 
	 * @return
	 */
	public String getOutput() {
		return output;
	}
	
	// TODO:
	// This method exists only to support executing scripts directly from a file during unit testing.
	// Need to rewrite unit tests to run within Sling context using Sling Testing frameworks and remove this.
	private void evalScriptFile(NodeJS nodeJS) {
		V8Object v8script = null;
		
        try {
        		v8script = (V8Object) nodeJS.require(scriptFile);
			output = v8script.executeStringFunction("render", null);
        } catch(Exception e) {
        		log.error("Unable to execure script.", e);
        		exception = new ScriptException(e);
        } finally {
        		if(v8script != null && !v8script.isReleased()) {
        			v8script.release();
        		}
        		executed = true;
        }
	}
	
	private void evalScriptContext(NodeJS nodeJS) {
		V8Object v8script = null;
		loader.lockToRead(scriptResource);
		
        initObjects(nodeJS.getRuntime());
        try {
        		log.debug("eval script {} for resource {}", new Object[] {scriptResource.getPath(), scriptHelper.getRequest().getResource().getPath()} );
        		File scriptFile = loader.loadScriptFile(scriptResource);
        		v8script = require(nodeJS, scriptFile);
        		if(v8script != null) {
        			Object output = v8script.executeFunction(getServerMethodName(), null);
        			// log.debug("Script return object of type {}", output.getClass()); 
        			if(output instanceof String) {
        				response.getWriter().write(output.toString());
        				response.getWriter().flush();
        			}
        			
        			this.scriptFile = scriptFile;
        			// scriptCollector.add(scriptFile.getAbsolutePath());
        		} else {
        			exception = new ScriptException("Method " + getServerMethodName() + " is not defined in the script " + scriptResource.getPath());
        		}
        } catch(Exception e) {
        		log.error("Unable to execure script.", e);
        		exception = new ScriptException(e);
        } finally {
        		if(v8script != null && !v8script.isReleased()) {
        			v8script.release();
        		}
        		executed = true;
        		loader.unlockToRead(scriptResource);
        }
	}
	
	private V8Object require(NodeJS nodeJS, File scriptFile) {
		V8Object script = (V8Object) nodeJS.require(scriptFile);
		
		String methodName = getServerMethodName();
		if(script.contains(methodName)) {
			log.debug("Found server script in {}.", new Object[] {scriptResource.getPath()});
			return script;
		} else {
			for(String key : script.getKeys()) {
				Object next = script.get(key);
				if(next instanceof V8Object) {
					V8Object nextV8Obj = (V8Object) next;
					if(nextV8Obj.contains(methodName)) {
						log.debug("Found server script in {}. Exported as {}.", new Object[] {scriptResource.getPath(), key});
						return nextV8Obj;
					}
				}
				
				if(next instanceof Releasable) {
					((Releasable) next).release();
				}
			}
		}
		
		return null;
	}
	
	public String getServerMethodName() {
		return DEFAULT_SERVER_METHOD;
	}
	
	public boolean isExecuted() {
		return executed;
	}
	
	public boolean hasException() {
		return exception != null;
	}
	
	public ScriptException getException() {
		return exception;
	}
	
	@Override
	public void release() {
		for(Releasable r : scriptableObjects) {
			r.release();
		}
	}

	private void initObjects(V8 v8) {
		log.debug("Initializing scripting objects for {}", scriptResource.getPath());
		Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingScriptHelper scriptHelper = (SlingScriptHelper) bindings.get("sling");
        SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
        SlingHttpServletRequest request = scriptHelper.getRequest();
		Resource resource = request.getResource();
		ResourceResolver resolver = resource.getResourceResolver();
		Session jcrSession = resolver.adaptTo( Session.class );
		SlingLogger logger = new SlingLogger();
		
		addObject(v8, SlingLogger.LOGGER_JS_NAME, logger);
		addObject(v8, SlingBindings.SLING, scriptHelper);
		addObject(v8, SlingBindings.REQUEST, request);
		addObject(v8, SlingBindings.RESPONSE, response);
		addObject(v8, SlingBindings.RESOURCE, resource);
		try {
			addObject(v8, SlingBindings.OUT, response.getWriter());
		} catch (IOException e) {
			log.error("Unable to create scripting variable " + SlingBindings.OUT, e);
		}
		addObject(v8, "resolver", resolver);
		addObject(v8, "jcrSession", jcrSession);
		addObject(v8, "node", resource.adaptTo(Node.class));
		addObject(v8, "properties", resource.adaptTo(ValueMap.class));
		
		ResourceIncluder resourceIncluder = new ResourceIncluder(request, response, v8);
		v8.registerJavaMethod(resourceIncluder, "slingInclude");
		log.debug("Finished initializing scripting objects for {}", scriptResource.getPath());
	}

	private void addObject(V8 v8, String name, Object object) {
		V8ObjectWrapper wrapper = new V8ObjectWrapper(dynamicClassLoader, v8, object, name);
		scriptableObjects.add(wrapper);
	}
	
	private void inspect(V8Object obj) {
		log.debug("Script return V8Object:");
		for(String k : obj.getKeys()) {
			// int type = obj.getType(k);
			log.debug("		key: {}", new Object[]{k});
			try {
				log.debug("			type: {}", new Object[]{obj.getType(k)});
				if(V8Value.STRING == obj.getType(k)) {
					log.debug("			value: {}", new Object[]{obj.getString(k)});
				}
			} catch(Exception e) {}
		}
	}
}
