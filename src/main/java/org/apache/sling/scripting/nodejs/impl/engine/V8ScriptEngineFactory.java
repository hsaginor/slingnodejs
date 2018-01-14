package org.apache.sling.scripting.nodejs.impl.engine;

import java.io.File;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.nodejs.impl.threadpool.ScriptExecutionPool;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		name = "NodeJS Scripting Engine Factory", 
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		service = ScriptEngineFactory.class,
		immediate = true,
		property = {
				Constants.SERVICE_VENDOR + "=" + "The Apache Software Foundation",
				Constants.SERVICE_DESCRIPTION + "=" + "Scripting Engine using NodeJS module loader via J2V8 bridge.",
				"compatible.javax.script.name=jsx"
		})
@Designate( ocd = V8ScriptEngineConfiguration.class )
public class V8ScriptEngineFactory extends AbstractScriptEngineFactory {

	private final Logger log = LoggerFactory.getLogger( V8ScriptEngineFactory.class );
	
	private final static String DEFAULT_SCRIPTS_LOCATION = "node";
	
	public final static String SHORT_NAME = "V8";

    public final static String LANGUAGE_NAME = "Node JS Scripting Language";

    public final static String LANGUAGE_VERSION = "1.0";
    
    public final static String EXTENSION = "jsx";
    
    private int poolSize = V8ScriptEngineConfiguration.DEFAULT_NODE_POOL_SIZE;
    
    private ScriptExecutionPool threadPool;
    
    private ScriptLoader scriptLoader = null;
    
    private NodeScriptsUtil scriptsUtil = null;
    
    private V8ScriptEngine engine = null;
    
    public V8ScriptEngineFactory() {
    		setNames("jsx", "node", "NodeJS", SHORT_NAME);
    		setExtensions(EXTENSION);
    }
    
	@Override
	public String getLanguageName() {
		return LANGUAGE_NAME;
	}

	@Override
	public String getLanguageVersion() {
		return LANGUAGE_VERSION;
	}

	@Override
	public ScriptEngine getScriptEngine() {
		return engine;
	}
	
	public ScriptLoader getScriptLoader() {
		return scriptLoader;
	}
	
	public ScriptExecutionPool getScriptExecutionPool() {
		return threadPool;
	}

	/**
     * Activate this component
     */
    @Activate
    protected void activate(final ComponentContext context, final V8ScriptEngineConfiguration config) throws Exception {
    		log.debug("Activating V8 Node scripting.");
    		
    		// Dictionary<?, ?> properties = context.getProperties();
    		// BundleContext bundleContext = context.getBundleContext();
    		
    		poolSize = config.poolSize() > 0 ? config.poolSize() : V8ScriptEngineConfiguration.DEFAULT_NODE_POOL_SIZE; // PropertiesUtil.toInteger(properties.get(POOL_SIZE_PROPERTY), DEFAULT_NODE_POOL_SIZE);
    		String scriptsPath = config.scriptsFilePath(); // PropertiesUtil.toString(properties.get(NODE_SCRIPTS_FILEPATH_PROPERTY), "").trim();
    		
    		File scriptsFolder = null;
    		if(scriptsPath == null || scriptsPath.length() == 0) {
    			scriptsFolder = context.getBundleContext().getDataFile(DEFAULT_SCRIPTS_LOCATION);
    		} else {
    			scriptsFolder = new File(scriptsPath);
    		}
    		
    		if(!scriptsFolder.exists()) {
    			if(!scriptsFolder.mkdirs()) {
    				log.error("Unable to create scripts folder. Can't start V8 Node scripting engine.");
    				throw new ServiceException("Unable to create scripts folder. Can't start V8 Node scripting engine.");
    			}
    		}
    		
    		log.debug("Scripts directory {}. Pool size {}. java.library.path = {}", new Object[] {scriptsFolder.getAbsolutePath(), poolSize, System.getProperty("java.library.path")});
    		scriptLoader = new ScriptLoader(scriptsFolder);
    		scriptsUtil = NodeScriptsUtil.getNodeScriptsUtil();
    		
    		threadPool = new ScriptExecutionPool(poolSize);
    		engine = new V8ScriptEngine(this, threadPool);
    }
    
    @Deactivate
    protected void deactivate(final ComponentContext context) { 
    		if(engine != null)
    			engine.release();
    		scriptsUtil.destroy();
    }

}
