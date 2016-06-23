var matchHeader = function(fileFormat, line) { 
    return fileFormat.getLineNumber() == 1
        || fileFormat.getCurrentParsedLine().toUpperCase().startsWith("HIERARCHY"); 
}