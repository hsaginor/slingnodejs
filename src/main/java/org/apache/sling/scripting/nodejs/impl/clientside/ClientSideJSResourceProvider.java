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
package org.apache.sling.scripting.nodejs.impl.clientside;

import java.io.File;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		name = "NodeJS Client Side JS Resource Provider", 
		service = ResourceProvider.class,
		immediate = true,
		property = {
				ResourceProvider.OWNS_ROOTS + "=true", 
				ResourceProvider.ROOTS + "=" + ClientSideJSResourceProvider.ROOT
		}
)
public class ClientSideJSResourceProvider implements ResourceProvider {

	private static final Logger log = LoggerFactory.getLogger( ClientSideJSResourceProvider.class );
	
	public static final String ROOT = "/clientlib";
	
	@Reference( target="(component.name=org.apache.sling.scripting.nodejs.impl.clientside.ScriptCollector)")
    private SlingRequestListener scriptCollector;
	
	@Override
	public Resource getResource(ResourceResolver resolver, String path) {
		Resource resource = null;
		
		log.debug("Called to provide resource {} ", path);
		if(path.endsWith("/" + ClientSideJSResource.RESOURCE_NAME)) {
			String parentPath = path.substring(ROOT.length(), path.lastIndexOf('/'));
			log.debug("Content resource {}", parentPath);
			Resource parent = resolver.getResource(parentPath);
			if(parent != null) {
				File bundleFile = getScriptCollector().findCompiledBundle(parent);
				if(bundleFile != null) {
					resource = new ClientSideJSResource(resolver, bundleFile, parent, path);
				}
			}
		}
		
		return resource;
	}

	@Override
	public Resource getResource(ResourceResolver resolver, HttpServletRequest request, String path) {
		return getResource(resolver, path);
	}

	@Override
	public Iterator<Resource> listChildren(Resource resource) {
		// There are no children
		return null;
	}

	private ScriptCollector getScriptCollector() {
		return (ScriptCollector) scriptCollector;
	}
}
