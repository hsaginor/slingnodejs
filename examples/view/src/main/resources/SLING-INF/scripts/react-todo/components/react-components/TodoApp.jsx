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
import React from 'react';;
import Header from './Header';
import TodoList from './TodoList';

class TodoApp extends React.Component {
	render() {
    	return (
            <div>
            <Header title={this.props.title} />
            	<p className="App-intro">To add item, type in item name and click Add.</p>
        		<TodoList items={this.props.items} apiPath={this.props.apiPath}/>
            </div>
        );
    }
}

export default TodoApp;