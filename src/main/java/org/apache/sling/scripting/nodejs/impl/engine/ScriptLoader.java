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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.script.ScriptException;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptLoader {

	private static final Logger log = LoggerFactory.getLogger( ScriptLoader.class );
	
	/**
     * Locks for loading and writing script files
     */
    private final ConcurrentHashMap<String, ReadWriteLock> fileLocks = new ConcurrentHashMap<String, ReadWriteLock>();
    
	private File scriptsRootDir;
	private ScriptChangeListener changeListener;
	
	ScriptLoader(File scriptsRootDir) {
		this.scriptsRootDir = scriptsRootDir;
	}
	
	void setChangeListener(ScriptChangeListener changeListener) {
		this.changeListener = changeListener;
	}
	
	File loadScriptFile(Resource scriptResource) throws ScriptException {
		
		File script = null;
		
		try {
		
			script = mapScriptResourceToFile(scriptResource);
					
			if(!script.exists()) {
				makeNewFile(scriptResource, script);
			} else if(isScriptFileOutOfDate(scriptResource, script)) { 
				log.debug("Deleting outdated file {}", script.getAbsolutePath());
				makeNewFile(scriptResource, script, true);
			}
			
			log.debug("Loaded script file {}", script.getAbsolutePath());
		
		} finally {
			// this.unlockToRead(scriptResource);
		}
		
		return script;
	}
	
	private void makeNewFile(Resource scriptResource, File script) throws ScriptException {
		makeNewFile(scriptResource, script, false);
	}
	
	private void makeNewFile(Resource scriptResource, File scriptFile, boolean deleteCurrent) throws ScriptException {
		
		synchronized(this) {
			unlockToRead(scriptResource);
			lockToWrite(scriptResource);
		}
		
		try {
			String filePath = scriptFile.getAbsolutePath();
			
			if(deleteCurrent) {
				scriptFile.delete();
				
				if(changeListener != null) {
					changeListener.onChange(filePath);
				}
			}
			
			log.debug("Creating file {}", filePath);
			scriptFile.getParentFile().mkdirs();
			scriptFile.createNewFile();
			writeScriptFile(scriptResource, scriptFile);
			lockToRead(scriptResource);
			
		} catch (IOException e) {
			log.error("Unable to create script file for {}", new String[] {scriptResource.getPath()}, e);
			throw new ScriptException(e);
		} finally {
			unlockToWrite(scriptResource);
		}
	}
	
	void lockToRead(Resource scriptResource) {
		log.debug("read lock {}", scriptResource.getPath());
		getFileLock(scriptResource.getPath()).readLock().lock();
	}
	
	void lockToWrite(Resource scriptResource) {
		log.debug("write lock {}", scriptResource.getPath());
		getFileLock(scriptResource.getPath()).writeLock().lock();
	}
	
	void unlockToRead(Resource scriptResource) {
		log.debug("read unlock {}", scriptResource.getPath());
		getFileLock(scriptResource.getPath()).readLock().unlock();
	}
	
	void unlockToWrite(Resource scriptResource) {
		log.debug("write unlock {}", scriptResource.getPath());
		getFileLock(scriptResource.getPath()).writeLock().unlock();
	}
	
	private File mapScriptResourceToFile(Resource scriptResource) {
		String resourcePath = scriptResource.getPath().substring(1);
		return new File(scriptsRootDir, resourcePath);
	}
	
	private void writeScriptFile(Resource scriptResource, File scriptFile) throws IOException {
		InputStream in = scriptResource.getChild("jcr:content").adaptTo(InputStream.class);
		OutputStream out = new FileOutputStream(scriptFile);
		
		try {
			byte[] buf = new byte[32768];
			int r =0;
			while((r=in.read(buf)) >= 0) {
				out.write(buf, 0, r);
				out.flush();
			}
		} finally {
			out.flush();
			out.close();
		}
		
	}
	
	private boolean isScriptFileOutOfDate(Resource scriptResource, File scriptFile) {
		Resource resource = scriptResource.getChild("jcr:content");
		if(resource == null) {
			resource = scriptResource;
		}
		
		log.debug("Checking modification time for {}", resource.getPath());
		
		// This does not seem to reliably return modification time for nt:resource node when file is modified.
		// long resourceTime = resource.getResourceMetadata().getModificationTime();
		
		long resourceTime = 0;
		try {
			resourceTime = getResourceModificationTime(resource);
		} catch (RepositoryException e) {
			log.error("Unable to get resource modification time for {}", new String[]{resource.getPath()}, e);
		}
		
		if(resourceTime > 0 && scriptFile.exists()) {
			Path filePath = Paths.get(scriptFile.getAbsolutePath());
			try {
				BasicFileAttributes attr = Files.getFileAttributeView(filePath, BasicFileAttributeView.class).readAttributes();
				long fileTime = attr.lastModifiedTime().toMillis();
				boolean isFileOld = resourceTime > fileTime;
				log.debug("isScriptFileOutOfDate check is returning {}. JCR mod time is {}. File mod time is {}", 
						new Object[] { isFileOld, new Date(resourceTime).toString(), new Date(fileTime).toString() });
				return isFileOld;
			} catch (IOException e) {
				log.error("Unable to get file creation time for file {}", new String[]{scriptFile.getAbsolutePath()}, e);
			}
		}
		return true;
	}
	
	private long getResourceModificationTime(Resource resource) throws RepositoryException {
		Node node = resource.adaptTo( Node.class );
		Property p = null;
		
		if (node.hasProperty("jcr:lastModified")) {
			p = node.getProperty("jcr:lastModified");
		} else if(node.hasProperty("jcr:created")) {
			p = node.getProperty("jcr:created");
		}
		
		if(p != null) {
			return p.getDate().getTimeInMillis();
		}
		
		return 0;
	}
	
	private ReadWriteLock getFileLock(String path) {
		ReadWriteLock lock = fileLocks.get(path);
		if(lock == null) {
			lock = new ReentrantReadWriteLock();
			final ReadWriteLock existingLock = fileLocks.putIfAbsent(path, lock);
			if(existingLock != null) {
				lock = existingLock;
			}
		}
		return lock;
	}

}
