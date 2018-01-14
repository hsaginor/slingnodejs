package org.apache.sling.scripting.nodejs.impl.objects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.nodejs.impl.engine.V8WrapperCallException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.eclipsesource.v8.utils.V8ObjectUtils;

/**
 * V8Object wrapper that uses java reflection to expose object's public methods to V8 runtime.
 * 
 * @author hsaginor
 *
 */
public class V8ObjectWrapper implements Releasable {

	private static final Logger log = LoggerFactory.getLogger(V8ObjectWrapper.class);
			
	private V8 runtime;
	private V8Object register;
	private String asName;
	private Object callableObject;
	
	/**
	 * Constructor.
	 * 
	 * @param runtime - V8 runtime
	 * @param callableObject - the Java Object to be registered as an object in V8 environment. 
	 * @param asName - name the object will be registered as in V8 runtime
	 */
	public V8ObjectWrapper(V8 runtime, Object callableObject, String asName) {
		
		if(callableObject == null || asName == null || asName.trim().length()==0) {
			throw new IllegalArgumentException();
		}
		
		this.runtime = runtime;
		this.callableObject = callableObject;
		this.asName = asName.trim();
		register = new V8Object(runtime);
		initCallbacks();
	}
	
	@Override
	public void release() {
		register.release();
		log.debug("Released {}", callableObject.getClass().getName());
	}
	
	private void initCallbacks() {
		runtime.add(asName, register);
		for(Method m : callableObject.getClass().getMethods()) {
			String methodName = m.getName();
			Class<?>[] types = m.getParameterTypes();
			
			if(Void.TYPE.equals(m.getReturnType())) {
				addVoidCallback(m);
			} else {
				addCallback(m);
			}
			
			if(log.isDebugEnabled()) {
				logMethod(callableObject.getClass().getName(), methodName, types);
			}
		}
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
			Object[] methodArgs = getArgs(parameters);
			
			Method method = getMethod(methods, methodArgs);
			if(method == null) {
				String msg = "Unable to invoke " + callableObject.getClass().getName() + "." + name + ". Parameter list does not match.";
				log.error(msg);
				throw new V8WrapperCallException(msg);
			}
			
			try {
				return invokeMethod(method, methodArgs);
			} catch (Exception e) {
				String msg = "Unable to invoke " 
							+ callableObject.getClass().getName() 
							+ "." + method.getName();
				log.error(msg, e);
				throw new V8WrapperCallException(msg, e);
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
			Object[] methodArgs = getArgs(parameters);
			
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
			if(arg instanceof Releasable) {
				((Releasable)arg).release();
			}
		}
		return methodArgs;
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
				
				if(!type.isInstance(arg)) {
					match = false;
				}
			}
		} else {
			match = false;
		}
		
		return match;
	}

}
