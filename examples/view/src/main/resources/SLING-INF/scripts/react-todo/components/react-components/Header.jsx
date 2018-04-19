import React from 'react';

class Header extends React.Component {

    constructor(props) {
        super();
        this.state = {
            title: props.title
        };
    }

	render() {
		return (
			<header className="App-header">
            <h1 className="App-title">{this.state.title}</h1>
        	</header>
        	);
	}
}

export default Header;