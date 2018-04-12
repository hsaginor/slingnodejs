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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Adaptable(	
		adaptableClass=Resource.class,
		adapters={
				@Adapter(ValueMap.class),
				@Adapter(InputStream.class)
		}
)
public class ClientSideJSResource extends AbstractResource {

	private static final Logger log = LoggerFactory.getLogger( ClientSideJSResource.class );
	
	public static final String RESOURCE_NAME = "jsbundle.js";
	public static final String RESOURCE_TYPE = "sling/nodejs/clientside/bundle";
	
	private String path;
	private ResourceResolver resolver;
	private Resource parent;
	private ResourceMetadata metadata;
	private ValueMap properties;
	private File file;
	
	ClientSideJSResource(ResourceResolver resolver, File file, Resource parent, String path) {
		this.path = path;
		this.resolver = resolver;
		this.parent = parent;
		this.file = file;
		
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("sling:resourceType", RESOURCE_TYPE);
		properties = new ValueMapDecorator(props);
		
		metadata = new ResourceMetadata();
		metadata.setResolutionPath(path);
		metadata.setContentType("application/javascript");
		metadata.setCharacterEncoding("UTF-8");
		
		try {
			metadata.setContentLength(getSize());
		} catch (IOException e) {
			log.warn("Unable to get file size for {}: {}", new Object[] {file.getAbsolutePath(), e.getMessage()});
		}
		
		try {
			metadata.setModificationTime(getModificationTime());
		} catch (IOException e) {
			log.warn("Unable to get file modification time for {}: {}", new Object[] {file.getAbsolutePath(), e.getMessage()});
		}
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public ResourceMetadata getResourceMetadata() {
		return metadata;
	}

	@Override
	public ResourceResolver getResourceResolver() {
		return resolver;
	}

	@Override
	public String getResourceSuperType() {
		return null;
	}

	@Override
	public String getResourceType() {
		return RESOURCE_TYPE;
	}

	@Override
	public String getName() {
		return RESOURCE_NAME;
	}

	@Override
	public Resource getParent() {
		return parent;
	}

	@Override
	public ValueMap getValueMap() {
		return properties;
	}

	@Override
	public String toString() {
		return path;
	}

	@Override
	public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
		if(type == InputStream.class) {
			InputStream stream;
			try {
				stream = new FileInputStream(file);
				return (AdapterType) stream;
			} catch (FileNotFoundException e) {
				log.error("Unable to open file stream for resource {}", new Object[] {this.getPath()}, e);
			}
		}
		return super.adaptTo(type);
	}
	
	private long getSize() throws IOException {
		Path path = FileSystems.getDefault().getPath(file.getAbsolutePath());
		return Files.size(path);
	}
	
	private long getModificationTime() throws IOException {
		Path path = FileSystems.getDefault().getPath(file.getAbsolutePath());
		BasicFileAttributes basicAttr = Files.readAttributes(path, BasicFileAttributes.class);
		return basicAttr.lastModifiedTime().toMillis();
	}

}
