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