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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.request.SlingRequestEvent;
import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.nodejs.impl.engine.NodeBuilder;
import org.apache.sling.scripting.nodejs.impl.engine.V8ScriptEngineFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ScriptCollector implements SlingRequestListener {

	private static final Logger log = LoggerFactory.getLogger( ScriptCollector.class );
	
	private static final String ATTR_NAME = ScriptCollector.class.getName() + ".SCRIPTS"; 

	// private final ThreadLocal<List<String>> scripts = new ThreadLocal<List<String>>();
	
	private NodeBuilder builder = null;
	private File buildDir = null;
	
	public void setNodeBuilder(NodeBuilder builder, File buildDir) {
		this.builder = builder;
		this.buildDir = buildDir;
	}
	
	@Override
	public void onEvent(SlingRequestEvent event) {
		HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
		
		if ( event.getType() == SlingRequestEvent.EventType.EVENT_INIT ) {
			// log.debug("SlingRequestEvent.EventType.EVENT_INIT {}");
			// scripts.set(new ArrayList<String>());
		} else if ( event.getType() == SlingRequestEvent.EventType.EVENT_DESTROY ) {
			// log.debug("SlingRequestEvent.EventType.EVENT_DESTROY {}");
			log(request);
			
			try {
				if(hasScripts(request)) {
					List<String> scripts = getPerRequestScripts(request);
					long start = System.currentTimeMillis();
					String hash = sha1(event.getServletRequest());
					long time = System.currentTimeMillis() - start;
					String req = request.getServletPath();
					log.debug("{} Computed collected files hash {} in {} milliseconds.", new Object[] {req, hash, time});
					
					String bundleFilePath = getBundleFilePath(request.getServletPath(),  hash);
					log.debug("Computed bundle file path {}", bundleFilePath);
					
					if(builder != null && buildDir != null) 
						builder.browserfy(buildDir, scripts, bundleFilePath);
				}
			} catch (Exception e) {
				log.error("Unable to compute hash.");
			} 
			// scripts.remove();
		}
	}
	
	public void add(ServletRequest request, String scriptPath) {
		// log.debug("adding {}", scriptPath);
		// List<String> list = scripts.get();
		getPerRequestScripts(request).add(scriptPath);
		
		/*
		if(list != null) {
			// scripts.get().add(scriptPath);
		} else {
			log.error("Per thread script collection is null.");
		}
		*/
	}
	
	public Iterator<String> getCollectedScriptsIterator(ServletRequest request) {
		List<String> scripts = getPerRequestScripts(request);
		return scripts.iterator();
	}
	
	public boolean hasScripts(ServletRequest request) {
		return getPerRequestScripts(request).size() > 0;
	}
	
	public File findCompiledBundle(Resource resource) {
		File file = null;
		
		String dirPath = buildDir.getAbsolutePath() + File.separatorChar + V8ScriptEngineFactory.SCRIPTS_OUT_DIR + resource.getParent().getPath();
		File dir = new File(dirPath);
		if(dir.exists() && dir.isDirectory()) {
			String name = resource.getName() + NodeBuilder.BUNDLE_FILE_SELECTOR;
			File foundFiles[] = dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					// TODO Auto-generated method stub
					return pathname.getName().startsWith(name);
				}
			});
			
			if(foundFiles.length > 0) {
				File bundleFile = foundFiles[0];
				if(bundleFile.exists() && bundleFile.isFile()) {
					file = bundleFile;
				}
			}
		} else {
			log.warn("Directory {} doesn't exist.", dirPath);
		}
		
		return file;
	}
	
	private void log(ServletRequest request) {
		if(log.isDebugEnabled()) {
			List<String> scripts = getPerRequestScripts(request);
			int count = 0;
			StringBuilder sb = new StringBuilder("[");
			for(String path : scripts) {
				if(count>0)
					sb.append(", ");
				sb.append(path);
				count++;
			}
			sb.append(']');
			if(count > 0)
				log.debug("Collected {} scripts: {}", new Object[] {count, sb.toString()});
		}
	}
	
	private String getBundleFilePath(String mainScript, String hash) {
		String scriptName = mainScript.substring(0, mainScript.indexOf('.'));
		return buildDir.getAbsolutePath() + File.separatorChar + V8ScriptEngineFactory.SCRIPTS_OUT_DIR + scriptName + NodeBuilder.BUNDLE_FILE_SELECTOR + hash + ".js";
	}
	
	private List<String> getPerRequestScripts(ServletRequest request) {
		List<String> scripts = (List<String>) request.getAttribute(ATTR_NAME);
		if(scripts == null) {
			scripts = new ArrayList<String>();
			request.setAttribute(ATTR_NAME, scripts);
		}
		return scripts;
	}
	
	private String sha1(ServletRequest request) throws NoSuchAlgorithmException, IOException {
		final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

		Iterator<String> it = getCollectedScriptsIterator(request);
		while (it.hasNext()) {
			String path = it.next();
			File file = new File(path);

			if (file.exists() && file.isFile()) {
				try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
					final byte[] buffer = new byte[1024];
					for (int read = 0; (read = is.read(buffer)) != -1;) {
						messageDigest.update(buffer, 0, read);
					}
				}
			}
		}
		
		// Convert the byte to hex format
		try (Formatter formatter = new Formatter()) {
			for (final byte b : messageDigest.digest()) {
				formatter.format("%02x", b);
			}
			return formatter.toString();
		}

	}

}
