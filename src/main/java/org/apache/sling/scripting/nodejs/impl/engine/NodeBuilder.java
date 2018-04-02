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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeBuilder {

	private final Logger log = LoggerFactory.getLogger( NodeBuilder.class );
	
	private static final String INSTALL_COMMAND = "npm install -S -D";
	
	private static final String INSTALL_BROWSERIFY = "npm install -g browserify grasp";
	
	private static final String BUILD_COMMAND = "npm run build";
	
	public static final String BUNDLE_FILE_SELECTOR = ".bundle.";
	
	public synchronized void init(File dir) {
		executeCommand(INSTALL_COMMAND, dir);
		executeCommand(INSTALL_BROWSERIFY, dir);
	}
	public synchronized void executeInstall(File dir) {
		executeCommand(INSTALL_COMMAND, dir);
	}
	
	public synchronized void executeBuild(File dir) {
		executeCommand(BUILD_COMMAND, dir);
	}
	
	public synchronized void browserfy(File buildDir, List<String> scripts, String outputFilePath) {
		File outputFile = new File(outputFilePath);
		if(outputFile.exists()) {
			// This file is already created
			return;
		} else {
			outputFile = null;
		}
		
		// Build the command
		StringBuilder commandBuilder = new StringBuilder("browserify ");
		commandBuilder.append("-t graspify ");
		//commandBuilder.append(scripts.get(0)).append(' ');
		//commandBuilder.append("-t graspify -e 'renderServerResponse() { _$ }' -R '// browserified'  ");
		for(String f : scripts) {
			commandBuilder.append(f).append(' ');
		}
		commandBuilder.append("-o ").append(outputFilePath);
		
		// Run the command
		String command = commandBuilder.toString();
		Process p = executeCommand(command, buildDir);
		
		// delete old generated bundles
		if(p.exitValue() == 0) {
			String dirPath = outputFilePath.substring(0, outputFilePath.lastIndexOf('/'));
			File dir = new File(dirPath);
			if(dir.exists() && dir.isDirectory()) {
				File oldFiles[] = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.contains(BUNDLE_FILE_SELECTOR);
					}
				});
				
				for(File f : oldFiles) {
					if(f.exists() && f.isFile() && !outputFilePath.equals(f.getAbsolutePath())) {
						log.debug("Deleting old JS bundle {}.", f.getAbsolutePath());
						if(!f.delete()) {
							log.warn("Unable to delete {}.", f.getAbsolutePath());
						}
					}
				}
			} else {
				log.error("Unable to delete old bundles. Directory {} does not exist.", dirPath);
			}
		}
	}
	
	private Process executeCommand(String command, File dir) {

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command, null, dir);
			p.waitFor();
			int exitValue = p.exitValue();
			
			if(exitValue == 0) {
				if(log.isDebugEnabled()) {
					String info = getCommandInput(p.getInputStream());
					log.debug("Command '{}' exited with value {}", new Object[] {command,exitValue});
					log.debug(info);
				}
			} else {
				String error = getCommandInput(p.getErrorStream());
				log.error("Command '{}' exited with value {}", new Object[] {command,exitValue});
				log.error(error);
			}

		} catch (Exception e) {
			log.error("Unable to execute command '" + command + "'", e);
		}

		return p;

	}
	
	private String getCommandInput(InputStream in) throws IOException {
		StringBuffer output = new StringBuffer();

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String line = "";
		while ((line = reader.readLine()) != null) {
			output.append(line + "\n");
		}

		return output.toString();
	}
	
}
