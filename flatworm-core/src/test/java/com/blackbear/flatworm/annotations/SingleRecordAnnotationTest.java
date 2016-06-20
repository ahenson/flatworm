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
import com.blackbear.flatworm.annotations.beans.RecordBeanFiveChildToOne;
import com.blackbear.flatworm.annotations.beans.RecordBeanFourChildToTwo;
import com.blackbear.flatworm.annotations.beans.RecordBeanOne;
import com.blackbear.flatworm.annotations.beans.RecordBeanSix;
import com.blackbear.flatworm.annotations.beans.RecordBeanThreeChildToTwo;
import com.blackbear.flatworm.annotations.beans.RecordBeanTwoChildToOne;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Performs in-depth testing of the annotation framework to ensure that it completely mimics what's capable via the XML configuration.
 *
 * @author Alan Henson
 */
public class SingleRecordAnnotationTest extends AbstractBaseAnnotationTest {

    @Test
    public void singleRecordAnnotationValueTest() {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("annotation_input.txt")) {
            FileFormat fileFormat = configLoader.loadConfiguration(RecordBeanOne.class, RecordBeanSix.class);

            assertNotNull("FileFormat returned as null.", fileFormat);

            BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));

            MatchedRecord matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Null MatchedRecord returned when not expected.", matchedRecord);
            assertEquals("RecordBeanOne", matchedRecord.getRecordName());

            Object bean = matchedRecord.getBean(RecordBeanOne.class.getName());
            assertNotNull("Failed to load RecordBeanOne.", bean);
            assertTrue("Loaded RecordBeanOne, but wrong Class.", bean instanceof RecordBeanOne);

            RecordBeanOne beanOne = RecordBeanOne.class.cast(bean);
            assertEquals("RecordBeanOne.valueOne value is incorrect", "valueOne", beanOne.getValueOne());
            assertEquals("RecordBeanOne.valueTwo value is incorrect", "valueTwo", beanOne.getValueTwo());

            // Child Two
            assertNotNull("Failed to load RecordBeanTwoChildToOne instances.", beanOne.getBeanTwoList());
            assertEquals("Failed to load correct number of RecordBeanTwoChildToOne instances", 2, beanOne.getBeanTwoList().size());
            RecordBeanTwoChildToOne beanTwo = beanOne.getBeanTwoList().get(0);
            assertEquals("RecordBeanTwoChildToOne.valueOne value is incorrect", "valueOne2A", beanTwo.getValueOne());
            assertEquals("RecordBeanTwoChildToOne.valueTwo value is incorrect", "valueTwo2A", beanTwo.getValueTwo());

            // Child Four (beanTwo - instance 0)
            assertNotNull("Failed to load RecordBeanFourChildToTwo instances.", beanTwo.getBeanFourList());
            assertEquals("Failed to load correct number of RecordBeanFourChildToTwo instances", 1, beanTwo.getBeanFourList().size());
            RecordBeanFourChildToTwo beanFour = beanTwo.getBeanFourList().get(0);
            assertEquals("RecordBeanFourChildToTwo.valueOne value is incorrect", "valueOne4A", beanFour.getValueOne());
            assertEquals("RecordBeanFourChildToTwo.valueTwo value is incorrect", "valueTwo4A", beanFour.getValueTwo());

            beanTwo = beanOne.getBeanTwoList().get(1);
            assertEquals("RecordBeanTwoChildToOne.valueOne value is incorrect", "valueOne2B", beanTwo.getValueOne());
            assertEquals("RecordBeanTwoChildToOne.valueTwo value is incorrect", "valueTwo2B", beanTwo.getValueTwo());

            // Child Three (of beanTwo - instance 1)
            assertNotNull("Failed to load RecordBeanThreeChildToTwo instances.", beanTwo.getBeanThreeList());
            assertEquals("Failed to load correct number of RecordBeanThreeChildToTwo instances", 1, beanTwo.getBeanThreeList().size());
            RecordBeanThreeChildToTwo beanThree = beanTwo.getBeanThreeList().get(0);
            assertEquals("RecordBeanThreeChildToTwo.valueOne value is incorrect", "valueOne3B", beanThree.getValueOne());
            assertEquals("RecordBeanThreeChildToTwo.valueTwo value is incorrect", "valueTwo3B", beanThree.getValueTwo());

            // Child 5 (of beanOne - this is a single instance).
            assertNotNull("Null RecordBeanFiveChildToOne", beanOne.getBeanFive());
            RecordBeanFiveChildToOne beanFive = beanOne.getBeanFive();
            assertEquals("RecordBeanFiveChildToOne.valueOne value is incorrect", "valueOne5      ", beanFive.getValueOne());
            assertEquals("RecordBeanFiveChildToOne.valueTwo value is incorrect", "valueTwo5      ", beanFive.getValueTwo());

            // Load the next record - which should be RecordBeanSix.
            matchedRecord = fileFormat.nextRecord(bufIn);
            assertNotNull("Null MatchedRecord returned when not expected.", matchedRecord);
            assertEquals("RecordBeanSix", matchedRecord.getRecordName());

            bean = matchedRecord.getBean(RecordBeanSix.class.getName());
            assertTrue("RecordBeanSix read, but is of the wrong type.", bean instanceof RecordBeanSix);
            RecordBeanSix beanSix = RecordBeanSix.class.cast(bean);
            assertEquals("RecordBeanSix.valueOne value is incorrect", "valueOne6     ", beanSix.getValueOne());
            assertEquals("RecordBeanSix.valueTwo value is incorrect", "valueTwo6     ", beanSix.getValueTwo());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to parse the data from annotation_input.txt: " + e.getMessage());
        }
    }
}
