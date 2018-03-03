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
package org.apache.sling.scripting.nodejs.impl.objects;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.scripting.core.servlet.CaptureResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

public class ResourceIncluder implements JavaVoidCallback {
	private static final Logger log = LoggerFactory.getLogger( ResourceIncluder.class );

	public final String RESOURCE_ARG = "resource";
	public final String PATH_ARG = "path";
	public final String RESROUCE_TYPE_ARG = "resourceType";
	public final String REPLACE_SELECTORS_ARG = "replaceSelectors";
	public final String ADD_SELECTORS_ARG = "addSelectors";
	public final String REPLACE_SUFFIX_ARG = "replaceSuffix";
	public final String FLUSH_ARG = "flush";
	public final String VAR_ARG = "var";
	
	private SlingHttpServletRequest request;
	private SlingHttpServletResponse response;
	private V8 runtime;
	
	public ResourceIncluder(SlingHttpServletRequest request, SlingHttpServletResponse response, V8 runtime) {
		if(request == null || response == null || runtime == null)
			throw new NullPointerException();
		
		this.request = request;
		this.response = response;
		this.runtime = runtime;
	}
	
	public void include(V8Object config) {
		IncludeOptions configOptions = this.getOptioons(config);
		log.info("Include -> {}", configOptions.toString());
		
		final RequestDispatcherOptions dispatcherOptions = new RequestDispatcherOptions();
        dispatcherOptions.setForceResourceType(configOptions.resourceType);
        dispatcherOptions.setReplaceSelectors(configOptions.replaceSelectors);
        dispatcherOptions.setAddSelectors(configOptions.addSelectors);
        dispatcherOptions.setReplaceSuffix(configOptions.replaceSuffix);

        // ensure the path (if set) is absolute and normalized
        String path = configOptions.path;
        if (path != null) {
            if (!path.startsWith("/")) {
                path = request.getResource().getPath() + "/" + path;
            }
            path = ResourceUtil.normalize(path);
        }

        // check the resource
        Resource resource = configOptions.resource;
        if (resource == null) {
            if (path == null) {
                // neither resource nor path is defined, use current resource
                resource = request.getResource();
            } else {
                // check whether the path (would) resolve, else SyntheticRes.
                Resource tmp = request.getResourceResolver().resolve(path);
                if (tmp == null && configOptions.resourceType != null) {
                    resource = new DispatcherSyntheticResource(
                        request.getResourceResolver(), path, configOptions.resourceType);

                    // remove resource type overwrite as synthetic resource
                    // is correctly typed as requested
                    dispatcherOptions.remove(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE);
                }
            }
        } else {
        		path = resource.getPath();
        }
        
        RequestDispatcher dispatcher = null;
        if (resource != null) {
            dispatcher = request.getRequestDispatcher(resource, dispatcherOptions);
        } else {
            dispatcher = request.getRequestDispatcher(path, dispatcherOptions);
        }
  
        
        if(dispatcher != null) {
			try {
				dispatch(dispatcher, configOptions);
			} catch (IOException e) {
				log.error("Unable to render include content for resource {}", new Object[] {path}, e);
			} catch (ServletException e) {
				log.error("Unable to render include content for resource {}", new Object[] {path}, e);
			}
        } else {
        		log.warn("Unable to render content included on {}. Make sure either resource or path argument is specified.", 
        				new Object[] {request.getRequestPathInfo().getResourcePath()});
        }
	}
	
	private void dispatch(RequestDispatcher dispatcher, IncludeOptions options) throws IOException, ServletException {
		if(options.flush) {
			response.getWriter().flush();
		}
		
		if(options.var == null) {
			dispatcher.include(request, response);
		} else {
			final CaptureResponseWrapper wrapper = new CaptureResponseWrapper((HttpServletResponse) response);
			dispatcher.include(request, wrapper);
			if (!wrapper.isBinaryResponse()) {
				String output = wrapper.getCapturedCharacterResponse();
				output = StringEscapeUtils.escapeJavaScript(output);
				String script = options.var + " = " + "\"" + output + "\";";
				log.info(script);
				runtime.executeVoidScript(script);
			}
		}
	}
	
