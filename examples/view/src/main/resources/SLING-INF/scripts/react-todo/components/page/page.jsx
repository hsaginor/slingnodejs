{/*
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
 */}
import React from 'react';
import ReactDOM from 'react-dom';
import ReactDOMServer from 'react-dom/server';
import Header from '../react-components/Header';
import TodoList from '../react-components/TodoList';
import TodoApp from '../react-components/TodoApp';

class TodoPage extends React.Component {

	// server side rendering
    renderServerResponse() {
        const path = resource.getPath();
        const model = resource.adaptTo("org.apache.sling.nodejs.examples.models.todo.TodoList");
        const title = model.getTitle();
		const items = this.getItems(model);

        return ReactDOMServer.renderToString(
            (
            <html>
            	<head>
					<title>{title}</title>
					<link rel="stylesheet" type="text/css" href="/etc/todo/css/app.css" />
            	</head>
            	<body>
                <div id="TodoAppRoot" data-resource-path={path}>
                <TodoApp title={title} items={items}/>
            		</div>
                <script src={ "/clientlib" + resource.getPath() + "/jsbundle.js" } />
            	</body>
            </html>
        	)
        );
    }
    
    // client side rendering
    renderClientSide() {
    		const apiPath = document.getElementById('TodoAppRoot').attributes['data-resource-path'].value;
        const getapiPath = apiPath + ".model.json"
    		fetch(getapiPath, {
            credentials: "same-origin",
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }}
        ).then((response) => response.json())
        .then(result => {
        		const title = result.title;
        		const items = result.items;
            ReactDOM.hydrate(<TodoApp title={title} items={items} apiPath={apiPath}/>, document.getElementById('TodoAppRoot'));
        },
        error => {
            // alert(error);
        });
    }

    getItems(model) {
        const items = [];
        const itemsList = model.getItems();

        log.debug("Iterating over " + itemsList.size() + " items now.");
        for(var i = 0; i < itemsList.size(); i++) {
            var itemi = itemsList.get(i);
            var item = {
                name: itemi.getName(), 
                done: itemi.isDone(), 
                itemKey: itemi.getItemKey(),
                path: itemi.getPath()};
            items.push(item);
        }

        return items;
    }
}

const page = new TodoPage();
export default page;

// client side rendering
if (typeof document != "undefined") {
	page.renderClientSide();
}