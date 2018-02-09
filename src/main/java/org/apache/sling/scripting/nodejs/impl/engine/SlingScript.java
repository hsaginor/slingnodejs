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
import org.apache.sling.scripting.nodejs.impl.objects.V8ObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

public class SlingScript implements Releasable {

	private static final Logger log = LoggerFactory.getLogger( SlingScript.class );
			
	private ScriptContext context;
	private ScriptLoader loader;
	private List<Releasable> scriptableObjects = new ArrayList<Releasable>(); 
	
	private Bindings bindings;
    private SlingScriptHelper scriptHelper;
    private SlingHttpServletResponse response;
    private Resource scriptResource;
    
	private ScriptException exception;
	private boolean executed;
	
	private File scriptFile;
	private String output;
	
	/**
	 * Constructor called by Sling ScriptEngine to process a request.
	 * 
	 * @param context
	 * @param loader
	 * @throws ScriptException 
	 */
	public SlingScript(ScriptContext context, ScriptLoader loader) throws ScriptException {
		this.context = context;
		this.loader = loader;
		init();
	}
	
	private void init() throws ScriptException {
		bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        scriptHelper = (SlingScriptHelper) bindings.get("sling");
        response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
        scriptResource = scriptHelper.getScript().getScriptResource();
        // scriptFile = loader.loadScriptFile(scriptResource);
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
        		v8script = (V8Object) nodeJS.require(scriptFile);
			String output = v8script.executeStringFunction("render", null);
			response.getWriter().write(output);
			response.getWriter().flush();
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
		Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingScriptHelper scriptHelper = (SlingScriptHelper) bindings.get("sling");
        SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
        SlingHttpServletRequest request = scriptHelper.getRequest();
		Resource resource = request.getResource();
		ResourceResolver resolver = resource.getResourceResolver();
		Session jcrSession = resolver.adaptTo( Session.class );
		
		addObject(v8, SlingBindings.SLING, scriptHelper);
		addObject(v8, SlingBindings.REQUEST, request);
		addObject(v8, SlingBindings.RESPONSE, response);
		addObject(v8, SlingBindings.RESOURCE, resource);
		addObject(v8, "resolver", resolver);
		addObject(v8, "jcrSession", jcrSession);
		addObject(v8, "node", resource.adaptTo(Node.class));
		addObject(v8, "properties", resource.adaptTo(ValueMap.class));
	}

	private void addObject(V8 v8, String name, Object object) {
		V8ObjectWrapper wrapper = new V8ObjectWrapper(v8, object, name);
		scriptableObjects.add(wrapper);
	}
}
