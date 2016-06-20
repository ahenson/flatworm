//@ sourceURL=flatworm-core/src/test/resources/before_after_script_test.js

var modifyRecord = function (fileFormat, line) {
    with(new JavaImporter('com.blackbear.flatworm.config')) {
        
        var record = fileFormat.getRecords().get(1);
        var recordDef = record.getRecordDefinition();
        var line = recordDef.getLines().get(0);
    
        record.getRecordIdentity().setMinLength(28);
        
        line.getLineElements().get(0).setFieldLength(14);
        line.getLineElements().get(1).setFieldLength(14);
    }
}