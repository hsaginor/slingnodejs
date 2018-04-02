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
package org.apache.sling.scripting.nodejs.impl.clientside;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

// import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
@SlingServlet(
		resourceTypes = ClientSideJSResource.RESOURCE_TYPE,
		extensions = "js",
		methods = "GET"
		)
		*/
@Component(
		service = Servlet.class,
		immediate = true,
		property = {
				"sling.servlet.resourceTypes=" + ClientSideJSResource.RESOURCE_TYPE,
				"sling.servlet.extensions=js",
				"sling.servlet.methods=GET"
		}
)
public class ClienSideJSServlet extends SlingSafeMethodsServlet {

	private static final Logger log = LoggerFactory.getLogger( ClienSideJSServlet.class );

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		Resource res = request.getResource();
		String path = res.getPath();
		log.debug("Requested resource {}.", path);
		response.getWriter().println("You requested " + path);
	}

	

}
