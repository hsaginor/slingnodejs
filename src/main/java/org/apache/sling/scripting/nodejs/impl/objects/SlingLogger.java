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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.NodeJS;
import com.eclipsesource.v8.V8;

public final class SlingLogger {
	private static final Logger LOGGER = LoggerFactory.getLogger(SlingLogger.class);
	
	public static final String LOGGER_JS_NAME = "log";
	
	public void debug(String statement) {
		LOGGER.debug(statement);
	}

	public void debug(String statement, Object error) {
		LOGGER.debug(statement, error);
	}

	public void log(String statement) {
		LOGGER.info(statement);
	}

	public void log(String statement, Object error) {
		LOGGER.info(statement, error);
	}

	public void info(String statement) {
		LOGGER.info(statement);
	}

	public void info(String statement, Object error) {
		LOGGER.info(statement, error);
	}

	public void error(String statement) {
		LOGGER.error(statement);
	}

	public void error(String statement, Object error) {
		LOGGER.error(statement, error);
	}

	public void warn(String statement) {
		LOGGER.warn(statement);
	}

	public void warn(String statement, Object error) {
		LOGGER.warn(statement, error);
	}
    
}
