# slingnodejs

Apache Sling NodeJS Scripting Bridge using J2V8

This project is intended to provide a way to build isomorphic JavaScript applications on top of Apache Sling, rendered natively in NodeJS.  

Currently only server side rendering is implemented. Any JavaScript that can execute in NodeJS can render markup with Sling Content as long as it is installed in Apache Sling repository, and follows Apache Sling path and script resolution rules. The only requirement is that script needs to export an object with a render() method. In theory, it should be possible to statically include a pre-compiled JavaScript bundle in server side generated markup to provide client side rendering.    

## Installation requirements

A running Apache Sling or Adobe AEM service. 

## Installation Instructions 

1. Download (or clone this project) and install one of the pre-compiled J2V8 OSGi bundles from the [dependencies](./dependencies) folder via Felix Console.
2. Whitelist org.apache.sling.scripting.nodejs bundle of administrative login. 
	* Open configuration manager page in Felix Console (http://localhost:4502/system/console/configMgr on default AEM author port).
	*  Find Apache Sling Login Whitelist Configuration Fragment and click + icon.
	* Enter nodejs as Name and "org.apache.sling.scripting.nodejs" as Whitelisted BSNs. Then click Save
3. Clone this project locally and from the root project folder run the following maven command:
	> mvn clean install -PautoInstallBundle -Dsling.port=4502 

The above command will install the project OSGi bundle to AEM Author instance on default port 4502. Without sling.port parameter the build will try to install it on the default Sling port 8000 




