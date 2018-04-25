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
    constructor(props) {
        super();
        this.state = {
            items: props.items
        };
        this.apiPath = props.apiPath;
        this.handleAddItem = this.handleAddItem.bind(this);
        this.handleToggle = this.handleToggle.bind(this);
    }
    
    handleAddItem(name) {
        const newName = name.trim();
        if(newName.length > 0) {
            const itemKey = "item" + (this.state.items.length+1);
            const item = { name: newName, done: false, itemKey: itemKey }
            this.postItem(item, response => { this.setState({ items: this.state.items.concat(item) }); });
        }
    }
    
    handleToggle(item) {
        const newItems = this.state.items.slice();
        const toggledItem = newItems[this.state.items.indexOf(item)];
        toggledItem.done = !toggledItem.done;
        this.postItem(toggledItem, response => { this.setState({ items: newItems }); });
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
    
    postItem(item, onSuccess, onError) {
    		const postUrl = this.apiPath + "/items/" + item.itemKey;
    		
    		fetch(postUrl, {
        		credentials: "same-origin",
        		method: 'POST',
        		headers: {
                'Accept': 'application/json, text/plain, text/html',
  				'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8'
        		},
        		body: 'jcr:primaryType=nt:unstructured&name='+item.name+'&done='+item.done+'&done@TypeHint=boolean',
        }).then(response => {
        		if(response.ok) {
        			onSuccess(response);
        		} else {
        			alert("Unable to save item '" + item.name + "': " + response.statusText);
        			console.error("Unable to save item '" + item.name + "': Response " + response.status);
        		}      	
        }).catch(err => {
        		alert("Unable to save item '" + item.name + "':" + err);
        		console.error(err);
        });
    }
}

export default TodoList;