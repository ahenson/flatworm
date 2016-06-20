/*
 * Flatworm - A Java Flat File Importer/Exporter Copyright (C) 2004 James M. Turner.
 * Extended by James Lawrence 2005
 * Extended by Josh Brackett in 2011 and 2012
 * Extended by Alan Henson in 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.blackbear.flatworm.annotations;

import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.config.RecordDefinitionBO;
import com.blackbear.flatworm.config.ScriptletBO;
import com.blackbear.flatworm.config.impl.DefaultAnnotationConfigurationReaderImpl;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Shared functionality across the annotation tests.
 *
 * @author Alan Henson
 */
public class AbstractBaseAnnotationTest {

    protected DefaultAnnotationConfigurationReaderImpl configLoader;

    @Before
    public void setup() {
        configLoader = new DefaultAnnotationConfigurationReaderImpl();
    }
    
    public void validateFileFormat(FileFormat fileFormat, int expectedRecordCount, boolean expectedIgnoreUnmappedRecordsFlag) {
        assertNotNull("FileFormat instance is null.");
        assertEquals("Incorrect ignoreUnmappedRecordsFlag", expectedIgnoreUnmappedRecordsFlag, fileFormat.isIgnoreUnmappedRecords());
        assertNotNull("Null Records for FileFormat.", fileFormat.getRecords());
        assertEquals("Incorrect number of Records loaded to FileFormat.", expectedRecordCount, fileFormat.getRecords().size());
    }
    
    public void validateLines(RecordBO recordBO, int expectedLineCount) {
        assertNotNull("Null RecordDefinition instance.", recordBO.getRecordDefinition());
        assertNotNull("Null RecordDefinition.Lines.", recordBO.getRecordDefinition().getLines());
        assertFalse("Empty RecordDefinition.Lines.", recordBO.getRecordDefinition().getLines().isEmpty());
        assertEquals("RecordDefinition.line size is incorrect.", expectedLineCount, recordBO.getRecordDefinition().getLines().size());
    }
    
    public void validateLine(LineBO line, String expectedDelimiter, char expectedQuotChar) {
        assertNotNull("Null Line", line);
        assertEquals("Wrong quoteChar", expectedQuotChar, line.getQuoteChar());
        assertEquals("Wrong delimiter", expectedDelimiter, line.getDelimiter());
    }
    
    public void validateRecord(RecordBO record, Class<?> expectedClass) {
        validateRecord(record, expectedClass.getSimpleName());
    }
    
    public void validateRecord(RecordBO record, String expectedName) {
        assertNotNull(String.format("%s bean is null.", expectedName), record);
        assertFalse(String.format("%s.name was not loaded.", expectedName), StringUtils.isBlank(record.getName()));
        assertNotNull(String.format("%s.recordIdentity was not loaded.", record.getName()), record.getRecordIdentity());
        assertNotNull(String.format("%s.recordDefinition was not loaded.", record.getName()), record.getRecordDefinition());
    }

    public void validateRecordDefinition(RecordBO record, RecordDefinitionBO definition, int expectedLineCount) {
        assertNotNull(String.format("Null line collection for %s.recordDefinition.", record.getName()), definition.getLines());
        assertEquals(String.format("Incorrect number of lines loaded for %s.recordDefinition", record.getName()), 
                expectedLineCount, definition.getLines().size());
        assertNotNull(String.format("Null %s.parentRecord", record.getName()), definition.getParentRecord());
        assertTrue(String.format("Incorrect %s.parentRecord", record.getName()), record == definition.getParentRecord());
    }
    
    public void validateScriptlet(ScriptletBO scriptlet, String expectedEngineName, 
                                  String expectedScript, String expectedFunctionName, 
                                  String expectedScriptFile) {
        if (expectedEngineName != null) {
            assertNotNull("Null script engine name", scriptlet.getScriptEngineName());
            assertEquals("Wrong script engine name", expectedEngineName, scriptlet.getScriptEngineName());
        }

        if (expectedScript != null) {
            assertNotNull("Null script", scriptlet.getScript());
            assertEquals("Wrong script", expectedScript, scriptlet.getScript().trim());
        }

        if (expectedScriptFile != null) {
            assertNotNull("Null script file", scriptlet.getScriptFile());
            assertEquals("Wrong script", expectedScriptFile, scriptlet.getScriptFile());
        }

        if (expectedFunctionName != null) {
            assertNotNull("Null script function", scriptlet.getFunctionName());
            assertEquals("Wrong script function name", expectedFunctionName, scriptlet.getFunctionName());
        }
    }
    
}
