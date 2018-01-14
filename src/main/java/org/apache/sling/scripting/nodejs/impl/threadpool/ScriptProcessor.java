package org.apache.sling.scripting.nodejs.impl.threadpool;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.sling.scripting.nodejs.impl.engine.NodeScriptsUtil;
import org.apache.sling.scripting.nodejs.impl.engine.SlingScript;
import org.apache.sling.scripting.nodejs.impl.objects.SlingLogger;
import org.apache.sling.scripting.nodejs.impl.objects.V8ObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;

public class ScriptProcessor extends Thread {

	private static final Logger log = LoggerFactory.getLogger(ScriptProcessor.class);
	
	private NodeJS nodeJS;
	private V8ObjectWrapper slingNodeLogger;
	private Runnable target;
	
	private Queue<String> changedScripts = new ConcurrentLinkedQueue<String>();

	public ScriptProcessor(Runnable target) {
		super(target);
		this.target = target;
	}
	
	
	@Override
	public void run() {
		target.run();
		release();
	}
	
	static final Callable<?> createWorker(final NodeJSTask<?> task) {
		return new Callable<Object>() {

			@Override
			public Object call() throws Exception {
				Thread t = Thread.currentThread();
				
				if(t instanceof ScriptProcessor) {
					return ((ScriptProcessor) t).eval(task);
				}
				
				return null;
			}
			
		};
	}


	private Object eval(NodeJSTask<?> task) throws Exception {
		
        NodeJS nodeJS = getNodeJS();
        
        try {
        		processScriptChanges(nodeJS);
        		task.setNode(nodeJS);
        		return task.call();
        } finally {
        		ungetNodeJS(nodeJS);
        		log.debug("Finished processing by thread {}", getName());
        }
	}
	
	void release() {
		log.debug("Stopping V8ScriptEngine thread {}", getName());
		NodeJS nodeJS = getNodeJS();
		try {
			slingNodeLogger.release();
			// printRuntimeObjects(nodeJS);
			nodeJS.release();
		} finally {
			ungetNodeJS(nodeJS);
		}
	}
	
	void scriptChanged(String path) {
		changedScripts.offer(path);
	}
	
	private void processScriptChanges(NodeJS node) {
		NodeScriptsUtil util = NodeScriptsUtil.getNodeScriptsUtil();
		
		while(!changedScripts.isEmpty()) {
			String path = changedScripts.poll();
			if(path != null) {
				util.clearNodeCache(node, path);
			}
		}
	}
	
	private NodeJS getNodeJS() {
		if(nodeJS == null) {
			nodeJS = NodeJS.createNodeJS();
			SlingLogger logger = new SlingLogger();
			slingNodeLogger = new V8ObjectWrapper(nodeJS.getRuntime(), logger, SlingLogger.LOGGER_JS_NAME);
		}
		nodeJS.getRuntime().getLocker().acquire();
		return nodeJS;
	}
	
	private void ungetNodeJS(NodeJS nodeJS) {
		nodeJS.getRuntime().getLocker().release();
	}

}
