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
    renderServerResponse() {
        const path = resource.getPath();
        log.debug("Adapting resource {}", path);
        const model = resource.adaptTo("org.apache.sling.nodejs.examples.models.todo.TodoList");
        const title = model.getTitle();

        return ReactDOMServer.renderToString(
            (
            <html>
            	<head>
					<title>{title}</title>
            	</head>
            	<body>
                <div id="TodoAppRoot" data-resource-path={path}>
                		<TodoApp title={title}/>
            		</div>
                <script src={ "/clientlib" + resource.getPath() + "/jsbundle.js" } />
            	</body>
            </html>
        	)
        );
    }
}

export default new TodoPage();

if (typeof document != "undefined") {
	const title = document.getElementsByClassName("App-title")[0].innerHTML;
	ReactDOM.hydrate(<TodoApp title={title} />, document.getElementById('TodoAppRoot'));
}