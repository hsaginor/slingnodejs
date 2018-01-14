package org.apache.sling.scripting.nodejs.impl.threadpool;

import java.util.concurrent.Callable;

import com.eclipsesource.v8.NodeJS;

public interface NodeJSTask<T> extends Callable<T> {

	public void setNode(NodeJS node);
}
