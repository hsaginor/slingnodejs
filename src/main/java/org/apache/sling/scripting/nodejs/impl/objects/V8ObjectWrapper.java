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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.apache.sling.scripting.nodejs.impl.exceptions.V8WrapperCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * V8Object wrapper that uses java reflection to expose object's public methods to V8 runtime.
 * 
 * @author hsaginor
 *
 */
public class V8ObjectWrapper implements Releasable {

	private static final Logger log = LoggerFactory.getLogger(V8ObjectWrapper.class);
			
	public static final long DEFAULT_MAX_CACHED_OBJECTS = 1000;
	
	private V8 runtime;
	private V8Object register;
	private String asName;
	private Object callableObject;
	private static long maxChachedObjects = DEFAULT_MAX_CACHED_OBJECTS;
	private ClassLoader dynamicClassLoader;
	
	// V8 provides only a way to handle primitives.
	// The ThreadLocal objectsCache is used to store and access wrapped objects when references are passed back from
	// JavaScript to Java
	private static final ThreadLocal<Map<String, V8ObjectWrapper>> objectsCache = new ThreadLocal<Map<String, V8ObjectWrapper>>();
	private List<String> cacheKeys = new ArrayList<String>();
	
	/**
	 * Constructor.
	 * 
	 * @param runtime - V8 runtime
	 * @param callableObject - the Java Object to be registered as an object in V8 environment. 
	 * @param asName - name the object will be registered as in V8 runtime
	 */
	public V8ObjectWrapper(ClassLoader dynamicClassLoader, V8 runtime, Object callableObject, String asName) {
		
		if(callableObject == null || dynamicClassLoader == null || asName == null || asName.trim().length()==0) {
			throw new IllegalArgumentException();
		}
		
		this.dynamicClassLoader = dynamicClassLoader;
		this.runtime = runtime;
		this.callableObject = callableObject;
		this.asName = asName.trim();
		register = new V8Object(runtime);
		initCallbacks();
		setCache();
	}
	
	/**
	 * This private constructor is only used when returning non-primitive Java objects from JavaScript callbacks.
	 * Since J2V8 only supports primitive we need to wrap these as well. 
	 * 
	 * @param runtime
	 * @param callableObject
	 */
	private V8ObjectWrapper(ClassLoader dynamicClassLoader, V8 runtime, Object callableObject) {
		// super(runtime, callableObject);
		
		if(callableObject == null || dynamicClassLoader == null) {
			throw new IllegalArgumentException();
		}
		
		this.dynamicClassLoader = dynamicClassLoader;
		this.runtime = runtime;
		this.callableObject = callableObject;
		register = new V8Object(runtime);
		initCallbacks();
		setCache();
	}
	
	public long getMaxChachedObjects() {
		return maxChachedObjects;
	}

	public static void setMaxChachedObjects(long maxChachedObjects) {
		V8ObjectWrapper.maxChachedObjects = maxChachedObjects;
	}

	@Override
	public void release() {
		clearCache();
		register.release();
		log.debug("{} objects left in cache.", new Object[] {objectsCache.get().size()});
	}
	
	private void clearCache() {
		for(String k : cacheKeys) {
			V8ObjectWrapper next = objectsCache.get().get(k);
			if(next != null) {
				next.release();
			}
		}
		String self = self();
		if(objectsCache.get().containsKey(self)) {
			objectsCache.get().remove(self);
			log.debug("Released {} from cache.", new Object[] {this.callableObject.getClass().getName()});
		}
	}
	
	/**
	 * Convenience method that returns an instance of the wrapped java object provided that receiver parameter represents one.
	 * 
	 * @param receiver
	 * @return Wrapped Java object if if receiver represents one. Null if it does not.
	 */
	public static final Object getSelf(V8Object receiver) {
		if(receiver.contains("self")) {
			String key = receiver.executeStringFunction("self", null);
			if(objectsCache.get().containsKey(key)) {
				return objectsCache.get().get(key).callableObject;
			}
		}
		return null;
	}
	
	private String self() {
		return Long.toString(register.hashCode());
	}
	
	private void setCache() {
		Map<String, V8ObjectWrapper> cache = objectsCache.get();
		if(cache == null) {
			cache = new HashMap<String, V8ObjectWrapper>();
			objectsCache.set(cache);
		}
		
		if(objectsCache.get().size() >= getMaxChachedObjects()) {
			throw new V8WrapperCallException("Maximum per request scriptable objects reached. Too many non-primitive objects returned from Java. Consider refectoring your JS code to create fewer scriptable objects in Java.");
		}
		
		cache.put(self(), this);
	}
	
