package org.apache.sling.scripting.nodejs.impl.engine;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "NodeJS Script Engine Configuration.", description = "Service configuration for NodeJs Script Engine")
public @interface V8ScriptEngineConfiguration {
    
    public final static int DEFAULT_NODE_POOL_SIZE = 10;
    
	@AttributeDefinition( 
			name = "NodeJS Pool Size", 
			description = "Number of Threads/NodeJS runtimes to process incoming requests.", 
			defaultValue = {""+DEFAULT_NODE_POOL_SIZE},
			required = true)
	public int poolSize();
	
	@AttributeDefinition( 
			name = "Scripts Path", 
			description = "Path where NodeJS scripts will be saved to file system and load as files by V8 Engine. This defaults to the data directory for this bundle.",
			required = false)
	public String scriptsFilePath();
}
