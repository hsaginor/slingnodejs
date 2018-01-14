package org.apache.sling.scripting.nodejs.impl.threadpool;

import org.apache.sling.scripting.nodejs.impl.engine.SlingScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;

class ScriptExecutionTask implements NodeJSTask<SlingScript> {

	private static final Logger log = LoggerFactory.getLogger( ScriptExecutionTask.class );
	
	private SlingScript script;
	private NodeJS nodeJS;
	
	ScriptExecutionTask(SlingScript script) {
		if(script == null) {
			throw new NullPointerException();
		}
		
		this.script = script;
	}
	
	@Override
	public void setNode(NodeJS node) {
		this.nodeJS = node;
	}

	@Override
	public SlingScript call() {
		/*
		Thread currentThread = Thread.currentThread();
		if(currentThread instanceof ScriptProcessor) {
			ScriptProcessor processor = (ScriptProcessor) currentThread;
			processor.eval(script);
		}
		*/
		if(nodeJS != null)
			eval();
		return script;
	}
	
	private void eval() {
        try {
        		script.eval(nodeJS);
        } finally {
        		script.release();
        		log.debug("Finished executing a script.");
        }
	}

}
