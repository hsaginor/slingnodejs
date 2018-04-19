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
import TodoItem from './TodoItem';
import TodoInput from './TodoInput';

class TodoList extends React.Component {
    constructor() {
        super();
        // TODO: hardcoded items for now
        this.state = {
            items: [
                {name: "Item 1", done: false, itemKey: "1"},
                {name: "Item 2", done: true, itemKey: "2"},
                {name: "Item 3", done: false, itemKey: "3"}
            ]
        };
        this.handleAddItem = this.handleAddItem.bind(this);
        this.handleToggle = this.handleToggle.bind(this);
    }
    
    handleAddItem(name) {
        const newName = name.trim();
        if(newName.length > 0) {
            const itemKey = "" + (this.state.items.length+1);
            const newItems = this.state.items.concat({ name: newName, done: false, itemKey: itemKey});
            this.setState({ items: newItems });
        }
    }
    
    handleToggle(item) {
        const newItems = this.state.items.slice();
        newItems[newItems.indexOf(item)].done = !item.done;
        this.setState({ items: newItems });
    }

	render() {
        const handleToggle = this.handleToggle;
		return (
            <div>
                <ul>
					{ 
                        this.state.items.map(function(item) { 
                            return <TodoItem name={item.name} done={item.done} index={item.itemKey} key={item.itemKey} onToggle={function(){handleToggle(item)}}/>
                        }) 
                    }
                </ul>
            	<TodoInput onAddItem={this.handleAddItem}/>
            </div>
        );
	}

    componentDidMount() {
        this.apiPath = document.getElementById('TodoAppRoot').attributes['data-resource-path'].value;
        this.getapiPath = this.apiPath + ".model.json"
        fetch(this.getapiPath, {
            // Does the request work without this?  In Author?  In Publish?
            credentials: "same-origin",
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }}
        ).then((response) => response.json())
        .then(result => {
            // alert(result);
        },
        error => {
            // alert(error);
        });
    }
}

export default TodoList;