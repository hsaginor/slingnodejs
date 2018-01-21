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
