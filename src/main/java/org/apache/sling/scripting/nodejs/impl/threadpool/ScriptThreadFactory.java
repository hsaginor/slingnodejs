package org.apache.sling.scripting.nodejs.impl.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ScriptThreadFactory implements ThreadFactory {

	private static final Logger log = LoggerFactory.getLogger( ScriptThreadFactory.class );
	
	private volatile int threadCounter = 0;
	private List<ScriptProcessor> runningScriptProcessors = new ArrayList<ScriptProcessor>();
	
	@Override
	public Thread newThread(Runnable r) {
		int counter = ++threadCounter;
		ScriptProcessor scriptProcessor = new ScriptProcessor(r);
		scriptProcessor.setName("NodeJS-"+counter);
		runningScriptProcessors.add(scriptProcessor);
		return scriptProcessor;
	}
	
	void scriptChanged(String path) {
		for(ScriptProcessor p : runningScriptProcessors) {
			p.scriptChanged(path);
		}
	}

}
