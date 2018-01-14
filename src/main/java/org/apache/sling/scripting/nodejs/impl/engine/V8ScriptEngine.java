package org.apache.sling.scripting.nodejs.impl.engine;

import java.io.Reader;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.nodejs.impl.threadpool.ScriptExecutionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V8ScriptEngine extends AbstractSlingScriptEngine {

	private static final Logger log = LoggerFactory.getLogger(V8ScriptEngine.class);
	
	private V8ScriptEngineFactory engineFactory;
	
	private ScriptExecutionPool threadPool;
	
	protected V8ScriptEngine(V8ScriptEngineFactory scriptEngineFactory, ScriptExecutionPool threadPool) {
		super(scriptEngineFactory);
		this.engineFactory = scriptEngineFactory;
		this.threadPool = threadPool;
		engineFactory.getScriptLoader().setChangeListener(threadPool);
	}
	
	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        SlingScript script = new SlingScript(context, engineFactory.getScriptLoader());
        threadPool.exec(script);
		
		return null;
	}
	
	void release() {
		log.debug("Stopping V8ScriptEngine");
		if(threadPool != null) {
			threadPool.shutdown();
		}
	}
	
}