	private void inspect(V8Object obj) {
		log.debug("include config V8Object:");
		for(String k : obj.getKeys()) {
			// int type = obj.getType(k);
			log.info("		key: {}", new Object[]{k});
			try {
				log.info("			type: {}", new Object[]{obj.getType(k)});
				if(V8Value.STRING == obj.getType(k)) {
					log.info("			value: {}", new Object[]{obj.getString(k)});
				}
			} catch(Exception e) {}
		}
	}
	
	private IncludeOptions getOptioons(V8Object config) {
		IncludeOptions options = new IncludeOptions();
		
		for(String k : config.getKeys()) {
			if(RESOURCE_ARG.equals(k)) {
				Object obj = config.get(k);
				if(obj instanceof Resource) {
					options.resource = (Resource) obj;
				} else {
					log.warn("The resource include argument type {} is not recognized", obj.getClass().getName());
				}
				continue;
			}
			if(PATH_ARG.equals(k)) {
				options.path = config.getString(k);
				continue;
			}
			if(RESROUCE_TYPE_ARG.equals(k)) {
				options.resourceType = config.getString(k);
				continue;
			}
			if(REPLACE_SELECTORS_ARG.equals(k)) {
				options.replaceSelectors = config.getString(k);
				continue;
			}
			if(ADD_SELECTORS_ARG.equals(k)) {
				options.addSelectors = config.getString(k);
				continue;
			}
			if(REPLACE_SUFFIX_ARG.equals(k)) {
				options.replaceSuffix = config.getString(k);
				continue;
			}
			if(FLUSH_ARG.equals(k)) {
				options.flush = config.getBoolean(k);
				continue;
			}
			if(VAR_ARG.equals(k)) {
				options.var = config.getString(k);
				continue;
			}
		}
	
		return options;
	}
	
	private class IncludeOptions {
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			if(resource!=null) {
				sb.append("resource").append(" = ").append(resource.toString()).append(", ");
			} else {
				sb.append("resource").append(" = ").append("null").append(", ");
			}
			sb.append("path").append(" = ").append(path).append(", ");
			sb.append("resourceType").append(" = ").append(resourceType).append(", ");
			sb.append("replaceSelectors").append(" = ").append(replaceSelectors).append(", ");
			sb.append("addSelectors").append(" = ").append(addSelectors).append(", ");
			sb.append("replaceSuffix").append(" = ").append(replaceSuffix).append(", ");
			sb.append("flush").append(" = ").append(flush).append(", ");
			sb.append("var").append(" = ").append(var);
			sb.append("}");
			return sb.toString();
		}

		/** resource argument */
	    private Resource resource;

	    /** path argument */
	    private String path;

	    /** resource type argument */
	    private String resourceType;

	    /** replace selectors argument */
	    private String replaceSelectors;

	    /** additional selectors argument */
	    private String addSelectors;

	    /** replace suffix argument */
	    private String replaceSuffix;
	    
	    /** flush argument */
	    private boolean flush = false;
	    
	    /** argument for a name of scripting variable to assign include output to **/
	    private String var = null;
	}
	
	/**
     * The <code>DispatcherSyntheticResource</code> extends the
     * <code>SyntheticResource</code> class overwriting the
     * {@link #getResourceSuperType()} method to provide a possibly non-
     * <code>null</code> result.
     */
    private class DispatcherSyntheticResource extends SyntheticResource {

        public DispatcherSyntheticResource(ResourceResolver resourceResolver,
                String path, String resourceType) {
            super(resourceResolver, path, resourceType);
        }

        @Override
        public String getResourceSuperType() {
            return getResourceResolver().getParentResourceType(getResourceType());
        }
    }

	@Override
	public void invoke(V8Object receiver, V8Array parameters) {
		if (parameters.length() > 0) {
		      Object arg = parameters.get(0);
		      if(arg instanceof V8Object) {
			    	  include((V8Object)arg);
		      }
		      
		      if(arg instanceof Releasable) {
			    	  ((Releasable) arg).release();
		      }
		}
	}
}
