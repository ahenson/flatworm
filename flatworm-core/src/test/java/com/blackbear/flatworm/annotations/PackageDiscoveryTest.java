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
import com.blackbear.flatworm.Util;
import com.blackbear.flatworm.annotations.beans.RecordBeanOne;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the proper resolution of all Record annotated classes within a given package.
 *
 * @author Alan Henson
 */
public class PackageDiscoveryTest extends AbstractBaseAnnotationTest {
    
    @Before
    public void setup() {
        super.setup();
        configLoader.setPerformValidation(false);
    }
    
    @Test
    public void loadAllRecords() {
        try {
            String packageName = RecordBeanOne.class.getPackage().getName();
            FileFormat fileFormat = configLoader.loadConfiguration(packageName);

            List<Class<?>> classes = Util.findRecordAnnotatedClasses(packageName, Record.class);

            Set<String> simpleClassNames = new HashSet<>();
            classes.forEach(clazz -> simpleClassNames.add(clazz.getSimpleName()));
            
            validateFileFormat(fileFormat, classes.size(), false);
            fileFormat.getRecords().forEach(record -> 
                    assertTrue("Failed to find loaded Record: " + record.getName(), simpleClassNames.contains(record.getName())));
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Failed to load all Record annotated classes and their configuration: " + e.getMessage());
        }
    }
}
