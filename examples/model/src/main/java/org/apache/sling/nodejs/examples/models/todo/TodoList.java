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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;

@Model(
		adaptables = {Resource.class},
		resourceType = "react-todo/components/page",
		defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
		)
@Exporter(
		name = "jackson",
		extensions = "json"
		)
public class TodoList {
	
	@Self
	private Resource resource;
	
	@Named("items")
	@ChildResource
	private List<TodoItem> items;
	
	@Named("title")
	@ValueMapValue(name = "jcr:title")
	@Default(values = "Todo List")
	private String title;
	
	@PostConstruct
	private void init() {
		
	}
	
	public String getTitle() {
		return title;
	}

	public List<TodoItem> getItems() {
		return items;
	}

}