	private void initCallbacks() {
		if(asName != null) {
			runtime.add(asName, register);
		} 
		for(Method m : callableObject.getClass().getMethods()) {
			String methodName = m.getName();
			Class<?>[] types = m.getParameterTypes();
			
			if(Void.TYPE.equals(m.getReturnType())) {
				addVoidCallback(m);
			} else {
				addCallback(m);
			}
			
			//if(log.isDebugEnabled()) {
			//	logMethod(callableObject.getClass().getName(), methodName, types);
			//}
		}
		
		// a way to get back the original object for when V8Object instance is passed back to java
		
		register.registerJavaMethod(new JavaCallback() {

			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				// return new SelfValue(runtime, self());
				return self();
			}
			
		}, "self");
	}
	
	private Object getReturnResult(final Object result) {
        if (result == null) {
            return result;
        }
        if (result instanceof Float) {
            return ((Float) result).doubleValue();
        }
        if ((result instanceof Integer) || (result instanceof Double) || (result instanceof Boolean)
                || (result instanceof String)) {
            return result;
        }
        
        log.debug("Wrapping method return result of type {}", result.getClass().getName());
        V8ObjectWrapper wrapper = new V8ObjectWrapper(dynamicClassLoader, runtime, result);
        this.cacheKeys.add(wrapper.self());
        return wrapper.register.twin();
    }
	
	private void logMethod(String type, String name, Class<?>[] argTypes) {
		StringBuilder sb = new StringBuilder("Registering ");
		sb.append(type).append('.').append(name).append("( ");
		for(Class c : argTypes) {
			sb.append(c.getName()).append(' ');
		}
		sb.append(")");
		log.debug(sb.toString());
	}
	
	private Map<String, WrapperCallback> callbacks = new HashMap<String, WrapperCallback>();
	
	private void addCallback(Method method) {
		String name = method.getName();
		if(callbacks.containsKey(name)) {
			callbacks.get(name).methods.add(method);
		} else {
			WrapperCallback callback = new WrapperCallback(method, name);
			register.registerJavaMethod(callback, name);
			callbacks.put(name, callback);
		}
	}
	
	private class WrapperCallback implements JavaCallback {

		// List of methods with the same name that maybe overloaded. 
		private List<Method> methods = new ArrayList<Method>();
		
		private String name;
		
		private WrapperCallback(Method method, String methodName) {
			name = methodName;
			methods.add(method);
		}
		
		@Override
		public Object invoke(V8Object receiver, V8Array parameters) {
			Object[] methodArgs = null;
			
			try {
				methodArgs = getArgs(parameters);

				// Special case for handling adapTo calls.
				// V8 scripts must pass class name as string.
				if((callableObject instanceof Adaptable) && "adaptTo".equals(name)) {
					Object o = methodArgs[0];
					
					if(o instanceof String) {
						String className = (String) o;
						try {
							Class clazz = dynamicClassLoader.loadClass(className);
							Object adapted = ((Adaptable) callableObject).adaptTo(clazz);
							return getReturnResult(adapted);
						} catch (ClassNotFoundException e) {
							log.error("Attempt to adaptTo {} to {} failed.", new String[] {callableObject.getClass().getName(), className}, e);
							return null;
						}
					}
		
				}
				
				Method method = getMethod(methods, methodArgs);
				if(method == null) {
					String msg = "Unable to invoke " + callableObject.getClass().getName() + "." + name + ". Parameter list does not match.";
					log.error(msg);
					throw new V8WrapperCallException(msg);
				}
				
				log.debug("Invoking method {}.{}", new Object[] {callableObject.getClass().getName(), name});
				try {
					return getReturnResult(invokeMethod(method, methodArgs));
				} catch (Exception e) {
					String msg = "Unable to invoke " 
								+ callableObject.getClass().getName() 
								+ "." + method.getName();
					log.error(msg, e);
					throw new V8WrapperCallException(msg, e);
				} 
			} finally {
				// Must release arguments to free J2V8 memory. 
				if(methodArgs != null) {
					releaseArgs(methodArgs);
				}
			}
		}
		
	};
	
	private Map<String, WrapperVoidCallback> voidCallbacks = new HashMap<String, WrapperVoidCallback>();
	
	private void addVoidCallback(Method method) {
		String name = method.getName();
		if(voidCallbacks.containsKey(name)) {
			voidCallbacks.get(name).methods.add(method);
		} else {
			WrapperVoidCallback voidCallback = new WrapperVoidCallback(method, name);
			register.registerJavaMethod(voidCallback, name);
			voidCallbacks.put(name, voidCallback);
		}
	}	
	
	private class WrapperVoidCallback implements JavaVoidCallback {

		// List of methods with the same name that maybe overloaded. 
		private List<Method> methods = new ArrayList<Method>();
		
		private String name;
		
		private WrapperVoidCallback(Method method, String methodName) {
			name = methodName;
			methods.add(method);
		}
		
		public String getName() {
			return name;
		}
		
		@Override
		public void invoke(V8Object receiver, V8Array parameters) {
			Object[] methodArgs = null;
			
			try {
				methodArgs = getArgs(parameters);
				
				Method method = getMethod(methods, methodArgs);
				if(method == null) {
					String msg = "Unable to invoke " + callableObject.getClass().getName() + "." + name + ". Parameter list does not match.";
					log.error(msg);
					throw new V8WrapperCallException(msg);
				}
				
				try {
					invokeVoidMethod(method, methodArgs);
				} catch (Exception e) {
					String msg = "Unable to invoke " + callableObject.getClass().getName() + "." + name;
					log.error(msg, e);
					throw new V8WrapperCallException(msg, e);
				} 
			} finally {
				if(methodArgs != null) {
					releaseArgs(methodArgs);
				}
			}
			
		}
		
	};
	
	private Object invokeMethod(Method m, Object[] args) throws Exception {
		if(!m.isAccessible())
			m.setAccessible(true);
		return m.invoke(callableObject, args);
	}
	
	private void invokeVoidMethod(Method m, Object[] args) throws Exception {
		if(!m.isAccessible())
			m.setAccessible(true);
		m.invoke(callableObject, args);
	}
	
	private Object[] getArgs(V8Array parameters) {
		Object[] methodArgs = new Object[parameters.length()];
		for(int i=0; i<methodArgs.length; i++) {
			Object arg = parameters.get(i);
			methodArgs[i] = arg;
		}
		return methodArgs;
	}
	
	private void releaseArgs(Object[] args) {
		for(Object arg : args) {
			if(arg instanceof Releasable) {
				((Releasable)arg).release();
			}
		}
	}
	
	private Method getMethod(List<Method> methods, Object[] methodArgs) {
		Method method = null;
		for(Method m : methods) {
			if(matchesTypes(m, methodArgs)) {
				method = m;
				break;
			}
		}
		return method;
	}
	
	private boolean matchesTypes(Method method, Object[] methodArgs) {
		boolean match = true;
		
		Class<?>[] types = method.getParameterTypes();
		if(types.length == methodArgs.length) {
			for(int i = 0; i < types.length; i++) {
				Class type = types[i];
				Object arg = methodArgs[i];
				
				if(!type.isInstance(arg) && !isPrimitiveAssignableFrom(type, arg)) {
					match = false;
				}
			}
		} else {
			match = false;
		}
		
		return match;
	}
	
	/**
	 * 
	 * J2V8 passes primitive arguments as Java object types. 
	 * There is no Java Reflection way to to check if true method arguments types are assignable from those via java unboxing.  
	 * There might be a better way.
	 */
	private boolean isPrimitiveAssignableFrom(Class type, Object arg) {
		
		if((arg instanceof Integer) && type.getName().equals("int")) {
			return true;
		}
		
		if((arg instanceof Float) && type.getName().equals("float")) {
			return true;
		}
		
		if((arg instanceof Double) && type.getName().equals("double")) {
			return true;
		}
		
		if((arg instanceof Boolean) && type.getName().equals("boolean")) {
			return true;
		}
		
		if((arg instanceof Short) && type.getName().equals("short")) {
			return true;
		}
		
		if((arg instanceof Long) && type.getName().equals("long")) {
			return true;
		}
		
		if((arg instanceof java.lang.Character) && type.getName().equals("char")) {
			return true;
		}
		
		if((arg instanceof Byte) && type.getName().equals("byte")) {
			return true;
		}
		
		return true;
	}

}
