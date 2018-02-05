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
package org.apache.sling.scripting.nodejs.impl.webconsole;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.scripting.nodejs.impl.engine.V8ScriptEngineFactory;
import org.apache.sling.scripting.nodejs.impl.threadpool.NodeJSTask;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Object;

@SuppressWarnings("serial")
@Component(name = "NodeJS Files Plugin", immediate = true, service = Servlet.class, 
		property = {
				WebConsoleConstants.PLUGIN_LABEL + "=" + NodeJSFilesPlugin.PLUGIN_LABEL,
				WebConsoleConstants.PLUGIN_TITLE + "=" + NodeJSFilesPlugin.PLUGIN_TITLE,
				WebConsoleConstants.PLUGIN_CATEGORY + "=" + NodeJSFilesPlugin.PLUGIN_CATEGORY
		})
public class NodeJSFilesPlugin extends AbstractWebConsolePlugin {

	/**
	 * 
	 */
	private static final long serialVersionUID = -372063988540618625L;

	private static final Logger log = LoggerFactory.getLogger( NodeJSFilesPlugin.class );
	
	public static final String PLUGIN_LABEL = "nodejsfiles";
	public static final String PLUGIN_TITLE = "NodeJS Scripting Files";
	public static final String PLUGIN_CATEGORY = "Sling NodeJS";

	@Reference(service=ScriptEngineFactory.class, target="(component.name=NodeJS Scripting Engine Factory)")
	private ScriptEngineFactory scriptFactory;
	
	@Override
	public String getLabel() {
		return PLUGIN_LABEL;
	}

	@Override
	public String getTitle() {
		return PLUGIN_TITLE;
	}
	
	@Override
	public String getCategory() {
		return PLUGIN_CATEGORY;
	}

	@Override
	protected void renderContent(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		V8ScriptEngineFactory v8EngineFactory = (V8ScriptEngineFactory) scriptFactory;
		File scriptsFolder = v8EngineFactory.getScriptLoader().getScriptsFolder();
		
		String scriptsPath = scriptsFolder.getAbsolutePath();
		PrintWriter writer = response.getWriter();
		
		writer.println("NodeJS Scripts Path: " + scriptsPath + "<br/>");
		
	}

}
