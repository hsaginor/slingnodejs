import React from 'react';

class TodoInput extends React.Component {
    handleSubmit(event) {
        console.log("handleSubmit");
        event.preventDefault();
        this.props.onAddItem(this.refs.input.value);
        this.refs.input.value = '';
    }
    
	render() {
		return (
            <div>
            <form onSubmit={this.handleSubmit.bind(this)}>
                <input type="text" ref="input"/>
                <button>Add</button>
            </form>
            </div>
        );
	}
}

export default TodoInput;