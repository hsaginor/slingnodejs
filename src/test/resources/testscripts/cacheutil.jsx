var fs = require('fs');

module.exports = {  
    delete(path) { delete require.cache[path]; },
    getcache(path) { return require.cache[path]; },
    watchcache(path) {
        
        fs.watch(path, { encoding: 'buffer' }, function(eventType, filename) {
            console.log(`event type is: ${eventType}`);
            if(filename) {
                var fullPath = __dirname + "/" + filename
                delete(fullPath);
                console.log(eventType + " -> " + fullPath);
            } else {
                console.log('filename not provided');
            }
        });
        console.log("Watching " + path);
        
    }
}