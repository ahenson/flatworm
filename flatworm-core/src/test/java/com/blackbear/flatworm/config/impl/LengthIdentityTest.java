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

package com.blackbear.flatworm.config.impl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the LengthIdentity implementation.
 *
 * @author Alan Henson
 */
public class LengthIdentityTest extends AbstractBaseIdentityTest {

    @Test
    public void simpleTest() {
        try {
            String line = "DVDFrozen Disney";
            LengthIdentity identity = new LengthIdentity();
            identity.setMinLength(0);
            identity.setMaxLength(Integer.MAX_VALUE);

            boolean matches = identity.doesMatch(record, fileFormat, line);

            assertTrue("Length Identity failed to correctly match line.", matches);

            identity.setMinLength(line.length() + 1);
            identity.setMaxLength(identity.getMinLength());

            matches = identity.doesMatch(record, fileFormat, line);
            assertFalse("Length Identity incorrect matched line.", matches);

            identity.setMinLength(line.length());
            identity.setMaxLength(identity.getMinLength());
            matches = identity.doesMatch(record, fileFormat, line);
            assertTrue("Length Identity failed to correctly match line.", matches);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void invalidTest() {
        try {
            String line = "DVDFrozen Disney";
            LengthIdentity identity = new LengthIdentity();
            identity.setMinLength(10);
            identity.setMaxLength(0);

            boolean matches = identity.doesMatch(record, fileFormat, line);
            assertFalse("Length Identity incorrect matched line.", matches);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception occurred: " + e.getMessage());
        }

    }
}
