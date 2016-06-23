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
import com.blackbear.flatworm.annotations.beans.HierarchyHeader;
import com.blackbear.flatworm.annotations.beans.HierarchyOne;
import com.blackbear.flatworm.annotations.beans.HierarchyThreeParentTwo;
import com.blackbear.flatworm.annotations.beans.HierarchyTwoParentOne;
import com.blackbear.flatworm.config.RecordBO;

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
 * Class description goes here.
 *
 * @author Alan Henson
 */
public class AnnotationHierarchyTest extends AbstractBaseAnnotationTest {
    
    @Test
    public void hierarchyTest() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("hierarchy_input.txt")) {
            configLoader.setPerformValidation(true);
            FileFormat fileFormat = configLoader.loadConfiguration(HierarchyHeader.class, HierarchyOne.class);

            validateFileFormat(fileFormat, 2, false);
            RecordBO recordOne = fileFormat.getRecords().get(0);
            validateRecord(recordOne, HierarchyHeader.class, true);
            validateRecordDefinition(recordOne, 1, 0);
            
            RecordBO recordTwo = fileFormat.getRecords().get(1);
            validateRecord(recordTwo, HierarchyOne.class, false);
            validateRecordDefinition(recordTwo, 0, 2);
            
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
            
            // ---------------------------------------------------------
            // Load the first record
            // ---------------------------------------------------------
            MatchedRecord matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Null MatchedRecord returned when not expected.", matchedRecord);
            assertEquals("HierarchyHeader", matchedRecord.getRecordName());

            Object bean = matchedRecord.getBean(HierarchyHeader.class.getName());
            assertNotNull("Failed to load HierarchyHeader.", bean);
            assertTrue("Loaded HierarchyHeader, but wrong Class.", bean instanceof HierarchyHeader);

            HierarchyHeader header = HierarchyHeader.class.cast(bean);
            assertEquals("HierarchyHeader.recordType value is incorrect", "Hierarchy", header.getRecordType());
            assertEquals("HierarchyHeader.recordName value is incorrect", "RecordOne", header.getRecordName());
            
            // ---------------------------------------------------------
            // Load the second record
            // ---------------------------------------------------------
            matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Null MatchedRecord returned when not expected.", matchedRecord);

            bean = matchedRecord.getBean(HierarchyOne.class.getName());
            assertNotNull("Failed to load HierarchyOne.", bean);
            assertTrue("Loaded HierarchyHeader, but wrong Class.", bean instanceof HierarchyOne);

            HierarchyOne one = HierarchyOne.class.cast(bean);
            assertFalse("HierarchyOne has an empty child list.", one.getChildList().isEmpty());
            assertEquals("HierarchyOne has incorrect number of children.",  1, one.getChildList().size());

            HierarchyTwoParentOne two = one.getChildList().get(0);
            assertEquals("HierarchyTwoParentOne.propValue is incorrect.", "123", two.getPropValue());
            assertNotNull("HierarchyTwoParentOne.childThree is null.", two.getChildThree());

            HierarchyThreeParentTwo three = two.getChildThree();
            assertEquals("HierarchyThreeParentTwo.propOne is incorrect.", "PropOne", three.getPropOne());
            assertEquals("HierarchyThreeParentTwo.propTwo is incorrect.", "PropTwo", three.getPropTwo());
            
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to parse the data from annotation_input.txt: " + e.getMessage());
        }

    }
}
