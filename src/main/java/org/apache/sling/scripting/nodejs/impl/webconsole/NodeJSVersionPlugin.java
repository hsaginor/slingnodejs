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
@Component(name = "NodeJS Version Plugin", immediate = true, service = Servlet.class, 
		property = {
				WebConsoleConstants.PLUGIN_LABEL + "=" + NodeJSVersionPlugin.PLUGIN_LABEL,
				WebConsoleConstants.PLUGIN_TITLE + "=" + NodeJSVersionPlugin.PLUGIN_TITLE,
				WebConsoleConstants.PLUGIN_CATEGORY + "=" + NodeJSVersionPlugin.PLUGIN_CATEGORY
		})
public class NodeJSVersionPlugin extends AbstractWebConsolePlugin {

	/**
	 * 
	 */
	private static final long serialVersionUID = -372063988540618625L;

	private static final Logger log = LoggerFactory.getLogger( NodeJSVersionPlugin.class );
	
	public static final String PLUGIN_LABEL = "nodejsversion";
	public static final String PLUGIN_TITLE = "NodeJS Scripting Version";
	public static final String PLUGIN_CATEGORY = "Sling";

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
		NodeJSTask<Object> task = new NodeJSTask<Object>() {

			private NodeJS nodeJS;
			
			@Override
			public Object call() throws Exception {
				if(nodeJS != null) {
					getNodeVersion(nodeJS, response.getWriter());
				}
				return null;
			}

			@Override
			public void setNode(NodeJS node) {
				nodeJS = node;
			}
			
		};
		
		try {
			if(scriptFactory!=null) {
				((V8ScriptEngineFactory) scriptFactory).getScriptExecutionPool().exec(task);
			}
		} catch (ScriptException e) {
			log.error("Unable to process request.", e);
			throw new ServletException(e);
		}
		
	}
	
	public void getNodeVersion(NodeJS node, PrintWriter writer) {
        V8Object process = null;
        V8Object versions = null;
        try {
            process = node.getRuntime().getObject("process");
            versions = process.getObject("versions");
            
            for(String key : versions.getKeys()) {
            		writer.println(key + " = " + versions.getString(key) + "<br/>");
            }
            // nodeVersion = versions.getString("node");
        } finally {
            safeRelease(process);
            safeRelease(versions);
        }
    }
	
	private void safeRelease(final Releasable releasable) {
        if (releasable != null) {
            releasable.release();
        }
    }

}
