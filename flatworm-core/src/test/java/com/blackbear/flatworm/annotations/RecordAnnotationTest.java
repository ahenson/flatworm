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
import com.blackbear.flatworm.MatchedRecord;
import com.blackbear.flatworm.annotations.beans.ConverterBean;
import com.blackbear.flatworm.annotations.beans.FieldIdentityBean;
import com.blackbear.flatworm.annotations.beans.LengthIdentityBean;
import com.blackbear.flatworm.annotations.beans.LineBean;
import com.blackbear.flatworm.annotations.beans.Pet;
import com.blackbear.flatworm.annotations.beans.PetOwner;
import com.blackbear.flatworm.annotations.beans.RecordBeanWithChildLine;
import com.blackbear.flatworm.annotations.beans.RecordBeanWithPropertiesOfSameType;
import com.blackbear.flatworm.annotations.beans.ScriptIdentityBean;
import com.blackbear.flatworm.annotations.beans.ScriptIdentityFileBean;
import com.blackbear.flatworm.config.ConverterBO;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.config.RecordDefinitionBO;
import com.blackbear.flatworm.config.RecordElementBO;
import com.blackbear.flatworm.config.ScriptletBO;
import com.blackbear.flatworm.config.impl.FieldIdentityImpl;
import com.blackbear.flatworm.config.impl.LengthIdentityImpl;
import com.blackbear.flatworm.config.impl.ScriptIdentityImpl;
import com.blackbear.flatworm.converters.ConversionHelper;
import com.blackbear.flatworm.converters.CoreConverters;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to parse out configured Converters: " + e.getMessage());
        }
    }

    @Test
    public void lineTest() {
        try {
            FileFormat fileFormat = configLoader.loadConfiguration(LineBean.class);
            validateFileFormat(fileFormat, 1, false);

            RecordBO record = fileFormat.getRecord("LineBean");
            validateRecord(record, LineBean.class, true);

            RecordDefinitionBO definition = record.getRecordDefinition();
            validateRecordDefinition(record, 1, 0);

            LineBO line = definition.getLines().get(0);
            assertEquals("Invalid line index.", 1, line.getIndex());
            assertEquals("Invalid line delimiter.", "|", line.getDelimiter());
            assertEquals("Invalid line quote char.", '"', line.getQuoteChar());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to parse out configured Lines: " + e.getMessage());
        }
    }

    @Test
    public void lineIdentityTest() {
        try {
            FileFormat fileFormat = configLoader.loadConfiguration(RecordBeanWithChildLine.class);
            validateFileFormat(fileFormat, 1, false);

            RecordBO record = fileFormat.getRecord(RecordBeanWithChildLine.class.getSimpleName());
            validateRecord(record, RecordBeanWithChildLine.class, true);

            validateRecordDefinition(record, 1, 2);

            // Test RecordBeanWithChildLine.class.
            LineBO line = record.getRecordDefinition().getLines().get(0);
            assertEquals("Invalid number of RecordElements loaded for Line 0.", 2, line.getLineElements().size());
            assertEquals("Invalid number of RecordElements loaded for Line 0.", 2L,
                    line.getLineElements().stream().filter(value -> value instanceof RecordElementBO).count());

            // Test RecordBeanTheChildLine.class.
            line = record.getRecordDefinition().getLinesWithIdentities().get(1);
            assertEquals("Invalid number of RecordElements loaded for Line 1.", 2, line.getLineElements().size());
            assertEquals("Invalid number of RecordElements loaded for Line 1.", 2L,
                    line.getLineElements().stream().filter(value -> value instanceof RecordElementBO).count());

            // Test RecordBeanTheChildOfChildLine.class.
            line = record.getRecordDefinition().getLinesWithIdentities().get(1);
            assertEquals("Invalid number of RecordElements loaded for Line 2.", 2, line.getLineElements().size());
            assertEquals("Invalid number of RecordElements loaded for Line 2.", 2L,
                    line.getLineElements().stream().filter(value -> value instanceof RecordElementBO).count());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed on Line Identity Test: " + e.getMessage());
        }
    }

    @Test
    public void lengthIdentityTest() {
        try {

            assertTrue("No Record annotation present.", LengthIdentityBean.class.isAnnotationPresent(Record.class));
            RecordBO record = configLoader.loadRecord(LengthIdentityBean.class.getAnnotation(Record.class));

            validateRecord(record, LengthIdentityBean.class, true);
            validateLines(record.getRecordDefinition(), 1, 0);
            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');

            assertTrue("The Length Identity information was not loaded.", record.getRecordIdentity() instanceof LengthIdentityImpl);
            LengthIdentityImpl identity = LengthIdentityImpl.class.cast(record.getRecordIdentity());

            assertNotNull("Null min length", identity.getMinLength());
            assertEquals("Wrong min length value", 0, identity.getMinLength().intValue());

            assertNotNull("Null max length", identity.getMaxLength());
            assertEquals("Wrong max length value", 3, identity.getMaxLength().intValue());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }

    @Test
    public void fieldIdentityTest() {
        try {
            assertTrue("No Record annotation present.", LengthIdentityBean.class.isAnnotationPresent(Record.class));
            RecordBO record = configLoader.loadRecord(FieldIdentityBean.class.getAnnotation(Record.class));

            validateRecord(record, FieldIdentityBean.class, true);
            validateLines(record.getRecordDefinition(), 1, 0);
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
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }

    @Test
    public void scriptIdentityTest() {
        try {
            assertTrue("No Record annotation present.", LengthIdentityBean.class.isAnnotationPresent(Record.class));
            RecordBO record = configLoader.loadRecord(ScriptIdentityBean.class.getAnnotation(Record.class));

            validateRecord(record, ScriptIdentityBean.class, true);
            validateLines(record.getRecordDefinition(), 1, 0);
            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');

            assertTrue("The Length Identity information was not loaded.", record.getRecordIdentity() instanceof ScriptIdentityImpl);

            ScriptIdentityImpl identity = ScriptIdentityImpl.class.cast(record.getRecordIdentity());
            assertTrue("Failed to match identifier", identity.matchesIdentity(record, new FileFormat(), ""));

            ScriptletBO scriptlet = identity.getScriptlet();
            validateScriptlet(scriptlet, "nashorn", "function myFunction(fileFormat, line) { return true; }", "myFunction", null);

            scriptlet = record.getBeforeScriptlet();
            validateScriptlet(scriptlet, "nashorn", "function myFunction(fileFormat, line) { return true; }", "myFunction", null);

            scriptlet = record.getAfterScriptlet();
            validateScriptlet(scriptlet, "nashorn", "function myFunction(fileFormat) { return true; }", "myFunction", null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }

    @Test
    public void scriptIdentityScriptFileTest() {
        try {
            assertTrue("No Record annotation present.", LengthIdentityBean.class.isAnnotationPresent(Record.class));
            RecordBO record = configLoader.loadRecord(ScriptIdentityFileBean.class.getAnnotation(Record.class));

            validateRecord(record, ScriptIdentityFileBean.class, true);
            validateLines(record.getRecordDefinition(), 1, 0);
            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');

            assertTrue("The Length Identity information was not loaded.", record.getRecordIdentity() instanceof ScriptIdentityImpl);
            ScriptIdentityImpl identity = ScriptIdentityImpl.class.cast(record.getRecordIdentity());
            assertTrue("Failed to match identifier", identity.matchesIdentity(record, new FileFormat(), ""));

            ScriptletBO scriptlet = identity.getScriptlet();
            validateScriptlet(scriptlet, "nashorn", "var myFunction = function (fileFormat, line) { return true; };", "myFunction",
                    "script_identity_script_file.js");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse configuration from object with RecordBO annotation: " + e.getMessage());
        }
    }

    @Test
    public void contaminationTest() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("same_property_types.txt")) {
            configLoader.setPerformValidation(true);

            assertTrue("No Record annotation present.", RecordBeanWithPropertiesOfSameType.class.isAnnotationPresent(Record.class));
            FileFormat fileFormat = configLoader.loadConfiguration(RecordBeanWithPropertiesOfSameType.class);

            validateFileFormat(fileFormat, 1, false);

            RecordBO record = fileFormat.getRecord(RecordBeanWithPropertiesOfSameType.class.getSimpleName());

            validateRecord(record, RecordBeanWithPropertiesOfSameType.class, false);
            validateLines(record.getRecordDefinition(), 0, 2);

            validateLine(record.getRecordDefinition().getLinesWithIdentities().get(0), "", '\0');
            validateLine(record.getRecordDefinition().getLinesWithIdentities().get(1), "", '\0');

            // ---------------------------------------------------------
            // Load the first record
            // ---------------------------------------------------------
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
            MatchedRecord matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Null MatchedRecord returned when not expected.", matchedRecord);
            assertEquals("Incorrect record name.", RecordBeanWithPropertiesOfSameType.class.getSimpleName(), matchedRecord.getRecordName());

            Object bean = matchedRecord.getBean(RecordBeanWithPropertiesOfSameType.class.getName());
            assertNotNull("Failed to load RecordBeanWithPropertiesOfSameType.", bean);
            assertTrue("Loaded RecordBeanWithPropertiesOfSameType, but wrong Class.", bean instanceof RecordBeanWithPropertiesOfSameType);

            RecordBeanWithPropertiesOfSameType testBean = RecordBeanWithPropertiesOfSameType.class.cast(bean);

            assertTrue("Null or empty dogs list.", testBean.getDogs() != null && !testBean.getDogs().isEmpty());
            assertEquals("Incorrect number of dogs loaded.", 1, testBean.getDogs().size());
            Pet dog = testBean.getDogs().get(0);
            assertEquals("Dog has wrong name.", "spot", dog.getName());
            assertEquals("Dog has wrong age.", 6, dog.getAge().intValue());
            assertEquals("Dog has wrong favorite toy.", "tennis ball", dog.getFavoriteToy());
            assertEquals("Dog has wrong allergies.", "chicken", dog.getAllergies());

            assertTrue("Null or empty cats list.", testBean.getCats() != null && !testBean.getCats().isEmpty());
            assertEquals("Incorrect number of cats loaded.", 1, testBean.getCats().size());

            Pet cat = testBean.getCats().get(0);
            assertEquals("Cat has wrong name.", "whiskers", cat.getName());
            assertEquals("Cat has wrong age.", 3, cat.getAge().intValue());
            assertEquals("Cat has wrong favorite toy.", "yarn", cat.getFavoriteToy());
            assertEquals("Cat has wrong allergies.", "", cat.getAllergies());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse RecordBeanWithPropertiesOfSameType: " + e.getMessage());
        }
    }

    @Test
    public void endRecordTest() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("end_record_test.txt")) {
            configLoader.setPerformValidation(true);

            assertTrue("No Record annotation present.", PetOwner.class.isAnnotationPresent(Record.class));
            FileFormat fileFormat = configLoader.loadConfiguration(PetOwner.class);

            validateFileFormat(fileFormat, 1, false);

            RecordBO record = fileFormat.getRecord(PetOwner.class.getSimpleName());

            validateRecord(record, PetOwner.class, false);
            validateLines(record.getRecordDefinition(), 1, 3);

            validateLine(record.getRecordDefinition().getLines().get(0), "", '\0');
            validateLine(record.getRecordDefinition().getLinesWithIdentities().get(0), "", '\0');
            validateLine(record.getRecordDefinition().getLinesWithIdentities().get(1), "", '\0');
            validateLine(record.getRecordDefinition().getLinesWithIdentities().get(2), "", '\0');

            // ---------------------------------------------------------
            // Load the first record
            // ---------------------------------------------------------
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
            MatchedRecord matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Null MatchedRecord returned when not expected.", matchedRecord);
            assertEquals("Incorrect record name.", PetOwner.class.getSimpleName(), matchedRecord.getRecordName());

            Object bean = matchedRecord.getBean(PetOwner.class.getName());
            assertNotNull("Failed to load PetOwner.", bean);
            assertTrue("Loaded PetOwner, but wrong Class.", bean instanceof PetOwner);

            PetOwner petOwner = PetOwner.class.cast(bean);
            assertEquals("Pet Owner 1 -> Wrong first name.", "Jane", petOwner.getFirstName());
            assertEquals("Pet Owner 1 -> Wrong last name.", "Smith", petOwner.getLastName());
            assertEquals("Pet Owner 1 -> Wrong city.", "New York", petOwner.getCity());

            assertTrue("Pet Owner 1 -> Null or empty dogs list.", petOwner.getDogs() != null && !petOwner.getDogs().isEmpty());
            assertEquals("Pet Owner 1 -> Incorrect number of dogs loaded.", 1, petOwner.getDogs().size());
            
            Pet dog = petOwner.getDogs().get(0);
            assertEquals("Pet Owner 1 -> Dog has wrong name.", "spot", dog.getName());
            assertEquals("Pet Owner 1 -> Dog has wrong age.", 6, dog.getAge().intValue());
            assertEquals("Pet Owner 1 -> Dog has wrong favorite toy.", "tennis ball", dog.getFavoriteToy());
            assertEquals("Pet Owner 1 -> Dog has wrong allergies.", "chicken", dog.getAllergies());

            assertTrue("Pet Owner 1 -> Null or empty cats list.", petOwner.getCats() != null && !petOwner.getCats().isEmpty());
            assertEquals("Pet Owner 1 -> Incorrect number of cats loaded.", 2, petOwner.getCats().size());

            Pet cat = petOwner.getCats().get(0);
            assertEquals("Pet Owner 1 -> Cat 1 has wrong name.", "whiskers", cat.getName());
            assertEquals("Pet Owner 1 -> Cat 1 has wrong age.", 3, cat.getAge().intValue());
            assertEquals("Pet Owner 1 -> Cat 1 has wrong favorite toy.", "yarn", cat.getFavoriteToy());
            assertEquals("Pet Owner 1 -> Cat 1 has wrong allergies.", "", cat.getAllergies());

            cat = petOwner.getCats().get(1);
            assertEquals("Pet Owner 1 -> Cat 2 has wrong name.", "fluffy", cat.getName());
            assertEquals("Pet Owner 1 -> Cat 2 has wrong age.", 1, cat.getAge().intValue());
            assertEquals("Pet Owner 1 -> Cat 2 has wrong favorite toy.", "rat toy", cat.getFavoriteToy());
            assertEquals("Pet Owner 1 -> Cat 2 has wrong allergies.", "veggies", cat.getAllergies());

            matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Pet Owner 2 -> Null MatchedRecord returned when not expected.", matchedRecord);
            assertEquals("Pet Owner 2 -> Incorrect record name.", PetOwner.class.getSimpleName(), matchedRecord.getRecordName());

            bean = matchedRecord.getBean(PetOwner.class.getName());
            assertNotNull("Pet Owner 2 -> Failed to load PetOwner.", bean);
            assertTrue("Pet Owner 2 -> Loaded PetOwner, but wrong Class.", bean instanceof PetOwner);

            petOwner = PetOwner.class.cast(bean);
            assertEquals("Pet Owner 2 -> Wrong first name.", "Piere", petOwner.getFirstName());
            assertEquals("Pet Owner 2 -> Wrong last name.", "Bordeaux", petOwner.getLastName());
            assertEquals("Pet Owner 2 -> Wrong city.", "Paris", petOwner.getCity());

            assertTrue("Pet Owner 2 -> Null or empty dogs list.", petOwner.getDogs() != null && !petOwner.getDogs().isEmpty());
            assertEquals("Pet Owner 2 -> Incorrect number of dogs loaded.", 2, petOwner.getDogs().size());
            
            dog = petOwner.getDogs().get(0);
            assertEquals("Pet Owner 2 -> Dog 1 has wrong name.", "max", dog.getName());
            assertEquals("Pet Owner 2 -> Dog 1 has wrong age.", 5, dog.getAge().intValue());
            assertEquals("Pet Owner 2 -> Dog 1 has wrong favorite toy.", "tug rope", dog.getFavoriteToy());
            assertEquals("Pet Owner 2 -> Dog 1 has wrong allergies.", "cats", dog.getAllergies());

            dog = petOwner.getDogs().get(1);
            assertEquals("Pet Owner 2 -> Dog 2 has wrong name.", "sir bob", dog.getName());
            assertEquals("Pet Owner 2 -> Dog 2 has wrong age.", 4, dog.getAge().intValue());
            assertEquals("Pet Owner 2 -> Dog 2 has wrong favorite toy.", "frisbee", dog.getFavoriteToy());
            assertEquals("Pet Owner 2 -> Dog 2 has wrong allergies.", "beef", dog.getAllergies());

            assertTrue("Pet Owner 2 -> Should not have cats.", petOwner.getCats() == null || petOwner.getCats().isEmpty());
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to correctly parse data with end-record flags set: " + e.getMessage());
        }
    }
}
