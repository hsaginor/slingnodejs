"use strict";
var fs = require('fs');

class Hello {
	render() {
		var hello = fs.readFileSync(__dirname + "/simple.txt", "utf8");
		// console.log("Loaded file " + __dirname + "/simple.txt");
		// console.log(hello);
		return hello;
    }
}

module.exports = new Hello();
