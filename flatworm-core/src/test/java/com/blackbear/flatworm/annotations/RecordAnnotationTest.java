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
import com.blackbear.flatworm.annotations.beans.ConverterBean;
import com.blackbear.flatworm.annotations.beans.FieldIdentityBean;
import com.blackbear.flatworm.annotations.beans.LengthIdentityBean;
import com.blackbear.flatworm.annotations.beans.LineBean;
import com.blackbear.flatworm.annotations.beans.ScriptIdentityBean;
import com.blackbear.flatworm.config.ConverterBO;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.config.RecordDefinitionBO;
import com.blackbear.flatworm.config.impl.FieldIdentityImpl;
import com.blackbear.flatworm.config.impl.LengthIdentityImpl;
import com.blackbear.flatworm.config.impl.ScriptIdentityImpl;
import com.blackbear.flatworm.converters.ConversionHelper;
import com.blackbear.flatworm.converters.CoreConverters;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the parsing of RecordBO-based annotations.
 *
 * @author Alan Henson
 */
public class RecordAnnotationTest extends AbstractBaseAnnotationTest {

    @Before
    public void setup() {
        super.setup();
        configLoader.setPerformValidation(false);
    }
    
    @Test
    public void converterTest() {
        try {
            FileFormat fileFormat = configLoader.loadConfiguration(ConverterBean.class);
            assertNotNull("Failed to load FileFormat instance from annotation configuration.", fileFormat);
            assertNotNull("Null fileFormat.conversionHelper", fileFormat.getConversionHelper());

            ConversionHelper helper = fileFormat.getConversionHelper();
            assertNotNull("Null collection of converters.", helper.getConverters());
            assertEquals("Invalid number of converters loaded.", 1, helper.getConverters().size());
            
            ConverterBO converter = helper.getConverter("test");
            assertNotNull("Converter not resolved by name.", converter);
            assertEquals("Invalid converter classname.", CoreConverters.class.getName(), converter.getConverterClass());
            assertEquals("Invalid converter method.", "convertChar", converter.getMethod());
            assertEquals("Invalid converter return-type.", String.class.getName(), converter.getReturnType());
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Failed to parse out configured Converters: " + e.getMessage());
        }
    }
    
    @Test
    public void lineTest() {
        try {
            FileFormat fileFormat = configLoader.loadConfiguration(LineBean.class);
            assertNotNull("Failed to load FileFormat instance from annotation configuration.", fileFormat);
            assertNotNull("Null fileFormat.records", fileFormat.getRecords());
            
            RecordBO record = fileFormat.getRecord("LineBean");
            validateRecord(record, LineBean.class);

            RecordDefinitionBO definition = record.getRecordDefinition();
            validateRecordDefinition(record, definition, 1);

            LineBO line = definition.getLines().get(0);
            assertEquals("Invalid line ID.", "line", line.getId());
            assertEquals("Invalid line delimiter.", "|", line.getDelimiter());
            assertEquals("Invalid line quote char.", '"', line.getQuoteChar());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to parse out configured Lines: " + e.getMessage());
        }
    }
    
    @Test
    public void lengthIdentityTest() {
        try {
            RecordBO record = configLoader.loadRecord(LengthIdentityBean.class);

            validateRecord(record, LengthIdentityBean.class);
            validateLines(record, 1);
            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');

            assertTrue("The Length Identity information was not loaded.", record.getRecordIdentity() instanceof LengthIdentityImpl);
            LengthIdentityImpl identity = LengthIdentityImpl.class.cast(record.getRecordIdentity());

            assertNotNull("Null min length", identity.getMinLength());
            assertEquals("Wrong min length value", 0, identity.getMinLength().intValue());

            assertNotNull("Null max length", identity.getMaxLength());
            assertEquals("Wrong max length value", 3, identity.getMaxLength().intValue());
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }

    @Test
    public void fieldIdentityTest() {
        try {
            RecordBO record = configLoader.loadRecord(FieldIdentityBean.class);

            validateRecord(record, FieldIdentityBean.class);
            validateLines(record, 1);
            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');

            assertTrue("The Field Identity information was not loaded.", record.getRecordIdentity() instanceof FieldIdentityImpl);
            FieldIdentityImpl identity = FieldIdentityImpl.class.cast(record.getRecordIdentity());

            assertNotNull("Null start position", identity.getStartPosition());
            assertEquals("Wrong start position value", 0, identity.getStartPosition().intValue());

            assertNotNull("Null field length", identity.getFieldLength());
            assertEquals("Wrong field length value", 3, identity.getFieldLength().intValue());

            assertTrue("The ignore-case flag was not properly parsed.", identity.isIgnoreCase());

            assertFalse("No match identities were loaded.", identity.getMatchingStrings().isEmpty());
            assertTrue("Match strings were not loaded.", identity.matchesIdentity("FLD"));
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }

    @Test
    public void scriptIdentityTest() {
        try {
            RecordBO record = configLoader.loadRecord(ScriptIdentityBean.class);

            validateRecord(record, ScriptIdentityBean.class);
            validateLines(record, 1);
            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');

            assertTrue("The Length Identity information was not loaded.", record.getRecordIdentity() instanceof ScriptIdentityImpl);
            ScriptIdentityImpl identity = ScriptIdentityImpl.class.cast(record.getRecordIdentity());

            assertNotNull("Null script engine name", identity.getScriptEngineName());
            assertEquals("Wrong script engine name", "nashorn", identity.getScriptEngineName());

            assertNotNull("Null script", identity.getScriptEngineName());
            assertEquals("Wrong script", "function myMethod(fileFormat, line) { return true; }", identity.getScript());

            assertNotNull("Null script method", identity.getMethodName());
            assertEquals("Wrong script method name", "myMethod", identity.getMethodName());
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }
}
