import React from 'react';

class TodoItem extends React.Component {
	render() {
        const itemHref = "#_" + this.props.index;
		return (
            <li style={{
                    textDecoration: this.props.done ? 'line-through' : 'none'
            }}>{ this.props.name } <a href={itemHref} onClick={this.props.onToggle}>{ this.props.done ? 'Done' : 'Not Done'}</a></li>
        );
	}
}

export default TodoItem;