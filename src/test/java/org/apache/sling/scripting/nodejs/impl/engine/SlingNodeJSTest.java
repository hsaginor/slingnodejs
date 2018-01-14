package org.apache.sling.scripting.nodejs.impl.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.apache.sling.scripting.nodejs.impl.engine.SlingScript;
import org.apache.sling.scripting.nodejs.impl.objects.SlingLogger;
import org.apache.sling.scripting.nodejs.impl.objects.V8ObjectWrapper;
import org.apache.sling.scripting.nodejs.impl.threadpool.ScriptExecutionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class SlingNodeJSTest {

	private static final String SIMPLE_SCRIPT_PATH = "src/test/resources/testscripts/simple.jsx";
	private static final String UTIL_SCRIPT_PATH = "src/test/resources/testscripts/cacheutil.jsx";
	
	private File scriptFile;
	private NodeJS node;
	
	private String deleteCacheScript = null;
	private File deleteCacheFile; 
	
	@Before
	public void setUp() throws Exception {
		scriptFile = new File(SIMPLE_SCRIPT_PATH);
		node = NodeJS.createNodeJS();
		// deleteCacheScript = "module.exports = { delete(path) { delete require.cache[require.resolve(path)]; } }";
		deleteCacheScript = "module.exports = { " 
								+ " delete(path) { delete require.cache[path]; }," 
								+ " getcache(path) { return require.cache[path]; }"
								+ " }";
		// deleteCacheFile = new File(UTIL_SCRIPT_PATH); // createTemporaryScriptFile(deleteCacheScript, "deleteCacheScript");
		// watchCache();
		
		// System.out.println("Created tmp file " + deleteCacheFile.getAbsolutePath());
	}

	@After
	public void tearDown() throws Exception {
		node.release();
		// deleteCacheFile.delete();
	}
	
	@Test
	public void testScript() throws ScriptException {
		V8Object script = (V8Object) node.require(scriptFile);
		String result = script.executeStringFunction("render", null);
		assertTrue("Hello World!".equals(result));
		// System.out.println(result);
		// printCache();
		script.release();
	}
	
	// @Test 
	public void reloadScriptFile() throws ScriptException {
		for(int i=0; i<100; i++) {
			testScript();
			// runMessageLoop();
			scriptFile = new File(SIMPLE_SCRIPT_PATH);
			try { Thread.sleep(300); } catch (InterruptedException e) {}
			// clearCache();
		}
	}
	
	private void runMessageLoop() {
		System.out.println("Checking node message loop");
		while(node.isRunning()) {
			if(node.handleMessage()) {
				System.out.println("In node message loop.");
			}
			System.out.println("Node running");
		}
	}
	
	private void clearCache() {
		V8Object script = (V8Object) node.require(deleteCacheFile);
		V8Array params = new V8Array(node.getRuntime());
		params.push(scriptFile.getAbsolutePath());
		script.executeFunction("delete", params);
		params.release();
		script.release();
	}
	
	private void watchCache() {
		V8Object script = (V8Object) node.require(deleteCacheFile);
		V8Array params = new V8Array(node.getRuntime());
		params.push(scriptFile.getParentFile().getAbsolutePath());
		script.executeFunction("watchcache", params);
		params.release();
		script.release();
	}
	
	private void printCache() {
		V8Object script = (V8Object) node.require(deleteCacheFile);
		V8Array params = new V8Array(node.getRuntime());
		params.push(scriptFile.getAbsolutePath());
		V8Object cache = script.executeObjectFunction("getcache", params);
		printObject(cache, "", 0);
		params.release();
		cache.release();
		script.release();
	}
	
	private void printObject(V8Object obj, String indent, int level) {
		for(String key : obj.getKeys()) {
			Object value = obj.get(key);
			if(value == null) {
				System.out.println(indent + key + " = null");
				continue;
			}
			
			System.out.println(indent + key + " -> " + value.getClass().getName());
			
			if(!(value instanceof V8Array)) {
				System.out.println(indent + key + " = " + value);
			}
			
			if((value instanceof V8Object) && level < 5) {
				String newIndent = indent + "   ";
				int newLevel = level + 1;
				printObject((V8Object)value, newIndent, newLevel);
			}
			
			if(value instanceof Releasable) {
				((Releasable) value).release();
			}
		}
	}
	
	@Test
	public void testLogger() {
		V8 runtime = node.getRuntime();
		SlingLogger logger = new SlingLogger();
		V8ObjectWrapper logWrapper = new V8ObjectWrapper(runtime, logger, SlingLogger.LOGGER_JS_NAME);
		logWrapper.release();
		runtime.executeScript(SlingLogger.LOGGER_JS_NAME + ".log('log');");
		runtime.executeScript(SlingLogger.LOGGER_JS_NAME + ".log('log', 'log');");
		runtime.executeScript(SlingLogger.LOGGER_JS_NAME + ".debug('debug');");
		runtime.executeScript(SlingLogger.LOGGER_JS_NAME + ".info('info');");
		runtime.executeScript(SlingLogger.LOGGER_JS_NAME + ".error('error');");
	}
	
	// @Test
	public void testThreadPool() throws ScriptException {
		ScriptExecutionPool threadPool = new ScriptExecutionPool(10);
		List<Thread> callers = new ArrayList<Thread>();
		
		for(int i=0; i<100; i++) {
			SlingScript script = new SlingScript(scriptFile);
			Thread t = makeScriptRunnerThread(script, threadPool);
			callers.add(t);
		}
		
		for(Thread t : callers) {
			try { Thread.sleep(100); } catch (InterruptedException e) {}
			t.start();
		}
		
		while(isAlive(callers)) {
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		
		threadPool.shutdown();
	}
	
	// @Test
	public void printNodeObjects() {
		V8 v8 = node.getRuntime();
		String[] runtimeKeys = v8.getKeys();
		for(String k : runtimeKeys) {
			Object obj = v8.get(k);
			if(obj != null)
				System.out.println(k +" : " + obj.getClass().getName());
			if(obj instanceof Releasable) 
				((Releasable) obj).release();
		}
		
	}
	
	private boolean isAlive(List<Thread> callers) {
		for(Thread t : callers) {
			if(t.isAlive()) 
				return true;
		}
		return false;
	}
	
	private Thread makeScriptRunnerThread(final SlingScript script, final ScriptExecutionPool threadPool) {
		Runnable scriptRunner = makeScriptRunnable(script, threadPool);
		return new Thread(scriptRunner);
	}
	
	private Runnable makeScriptRunnable(final SlingScript script, final ScriptExecutionPool threadPool) {
		Runnable scriptRunner = new Runnable() {

			@Override
			public void run() {
				try {
					for(int i=0; i<10; i++) {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {}
						threadPool.exec(script);
						System.out.println("" + Thread.currentThread().getName() + " -> " + script.getOutput());
				
					}
				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
			
		};
		
		return scriptRunner;
	}
	
	private String loadScript() throws IOException {
		String contents = new String(Files.readAllBytes(Paths.get(SIMPLE_SCRIPT_PATH)));
		return contents;
	}
	
	private static File createTemporaryScriptFile(final String script, final String name) throws IOException {
        File tempFile = File.createTempFile(name, ".js");
        PrintWriter writer = new PrintWriter(tempFile, "UTF-8");
        try {
            writer.print(script);
        } finally {
            writer.close();
        }
        return tempFile;
    }

}
