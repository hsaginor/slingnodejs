import React from 'react';;
import Header from './Header';
import TodoList from './TodoList';

class TodoApp extends React.Component {
	render() {
    	return (
            <div>
            <Header title={this.props.title} />
            	<p className="App-intro">To get started, type in item name and click Add.</p>
        		<TodoList />
            </div>
        );
    }
}

export default TodoApp;