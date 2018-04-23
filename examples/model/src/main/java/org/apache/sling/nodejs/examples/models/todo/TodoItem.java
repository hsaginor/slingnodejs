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
package org.apache.sling.nodejs.examples.models.todo;

import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
		adaptables = {Resource.class},
		defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
		)
@Exporter(
		name = "jackson",
		extensions = "json"
		)
public class TodoItem {

	@Self
	private Resource resource;
	
	@Named("done")
	@ValueMapValue
	private boolean done;
	
	@Named("name")
	@ValueMapValue
	private String name;

	public boolean isDone() {
		return done;
	}

	public String getName() {
		return name;
	}
	
	@Named("itemKey")
	public String getItemKey() {
		return resource.getName();
	}
	
	@Named("path")
	public String getPath() {
		return resource.getPath();
	}
}
