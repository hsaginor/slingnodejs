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

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptChangeObserver implements EventListener {

	private final Logger log = LoggerFactory.getLogger( ScriptChangeObserver.class );
	
	private ScriptLoader scriptLoader;
	private ResourceResolver resolver;
	private String[] scriptExtensions;
	private String[] nodeConfigFiles;
	
	public ScriptChangeObserver(ScriptLoader scriptLoader, ResourceResolver resolver, String[] scriptExtensions, String[] nodeConfigFiles) {
		if(scriptLoader == null || resolver == null || scriptExtensions == null)
			throw new NullPointerException();
		this.scriptLoader = scriptLoader;
		this.resolver = resolver;
		this.scriptExtensions = scriptExtensions;
		this.nodeConfigFiles = nodeConfigFiles;
	}
	
	@Override
	public void onEvent(EventIterator events) {
		String path = "";
		
		while(events.hasNext()) {
			final Event nextEvent = events.nextEvent();
			try {
				path = nextEvent.getPath();
				log.debug("Recieved jcr even of type {} for {}", new Object[] {nextEvent.getType(), path});
				
				if(nextEvent.getType() == Event.NODE_REMOVED) {
					log.debug("Processing delete event for {}", path);
					scriptLoader.deleteFiles(path);
				} else {
					if(path.endsWith("/jcr:content/jcr:data") || path.endsWith("/jcr:content")) {
						path = path.substring(0, path.indexOf("/jcr:content"));
					}
					
					if(isScript(path)) {
						log.debug("Processing change event for {}", path);
						
						Resource res = resolver.getResource(path);
						if(res != null) {
							scriptLoader.updateScript(res);
						}
					} else if(isConfig(path)) {
						log.debug("Processing change event for {}", path);
						
						Resource res = resolver.getResource(path);
						if(res != null) {
							scriptLoader.updateConfig(res);
						}
					}
				}
				
			} catch (Exception e) {
				log.error("Error processing change event. Path: " + path, e);
			}
		}

	}
	
	private boolean isScript(String path) {
		for(String extension : scriptExtensions) {
			if(path.endsWith(extension)) {
				return true;
			}
		}
	
		return false;
	}

	private boolean isConfig(String path) {
		for(String name : nodeConfigFiles) {
			if(path.endsWith("/"+name)) {
				return true;
			}
		}
	
		return false;
	}
}
