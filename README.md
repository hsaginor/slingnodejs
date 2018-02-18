# slingnodejs

Apache Sling NodeJS Scripting Bridge using J2V8

This project is intended to provide a way to build isomorphic JavaScript applications on top of Apache Sling, rendered natively in NodeJS.  

A script must end in a .jsx extension and export an object with a *renderServerResponse()* method. The *renderServerResponse()* method can return generated content as string which will be written to response, or write directly to response via *out* variable. 

Currently only server side rendering is implemented. However in theory it should be possible to statically include a pre-compiled JavaScript bundle in server side generated markup to provide client side rendering.

What else is not yet implemented? A simple way to include other resources and resource types as with <cq:include> or <sling:include> JSP tags.
    
## Installation Requirements

A running Apache Sling or Adobe AEM server. 

## Installation Instructions 

1. Download (or clone this project) and install one of the pre-compiled J2V8 OSGi bundles from the [dependencies](./dependencies) folder via Felix Console.
2. Whitelist org.apache.sling.scripting.nodejs bundle for administrative login. 
	* Open configuration manager page in Felix Console (http://localhost:4502/system/console/configMgr on default AEM author port).
	* Find Apache Sling Login Whitelist Configuration Fragment and click + icon (may not be required on older versions of Sling/AEM).
	* Enter nodejs as Name and org.apache.sling.scripting.nodejs as Whitelisted BSNs. Then click Save.
3. Clone this project locally and from the root project folder run the following maven command:
	> mvn clean install -PautoInstallBundle -Dsling.port=4502 

The above command will install an OSGi bundle to AEM Author instance on default port 4502. Without sling.port parameter the build will try to install it on the default Sling port 8000. 

## Configuration 

Place files *.babelrc* and *package.json* at the root of Sling JCR repository.

### .babelrc
```javascript
{
  "presets" : [ "es2015", "react"]
}
```

### package.json
 ```javascript
{
  "name": "sling-nodejs",
  "version": "1.0.0",
  "description": "Sling NodeJs apps.",
  "scripts": {
    "build": "babel src -d out"
  },
  "author": "",
  "dependencies": {
    "react": "^16.2.0",
    "react-dom": "^16.2.0"
  },
  "devDependencies": {
    "babel-cli": "^6.26.0",
    "babel-core": "^6.26.0",
    "babel-loader": "^7.1.2",
    "babel-preset-env": "^1.6.1",
    "babel-preset-es2015": "^6.24.1",
    "babel-preset-react": "^6.24.1"
  }
}
 ```
 
## Verify Installation

After installing the OSGi bundles go to Felix Console and navigate to OSGi -> Bundles. Verify that org.apache.sling.scripting.nodejs bundles is active.

You should also see a new menu item in Felix Console named Sling NodeJS. Navigate to Sling NodeJS -> Sling NodeJS Version. Verify node version. If you installed J2V8 bundle from this project (Installation Instruction above) node version should be 7.4.0.
   
## Server Side Rendering Objects

The following variables are available to scripts for server side rendering.

* sling - global sling variable as defined by [SlingScriptHelper](https://sling.apache.org/apidocs/sling9/org/apache/sling/api/scripting/SlingScriptHelper.html) interface.
* out - global scripting variable providing access to java.io.PrintWriter object for writing to response.  
* request - current request object defined by [SlingHttpServletRequest](https://sling.apache.org/apidocs/sling9/org/apache/sling/api/SlingHttpServletRequest.html) interface. 
* response - current request object defined by [SlingHttpServletResponse](https://sling.apache.org/apidocs/sling9/org/apache/sling/api/SlingHttpServletResponse.html) interface. 
* resolver - current ResourceResolver. This is the same object as returned by request.getResourceResolver() method.  
* resource - current resource. This is the same object as returned by request.getResource() method.
* properties - current resource properties or the [ValueMap](https://sling.apache.org/apidocs/sling9/org/apache/sling/api/resource/ValueMap.html) for the current resource.
* jcrSession - current JCR session.
* node - current JCR node.
* log - server side log object.

## Simple Script Example

Bellow is a simple script that demonstrates how some of the server side objects can be used to render Sling content.

```javascript
"use strict";

class ReadresourceTest {
	renderServerResponse() {
        var myPath = resource.getPath();
        var title = properties.get("title", "Read Resource Test");
        var n = properties.get("number", 15);
        log.info("Displaying path {}!", myPath);
		var text = "<html><head><title>Read Resource.</title></head><body><h1>" 
        			+ title 
                    + "</h1>Displaying Resource: " 
                    + myPath + "<br/>" 
                    + "Number: " + n
                    + "</body></html>";
        log.info(text);

        return text;
    }
}

module.exports = new ReadresourceTest();
```

To test this do the following:
1. Create a file in Sling (or AEM) /apps/nodetest/components/readresourceTest/readresourceTest.jsx
2. Copy/paste the code above into this file and save.
3. Create a JCR node somewhere in the repository and set it's sling:resourceType property to nodetest/components/readresourceTest.
4. Try to access the path you just created with .html extension on your Sling instance in a browser.
5. Add "title" property of type String and "number" property of type Long.
6. Change these properties and refresh the browser several times to see how rendered page changes.    

## React JSX Example

Bellow is a simple script that does the same thing as previous example using React with JSX syntax.

```javascript
import React from 'react';
import ReactDOMServer from 'react-dom/server';

class ResourceTest extends React.Component {
	render() {
        const myPath = resource.getPath();
        const title = properties.get("title", "Read Resource Test");
        const n = properties.get("number", 15);
        log.info("Displaying path {}!", myPath);
        return ( 
            <html>
            	<head><title>Read Resource {myPath}.</title></head>
            	<body>
            		<h1>{title}</h1>
            		Displaying Resource: {myPath}<br/>
            		Title Property: {title}<br/>
            		Number Property: {n}<br/>
            	</body>
			</html> 
        );
    }

    renderServerResponse() {
    		const text = ReactDOMServer.renderToString(this.render());
		out.write(text);
        out.flush();
    }
}
```

Note that unlike the previous example *renderServerResponse()* here does not return anything and writes output directly to the response via *out* variable. 

To test this do the following:
1. Create a file in Sling (or AEM) /apps/nodetest/components/resourceTest/resourceTest.jsx
2. Copy/paste the code above into this file and save.
3. Create a JCR node somewhere in the repository and set it's sling:resourceType property to nodetest/components/resourceTest.
4. Try to access the path you just created with .html extension on your Sling instance in a browser.
5. Add "title" property of type String and "number" property of type Long.
6. Change these properties and refresh the browser several times to see how rendered page changes.