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
package org.apache.sling.scripting.nodejs.impl.threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.sling.scripting.nodejs.impl.engine.ScriptChangeListener;
import org.apache.sling.scripting.nodejs.impl.engine.SlingScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScriptExecutionPool implements ScriptChangeListener {
	
	private static final Logger log = LoggerFactory.getLogger( ScriptExecutionPool.class );

	private ExecutorService executorService;
	private ScriptThreadFactory threadFactory;
	private volatile boolean isShutdown;
	
	public ScriptExecutionPool(int poolSize) {
		threadFactory = new ScriptThreadFactory();
		executorService = Executors.newFixedThreadPool(poolSize, threadFactory);
	}
	
	public Object exec(NodeJSTask<?> task) throws ScriptException {
		Object result = null;
		
		try {
			Future<?> f = executorService.submit(ScriptProcessor.createWorker(task));
			result = f.get();
		} catch (InterruptedException e) {
			log.warn("Script execution interrupted.", e);
		} catch (ExecutionException e) {
			throw new ScriptException(e);
		} catch(java.util.concurrent.RejectedExecutionException e) {
			if(isShutdown) {
				log.info("Script execution request recieved after shutdown call.");
			} else {
				log.error("Unable to precess another script execution request.", e);
				throw new ScriptException(e);
			}
		}
		
		return result;
	}
	
	public SlingScript exec(SlingScript script) throws ScriptException {
		ScriptExecutionTask task = new ScriptExecutionTask(script);
		
		SlingScript result = null;
		try {
			result = (SlingScript)exec(task); 
		} finally {
			if(result != null && result.hasException()) {
				throw result.getException();
			}
		}
		
		return result;
	}
	
	public void shutdown() {
		isShutdown = true;
		int waitToShutdown = 10;
		try {
			executorService.shutdown();
			executorService.awaitTermination(waitToShutdown, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			
		} finally {
			if(!executorService.isTerminated()) {
				log.info("ExecutorService is not yet terminated after {} seconds. Forcing shutdown.", waitToShutdown);
				executorService.shutdownNow();
			} else {
				log.debug("Clean ExecutorService shutdown after {} seconds.", waitToShutdown);
			}
		}
	}

	@Override
	public void onChange(String path) {
		threadFactory.scriptChanged(path);
	}

}
