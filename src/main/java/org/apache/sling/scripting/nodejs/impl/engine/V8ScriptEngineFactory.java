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

import java.io.File;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.Event;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.jackrabbit.api.observation.JackrabbitEventFilter;
import org.apache.jackrabbit.api.observation.JackrabbitObservationManager;
import org.apache.sling.api.request.SlingRequestListener;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.nodejs.impl.clientside.ScriptCollector;
import org.apache.sling.scripting.nodejs.impl.objects.V8ObjectWrapper;
import org.apache.sling.scripting.nodejs.impl.threadpool.ScriptExecutionPool;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
    
    public final static String SCRIPTS_SRC_DIR = "src";
    
    public final static String SCRIPTS_OUT_DIR = "out"; 
    
    // method used by ScriptChangeObserver
    private String[] getEngineExtensions() {
		return new String[] { "."+EXTENSION };
    }
    
    @Reference( target="(component.name=org.apache.sling.scripting.nodejs.impl.clientside.ScriptCollector)")
    private SlingRequestListener scriptCollector;
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;
    
    private ClassLoader dynamicClassLoader;
    
    private int poolSize = V8ScriptEngineConfiguration.DEFAULT_NODE_POOL_SIZE;
    
    private ScriptExecutionPool threadPool;
    
    private ScriptLoader scriptLoader = null;
    
    private NodeScriptsUtil scriptsUtil = null;
    
    private V8ScriptEngine engine = null;
    
    private String[] nodeConfigFileNames = { ".babelrc", "package.json" }; 
    
    private ResourceResolver resolver;
   
    private ObservationManager observationManager;
    
    private ScriptChangeObserver changeObserver;
    
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
	
	ScriptCollector getScriptCollector() {
		return (ScriptCollector) scriptCollector;
	}

	/**
     * Activate this component
     */
    @Activate
    protected void activate(final ComponentContext context, final V8ScriptEngineConfiguration config) throws Exception {
    		log.debug("Activating V8 Node scripting.");
    		
    		poolSize = config.poolSize() > 0 ? config.poolSize() : V8ScriptEngineConfiguration.DEFAULT_NODE_POOL_SIZE; 
    		String scriptsPath = config.scriptsFilePath(); 
    		
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
    		
    		File srcDir = new File(scriptsFolder, SCRIPTS_SRC_DIR);
    		if(!srcDir.exists()) 
    			srcDir.mkdir();
    		File outDir = new File(scriptsFolder, SCRIPTS_OUT_DIR);
    		if(!outDir.exists())
    			outDir.mkdir();
    		

    		NodeBuilder builder = new NodeBuilder();
    		builder.init(scriptsFolder);
    		builder.executeBuild(scriptsFolder);
    		
    		log.debug("Scripts directory {}. Pool size {}. java.library.path = {}", new Object[] {scriptsFolder.getAbsolutePath(), poolSize, System.getProperty("java.library.path")});
    		scriptLoader = new ScriptLoader(scriptsFolder, builder);
    		scriptsUtil = NodeScriptsUtil.getNodeScriptsUtil();
    		
    		getScriptCollector().setNodeBuilder(builder, scriptsFolder);
    		
    		threadPool = new ScriptExecutionPool(poolSize);
    		dynamicClassLoader = dynamicClassLoaderManager.getDynamicClassLoader();
    		engine = new V8ScriptEngine(dynamicClassLoader, this, threadPool);
    		
    		long maxScriptObjects = config.maxScriptObjects();
    		if(maxScriptObjects > 0 && maxScriptObjects != V8ObjectWrapper.DEFAULT_MAX_CACHED_OBJECTS) {
    			V8ObjectWrapper.setMaxChachedObjects(maxScriptObjects);
    		}
    		
    		// TODO change to service user
    		resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
    		registerChangeListener(resolver);
   
    }
    
    @Deactivate
    protected void deactivate(final ComponentContext context) { 
    		if(resolver != null && resolver.isLive()) {
			try {
				observationManager.removeEventListener(changeObserver);
			} catch (RepositoryException e) {
				log.error("Error removing event listener.", e);
			}
    			
    			resolver.close();
    		}
    		if(engine != null)
    			engine.release();
    		scriptsUtil.destroy();
    }
    
    private void registerChangeListener(ResourceResolver resolver) throws RepositoryException {
    		Session session = resolver.adaptTo( Session.class );
    		this.observationManager = session.getWorkspace().getObservationManager();
    		this.changeObserver = new ScriptChangeObserver(scriptLoader, resolver, getEngineExtensions(), nodeConfigFileNames);
    		
    		JackrabbitObservationManager observationManager = (JackrabbitObservationManager) this.observationManager;
    		
    		JackrabbitEventFilter eventFilter = new JackrabbitEventFilter();
		eventFilter.setAbsPath(resolver.getSearchPath()[0]).setEventTypes(Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED)
				.setIsDeep(true).setNoLocal(false).setNoExternal(true);
		
		ArrayList<String> paths = new ArrayList<String>();
		for(String searchPath : resolver.getSearchPath()) {
			paths.add(searchPath);
		}
		
		// add default config file paths
		for(String configFile : nodeConfigFileNames) {
			String path = "/" + configFile;
			paths.add(path);
		}
		
		if (paths.size() > 1) {
            eventFilter.setAdditionalPaths(paths.toArray(new String[paths.size()]));
        }
		
		observationManager.addEventListener(this.changeObserver, eventFilter);
    }
    
    private String[] getScriptsSearchNodeIds(Session session, String searchPaths[]) {
    		ArrayList<String> ids = new ArrayList<String>();
    		
    		if(searchPaths != null && searchPaths.length > 0) {
    			for(String path : searchPaths) {
				try {
					Node node = session.getNode(path);
					String id = node.getIdentifier();
					log.debug("Path {} with node id {} will be registered to listen for scripts change events.", new Object[] {path, id});
					ids.add(id);
				} catch (RepositoryException e) {
					log.warn("Error getting node id for {} to register change listener.", new Object[] {path}, e);
				}
    			}
    		}
    		
    		if(ids.isEmpty()) 
    			return null;
    		
    		return ids.toArray(new String[ids.size()]);
    }

}
