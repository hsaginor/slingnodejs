package org.apache.sling.scripting.nodejs.impl.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public final class NodeScriptsUtil {

	private static final Logger log = LoggerFactory.getLogger( NodeScriptsUtil.class );
	
	private static final String UTIL_SCRIPT = "module.exports = { delete(path) { if(require.cache[path]) { delete require.cache[path]; log.debug('Cleared {} from node cache.', path); } } }";
    private static final String UTIL_SCRIPT_NAME = "slingNodeUtil";
    
    private static Object fileCreationLock = new Object();
    private static File utilScriptFile;
    private static NodeScriptsUtil instance;
    
	private NodeScriptsUtil() {
		init();
	}
	
	public void clearNodeCache(NodeJS node, String path) {
		if(utilScriptFile != null && utilScriptFile.exists()) {
			V8Object script = (V8Object) node.require(utilScriptFile);
			V8Array params = new V8Array(node.getRuntime());
			params.push(path);
			script.executeFunction("delete", params);
			params.release();
			script.release();
		} else {
			log.error("Unable to clear {} from node cache. Util script doesn't exist.", new Object[] { path });
		}
	}
	
	public static final NodeScriptsUtil getNodeScriptsUtil() {
		if(instance == null) {
			instance = new NodeScriptsUtil();
		}
		return instance;
	}
	
	void destroy() {
		if(utilScriptFile != null && utilScriptFile.exists()) {
			utilScriptFile.delete();
		}
	}
	
	private void init() {
		synchronized(fileCreationLock) {
			if(utilScriptFile == null) {
				try {
					utilScriptFile = createTemporaryScriptFile(UTIL_SCRIPT, UTIL_SCRIPT_NAME);
				} catch (IOException e) {
					log.error("Unable to create util script file.", e);
				}
			}
		}
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
